package io.vyne.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.collect.MultimapBuilder
import io.vyne.models.*
import io.vyne.query.history.QuerySankeyChartRow
import io.vyne.query.history.SankeyNodeType
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.utils.orElse
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

/**
 * A SankeyGraph  is a visualization used to depict a flow from one set of values to another.
 * (see https://developers.google.com/chart/interactive/docs/gallery/sankey)
 *
 * Here, we generate data flows on between systems per attribute for a TypedInstance
 *
 */
class LineageSankeyViewBuilder(schema: Schema) {
   private val logger = KotlinLogging.logger {}
   private val operationNodeBuilder = LineageSankeyOperationNodeBuilder(schema)

   private val dataSourceTypes = MultimapBuilder.hashKeys()
      .hashSetValues().build<QualifiedName, String>()
   private val dataSourceIdsToNodes = ConcurrentHashMap<String, SankeyNode>()
   private val dataSourcePairsToWeights = ConcurrentHashMap<Pair<SankeyNode, SankeyNode>, Int>()
   private val operationNodeDetails = ConcurrentHashMap<QualifiedName, SankeyOperationNodeDetails>()
   fun append(instance: TypedInstance) {
      when (instance) {
         is TypedObject -> buildForObject(instance)
         else -> logger.warn { "No Sankey build strategy for TypedInstance of type ${instance::class.simpleName}" }
      }
   }


   private fun buildForObject(value: TypedObject, prefixes: List<String> = emptyList()) {
      value.map { (attributeName, instance) ->
         when {
            instance.type.isScalar -> {
               val target = SankeyNode.forAttribute(attributeName, prefixes)
               appendDataSource(instance.source, target)
            }
            instance is TypedObject -> {
               buildForObject(instance, prefixes + attributeName)
            }
            instance is TypedCollection -> {
               instance.value
                  .filterIsInstance<TypedObject>()
                  .forEach { collectionMember -> buildForObject(collectionMember, prefixes) }
            }
            instance is TypedNull -> {
               // Do nothing, I guess?
            }
            else -> {
               logger.warn { "Appending sankey chart data failed.  Expected either a scalar value, or a TypedObject - but neither condition was true.  ValueType = ${instance::class.simpleName}" }
            }
         }
      }
   }

   private fun appendDataSource(source: DataSource, targetNode: SankeyNode) {
      // Ignored data sources..
      if (source is FailedSearch) {
         return
      }
      val sourceNode = when (source) {
         is Provided -> SankeyNode(SankeyNodeType.ProvidedInput, "")
         is EvaluatedExpression -> {
            val expressionSource = source as EvaluatedExpression
            val expressionNode = SankeyNode(SankeyNodeType.Expression, expressionSource.expressionTaxi)
            expressionSource.inputs.forEach { input ->
               val expressionInput = SankeyNode(
                  SankeyNodeType.ExpressionInput,
                  input.typeName.fqn().shortDisplayName,
                  id = "(${input.typeName}) -> ${expressionSource.expressionTaxi}"
               )
               incrementSankeyCount(expressionInput, expressionNode)
               appendDataSource(input.source, expressionInput)
            }
            expressionNode
         }
         else -> dataSourceIdsToNodes[source.id]
      }
      if (sourceNode == null) {
         logger.debug { "No source matched for typedInstance with dataSource of type ${source.name} and id of ${source.id}" }
      } else {
         incrementSankeyCount(sourceNode, targetNode)
      }
   }

   private fun incrementSankeyCount(source: SankeyNode, target: SankeyNode) {
      dataSourcePairsToWeights.compute(source to target) { _, value ->
         value.orElse(0) + 1
      }
   }

