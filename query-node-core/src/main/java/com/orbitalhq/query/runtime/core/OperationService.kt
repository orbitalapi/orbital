package com.orbitalhq.query.runtime.core

import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.Fact
import com.orbitalhq.query.NoOpQueryContextEventDispatcher
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.runtime.OperationServiceApi
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.*
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.http.NotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

/**
 * This is a UI-facing API endpoint for invoking
 * services within the schema.
 *
 */
@RestController
class OperationService(
   private val operationInvokers: List<OperationInvoker>,
   private val schemaProvider: SchemaProvider
) : OperationServiceApi {

   @PostMapping("/api/services/{serviceName}/{operationName}")
   override suspend fun invokeOperation(
      @PathVariable("serviceName") serviceName: String,
      @PathVariable("operationName") operationName: String,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestBody facts: Map<String, Fact>
   ): ResponseEntity<Flow<Any>> {
      val (service, operation) = lookupOperation(serviceName, operationName)
      val parameterTypedInstances = mapFactsToParameters(operation, facts)
      try {
         val operationInvoker = operationInvokers.firstOrNull {
            it.canSupport(service, operation)
         } ?: error("No invoker found for operation ${operation.qualifiedName.shortDisplayName}")
         val invocationId = UUID.randomUUID().toString()
         val serialiser = FirstEntryMetadataResultSerializer(queryId = invocationId, queryOptions = QueryOptions.default())
         val operationResult = operationInvoker.invoke(
            service,
            operation,
            parameterTypedInstances,
            NoOpQueryContextEventDispatcher,
            invocationId
         )
            .map { value: TypedInstance -> serialiser.serialize(value, schemaProvider.schema) }
            .filterNotNull()
         return ResponseEntity.ok(operationResult)
      } catch (e: OperationInvocationException) {
         throw ResponseStatusException(HttpStatus.valueOf(e.httpStatus), e.message)
      }
   }

   private fun mapFactsToParameters(
      operation: Operation,
      facts: Map<String, Fact>
   ): List<Pair<Parameter, TypedInstance>> {
      val schema = schemaProvider.schema
      val parameters: List<Pair<Parameter, TypedInstance>> = facts.map { (parameterName, fact) ->
         val param = operation.parameter(parameterName)
            ?: throw BadRequestException("Operation ${operation.qualifiedName.longDisplayName} does not declare a parameter named ${parameterName}")
         if (!schema.hasType(fact.qualifiedName.parameterizedName)) {
            throw BadRequestException("Type ${fact.qualifiedName.longDisplayName} is not defined");
         }
         val factType = schema.type(fact.qualifiedName)
         val factTypedInstance = TypedInstance.from(factType, fact.value, schema, source = Provided)
         param to factTypedInstance
      }
      return parameters
   }

   private fun lookupOperation(serviceName: String, operationName: String): Pair<Service, Operation> {
      val service = try {
         schemaProvider.schema.service(serviceName)
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
