package io.vyne.queryService

import io.vyne.query.graph.Algorithms
import io.vyne.query.graph.ElementType
import io.vyne.query.graph.OperationQueryResultItemRole
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemas.ConsumedOperation
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.ServiceLineage
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.utils.orElse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

data class ServiceLineageForType(
   val serviceName: QualifiedName,
   val consumesVia: List<ConsumedOperation>
)

@RestController
class TypeLineageService(private val schemaProvider: SchemaProvider) {

   @GetMapping("/api/services/{serviceName}/lineage")
   fun getLineageGraphForService(@PathVariable("serviceName") serviceName: String): SchemaGraph {
      val schema = schemaProvider.schema()
      val service = schema.service(serviceName)
      val thisServiceLineage = service.lineage?.let { serviceLineage -> service.name to serviceLineage }
      // Find all the inbound links
      val serviceLineages = findInboundLineageLinks(schema, serviceName) + listOfNotNull(thisServiceLineage)

      val nodes = mutableSetOf<SchemaGraphNode>()
      val links = mutableSetOf<SchemaGraphLink>()
      serviceLineages.forEach { (name, serviceLineage) ->
         val (newNodes, newLinks) = createServiceLinks(name, serviceLineage)
         nodes.addAll(newNodes)
         links.addAll(newLinks)
      }
      return SchemaGraph(nodes, links)
   }

   /**
    * Returns the services with an operation that depend on the provided serviceName
    */
   private fun findInboundLineageLinks(
      schema: Schema,
      serviceName: String
   ) = schema.services
      .mapNotNull { service ->
         if (service.lineage != null) {
            service.name to service.lineage!!
         } else null
      }
      .filter { (serviceNameFromSchema, lineage) ->
         lineage.consumes.any { consumedOperation ->
            consumedOperation.serviceName == serviceName
         }
      }

   private fun createServiceLinks(
      name: QualifiedName,
      lineage: ServiceLineage
   ): Pair<Set<SchemaGraphNode>, Set<SchemaGraphLink>> {
      val (serviceNodeId, serviceNode) = graphNode(name, ElementType.SERVICE)
      val nodes = mutableSetOf<SchemaGraphNode>(serviceNode)
      val links = mutableSetOf<SchemaGraphLink>()
      lineage.consumes.forEach { consumedOperation ->
         val (dependencyOperationNodeId, dependencyOperationNode) = graphNode(
            consumedOperation.operationQualifiedName,
            ElementType.OPERATION
         )
         val (dependencyServiceNodeId, dependencyServiceNode) = graphNode(
            consumedOperation.serviceName.fqn(),
            ElementType.SERVICE
         )
         nodes.addAll(setOf(dependencyServiceNode, dependencyOperationNode))
         links.addAll(
            setOf(
               SchemaGraphLink(dependencyServiceNodeId, dependencyOperationNodeId, "Has operation"),
               SchemaGraphLink(serviceNodeId, dependencyOperationNodeId, "Consumes operation")
            )
         )
      }
      return nodes to links
   }

   private fun graphNode(name: QualifiedName, nodeType: ElementType): Pair<String, SchemaGraphNode> {
      val id = name.fullyQualifiedName.toBrowserSafeGraphId()
      return id to SchemaGraphNode(
         id,
         label = name.shortDisplayName,
         type = nodeType,
         nodeId = name.fullyQualifiedName
      )
   }

   @GetMapping("/api/types/{typeName}/lineage")
   fun getLineageGraphForType(@PathVariable("typeName") typeName: String): SchemaGraph {
      val lineage = getLineageForType(typeName)
      val nodes = mutableSetOf<SchemaGraphNode>()
      val links = mutableSetOf<SchemaGraphLink>()
      lineage.forEach { serviceLineage ->
         val sourceServiceId = serviceLineage.serviceName.fullyQualifiedName.toBrowserSafeGraphId()
         nodes.add(
            SchemaGraphNode(
               id = sourceServiceId,
               label = serviceLineage.serviceName.shortDisplayName,
               type = ElementType.SERVICE,
               nodeId = serviceLineage.serviceName.fullyQualifiedName
            )
         )
         serviceLineage.consumesVia.forEach { dependency ->
            val dependencyId = dependency.serviceName.toBrowserSafeGraphId()
            nodes.add(
               SchemaGraphNode(
                  id = dependencyId,
                  label = dependency.serviceName.fqn().shortDisplayName,
                  type = ElementType.SERVICE,
                  nodeId = dependency.serviceName
               )
            )
            links.add(
               SchemaGraphLink(
                  source = sourceServiceId,
                  target = dependencyId,
                  label = "via ${dependency.operationName}"
               )
            )
         }
      }
      return SchemaGraph(nodes, links)
   }

   fun getLineageForType(typeName: String): List<ServiceLineageForType> {
      val schema = schemaProvider.schema();
      // First, find everything that exposes our requested dataType
      val searchTypes = mutableListOf<Type>()
      val type = schema.type(typeName)
      searchTypes.add(type)
      // Search for operations retuning both T and T[]
      if (!type.isCollection) {
         searchTypes.add(type.asArrayType())
      }
      val operationsExposingType = searchTypes.flatMap { searchType ->
         Algorithms.findAllFunctionsWithArgumentOrReturnValueForType(schema, searchType.name.parameterizedName).results
      }.filter { resultItem -> resultItem.role == OperationQueryResultItemRole.Output }

      val serviceLineage = operationsExposingType
         .flatMap { resultItem ->
            val service = schema.service(resultItem.serviceName.fullyQualifiedName)
            val upstreamOperations = service.lineage.orElse(ServiceLineage.empty())
               .consumes.filter { upstreamOperation ->
                  operationsExposingType.any {
                     it.operationDisplayName == upstreamOperation.operationName && it.serviceName.fullyQualifiedName == upstreamOperation.serviceName
                  }
               }
            val upstreamConsumers = ServiceLineageForType(resultItem.serviceName, upstreamOperations)
            val downstreamConsumers = if (resultItem.operationName != null) {
               schema.services
                  .map { it to it.lineage.orElse(ServiceLineage.empty()) }
                  .filter { (service, serviceLineage) ->
                     serviceLineage.consumesOperation(resultItem.operationName!!)
                  }
                  .map { (service, serviceLineage) ->
                     ServiceLineageForType(service.name, serviceLineage.getConsumerOf(resultItem.operationName!!))
                  }
            } else {
               emptyList()
            }
            listOf(upstreamConsumers) + downstreamConsumers

         }
      return combineLineageResults(serviceLineage)
   }

   private fun combineLineageResults(serviceLineage: List<ServiceLineageForType>): List<ServiceLineageForType> {
      return serviceLineage.groupBy({ serviceLineageForType: ServiceLineageForType -> serviceLineageForType.serviceName }, { serviceLineageForType -> serviceLineageForType.consumesVia })
         .map { (serviceName, operations: List<List<ConsumedOperation>>) ->
            ServiceLineageForType(serviceName, operations.flatten().distinct())
         }
   }
}