   fun asChartRows(queryId: String): List<QuerySankeyChartRow> {
      return dataSourcePairsToWeights.map { (key, value) ->
         val (sourceNode, targetNode) = key
         fun operationNodeDetails(node: SankeyNode): SankeyOperationNodeDetails? {
            return if (node.nodeType == SankeyNodeType.QualifiedName) {
               operationNodeDetails[node.id.fqn()]
            } else null
         }

         val sourceNodeOperationData = operationNodeDetails(sourceNode)
         val targetNodeOperationData = operationNodeDetails(targetNode)
         QuerySankeyChartRow(
            queryId,
            sourceNode.nodeType,
            sourceNode.value,
            sourceNodeOperationData,
            targetNode.nodeType,
            targetNode.value,
            targetNodeOperationData,
            value
         )
      }
   }


   fun captureOperationResult(operationResult: OperationResult) {
      // Asscoiate the operation to the data source.  This is a one-to-many relationship,
      // as the same operation will provide multiple values.
      val operationQualifiedName = operationResult.remoteCall.operationQualifiedName
      dataSourceTypes.put(operationQualifiedName, operationResult.id)
      dataSourceIdsToNodes.getOrPut(operationResult.id) {
         val sankeyOperationNodeDetails = operationNodeBuilder.buildOperationNode(operationResult)
         if (sankeyOperationNodeDetails != null) {
            operationNodeDetails[operationQualifiedName] = sankeyOperationNodeDetails
         }
         SankeyNode(operationQualifiedName, sankeyOperationNodeDetails)
      }
      operationResult.inputs.forEach { operationParam ->
         when (operationParam.value) {
            is TypeNamedInstance -> {
               val sourceDataSourceId = (operationParam.value as TypeNamedInstance).dataSourceId
               val source = lookupSource(sourceDataSourceId)
               if (source == null) {
                  logger.warn { "Received dataSourceId ${sourceDataSourceId.orElse("null")} for input parameter ${operationParam.parameterName} on operation $operationQualifiedName but that has not yet been mapped.  No entry will be added for this pair" }
               } else {
                  incrementSankeyCount(source, SankeyNode(operationQualifiedName))
               }
            }
            null -> logger.debug { "Not recording null value as input to param ${operationParam.parameterName}" }// do nothing for null.  In the future, we might want to capture this somehow
            else -> logger.warn { "Unhandled type of operationParam value: ${operationParam.value!!::class.simpleName}" }
         }
      }
   }


   private fun lookupSource(sourceDataSourceId: String?): SankeyNode? {
      val source = when (sourceDataSourceId) {
         Provided.id -> {
            SankeyNode(SankeyNodeType.ProvidedInput, "")
         }

         else -> dataSourceIdsToNodes[sourceDataSourceId]
      }
      return source
   }
}


/**
 * Using a wrapper class rather than polymorphism since
 * the goal here is to serialize to send to a ui
 */
data class SankeyNode(
   val nodeType: SankeyNodeType,
   val value: String,
   val id: String = value,
   val sankeyOperationNodeDetails: SankeyOperationNodeDetails? = null
) {
   companion object {
      fun forAttribute(name: String, prefixes: List<String>): SankeyNode {
         val nodeName = (prefixes + name).joinToString("/")
         return SankeyNode(SankeyNodeType.AttributeName, nodeName)
      }
   }

   constructor(name: QualifiedName, sankeyOperationNodeDetails: SankeyOperationNodeDetails? = null) : this(
      SankeyNodeType.QualifiedName,
      name.parameterizedName,
      sankeyOperationNodeDetails = sankeyOperationNodeDetails
   )


}

/**
 * A collection of classes which provide operation specific metadata.
 * (eg., for a Kafka operation, it's broker and topic name).
 *
 * This is for usage in the UI
 */
sealed class SankeyOperationNodeDetails(
   val operationType: OperationNodeType,
)

data class KafkaOperationNode(
   val connectionName: String,
   val topic: String
) : SankeyOperationNodeDetails(OperationNodeType.KafkaTopic)

data class HttpOperationNode(
   val operationName: QualifiedName,
   val verb: String,
   val path: String
) : SankeyOperationNodeDetails(OperationNodeType.Http)

data class DatabaseNode(
   val connectionName: String,
   val tableNames: List<String>
) : SankeyOperationNodeDetails(OperationNodeType.Database)

enum class OperationNodeType {
   KafkaTopic,
   Database,
   Http
}
