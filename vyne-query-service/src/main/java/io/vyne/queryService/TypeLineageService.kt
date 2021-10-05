package io.vyne.queryService

import io.vyne.query.graph.Algorithms
import io.vyne.query.graph.ElementType
import io.vyne.query.graph.OperationQueryResultItemRole
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.ConsumedOperation
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.ServiceLineage
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
               nodeId = sourceServiceId
            )
         )
         serviceLineage.consumesVia.forEach { dependency ->
            val dependencyId = dependency.serviceName.toBrowserSafeGraphId()
            nodes.add(
               SchemaGraphNode(
                  id = dependencyId,
                  label = dependency.serviceName.fqn().shortDisplayName,
                  type = ElementType.SERVICE,
                  nodeId = sourceServiceId
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
      val operationsExposingType = Algorithms.findAllFunctionsWithArgumentOrReturnValueForType(schema, typeName)
         .results.filter { resultItem -> resultItem.role == OperationQueryResultItemRole.Output }

      val serviceLineage = operationsExposingType
         .map { resultItem ->
            val service = schema.service(resultItem.serviceName)
            val upstreamOperations = service.lineage.orElse(ServiceLineage.empty())
               .consumes.filter { upstreamOperation ->
                  operationsExposingType.any {
                     it.operationDisplayName == upstreamOperation.operationName && it.serviceName == upstreamOperation.serviceName
                  }
               }
            ServiceLineageForType(resultItem.serviceName.fqn(), upstreamOperations)
         }
      return serviceLineage
   }
}
