package io.vyne.history.db

import com.google.common.collect.MultimapBuilder
import io.vyne.models.DataSource
import io.vyne.models.EvaluatedExpression
import io.vyne.models.OperationResult
import io.vyne.models.Provided
import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.query.history.QuerySankeyChartRow
import io.vyne.query.history.SankeyNodeType
import io.vyne.schemas.QualifiedName
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
class LineageSankeyViewBuilder {
   private val logger = KotlinLogging.logger {}

   private val dataSourceTypes = MultimapBuilder.hashKeys()
      .hashSetValues().build<QualifiedName, String>()
   private val dataSourceIdsToNodes = ConcurrentHashMap<String, SankeyNode>()
   private val dataSourcePairsToWeights = ConcurrentHashMap<Pair<SankeyNode, SankeyNode>, Int>()

   fun append(instance: TypedInstance) {
      when (instance) {
//         is TypedValue -> buildForValue(instance)
//         is TypedCollection -> buildForCollection(instance)
         is TypedObject -> buildForObject(instance)
         else -> logger.warn { "No Sankey build strategy for TypedInstance of type ${instance::class.simpleName}" }
      }
   }

   private fun buildForValue(value: TypedValue) {
   }

   private fun buildForCollection(value: TypedCollection) {
   }

   private fun buildForObject(value: TypedObject) {
      value.map { (attributeName, instance) ->
         val target = SankeyNode.forAttribute(attributeName)
         appendDataSource(instance.source, target)
      }
   }

   private fun appendDataSource(source:DataSource, targetNode:SankeyNode) {
      val sourceNode = when (source) {
         is Provided -> SankeyNode(SankeyNodeType.ProvidedInput,"")
         is EvaluatedExpression -> {
            val expressionSource = source as EvaluatedExpression
            val expressionNode = SankeyNode(SankeyNodeType.Expression, expressionSource.expressionTaxi)
            expressionSource.inputs.forEach { input ->
               appendDataSource(input.source, expressionNode)
            }
            expressionNode
         }
         else -> dataSourceIdsToNodes[source.id]
      }
      if (sourceNode == null) {
         logger.warn { "No source matched for typedInstance with dataSource of type ${source.name} and id of ${source.id}" }
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
         QuerySankeyChartRow(
            queryId,
            sourceNode.nodeType,
            sourceNode.value,
            targetNode.nodeType,
            targetNode.value,
            value
         )
      }
   }


   fun captureOperationResult(operationResult: OperationResult) {
      // Asscoiate the operation to the data source.  This is a one-to-many relationship,
      // as the same operation will provide multiple values.
      val operationQualifiedName = operationResult.remoteCall.operationQualifiedName
      dataSourceTypes.put(operationQualifiedName, operationResult.id)
      dataSourceIdsToNodes.put(operationResult.id, SankeyNode(operationQualifiedName))
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
            else -> TODO("Unhandled type of operationParam value: ${operationParam.value!!::class.simpleName}")
         }
      }
   }

   private fun lookupSource(dataSource:DataSource): SankeyNode? {
      return when (dataSource) {
         is Provided -> SankeyNode(SankeyNodeType.ProvidedInput,"")
         is EvaluatedExpression -> {
            val expressionNode = SankeyNode(SankeyNodeType.Expression, dataSource.expressionTaxi)
            TODO()
         }
         else -> dataSourceIdsToNodes[dataSource.id]
      }
   }
   private fun lookupSource(sourceDataSourceId: String?): SankeyNode? {
      val source = when (sourceDataSourceId) {
         Provided.id -> {
            SankeyNode(SankeyNodeType.ProvidedInput,"")
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
   val value: String
) {
   companion object {
      fun forAttribute(name: String): SankeyNode {
         return SankeyNode(SankeyNodeType.AttributeName, name)
      }
   }

   constructor(name: QualifiedName) : this(SankeyNodeType.QualifiedName, name.parameterizedName)


}
