package io.vyne.queryService

import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.query.DefaultProfilerOperation
import io.vyne.query.Fact
import io.vyne.query.ResultMode
import io.vyne.query.graph.operationInvocation.OperationInvocationException
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * This is a UI-facing API endpoint for invoking
 * services within the schema.
 *
 */
@RestController
class OperationService(private val operationInvoker: OperationInvoker, private val schemaProvider: SchemaProvider) {

   @PostMapping("/api/services/{serviceName}/{operationName}")
   fun invokeOperation(
      @PathVariable("serviceName") serviceName: String,
      @PathVariable("operationName") operationName: String,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestBody facts: Map<String, Fact>
   ): ResponseEntity<TypedInstance> {
      val (service, operation) = lookupOperation(serviceName, operationName)
      val parameterTypedInstances = mapFactsToParameters(operation, facts)
      try {
         val operationResult = runBlocking { operationInvoker.invoke(service, operation, parameterTypedInstances, DefaultProfilerOperation.root()).first() }
         return ResponseEntity.ok(operationResult)
      } catch (e: OperationInvocationException) {
         throw ResponseStatusException(e.httpStatus, e.message)
      }
   }

   private fun mapFactsToParameters(operation: Operation, facts: Map<String, Fact>): List<Pair<Parameter, TypedInstance>> {
      val schema = schemaProvider.schema()
      val parameters: List<Pair<Parameter, TypedInstance>> = facts.map { (parameterName, fact) ->
         val param = operation.parameter(parameterName)
            ?: throw BadRequestException("Operation ${operation.qualifiedName.longDisplayName} does not declare a parameter named ${parameterName}")
         if (!schema.hasType(fact.qualifiedName.parameterizedName)) {
            throw  BadRequestException("Type ${fact.qualifiedName.longDisplayName} is not defined");
         }
         val factType = schema.type(fact.qualifiedName)
         val factTypedInstance = TypedInstance.from(factType, fact.value, schema, source = Provided)
         param to factTypedInstance
      }
      return parameters
   }

   private fun lookupOperation(serviceName: String, operationName: String): Pair<Service, Operation> {
      val service = try {
         schemaProvider.schema().service(serviceName)
      } catch (e: Exception) {
         throw NotFoundException(e.message!!)
      }
      val operation = try {
         service.operation(operationName)
      } catch (e: Exception) {
         throw NotFoundException(e.message!!)
      }
      return service to operation
   }
}
