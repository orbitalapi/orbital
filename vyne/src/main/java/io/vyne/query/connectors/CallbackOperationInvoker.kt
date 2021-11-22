package io.vyne.query.connectors

import io.vyne.models.DataSourceMutatingMapper
import io.vyne.models.OperationResult
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.json.Jackson
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseMessageType
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Parameter
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.fqn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import lang.taxi.formatter.TaxiCodeFormatter
import mu.KotlinLogging
import java.time.Instant

typealias OperationResponseHandler = (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> List<TypedInstance>
typealias OperationResponseFlowProvider = (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> Flow<TypedInstance>

private val logger = KotlinLogging.logger {}

/**
 * An OperationInvoker that provides callbacks to allow wiring in functional responses.
 * Similar to the StubOperationInvoker used for test purposes
 */
class CallbackOperationInvoker(
   private val schema: Schema,
   private val serviceName: QualifiedName
) : OperationInvoker {

   private val handlers: MutableMap<QualifiedName, OperationResponseHandler> = mutableMapOf()
   private val flowHandlers: MutableMap<QualifiedName, OperationResponseFlowProvider> = mutableMapOf()
   private val operationSpecs = mutableListOf<OperationSpec>()

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?
   ): Flow<TypedInstance> {
      val paramDescription = parameters.joinToString { "${it.second.type.name.shortDisplayName} = ${it.second.value}" }
      logger.debug { "Invoking ${service.name} -> ${operation.name}($paramDescription)" }

      val operationQualifiedName = operation.qualifiedName
      if (!handlers.containsKey(operationQualifiedName) && !flowHandlers.containsKey(operationQualifiedName)
      ) {
         throw IllegalArgumentException("No  response or handler prepared for operation $operationQualifiedName")
      }

      val repsonse: Flow<TypedInstance> = when {
         handlers.containsKey(operationQualifiedName) -> {
            unwrapTypedCollections(handlers[operationQualifiedName]!!.invoke(operation, parameters))
         }
         flowHandlers.containsKey(operationQualifiedName) -> flowHandlers[operationQualifiedName]!!.invoke(
            operation,
            parameters
         )
         else -> error("No handler found for $operationQualifiedName")
      }
      return repsonse
   }

   /**
    * If the provided TypedInstance is a TypedCollection, will unwrap it to a list
    * of TypedInstances, otherwise, a Flux of just the provided instance.
    * This is to be consistent with how RestTemplateInvoker handles unwrapping the responses
    * of collectons from HttpServices
    */
   private fun unwrapTypedCollections(typedInstances: List<TypedInstance>): Flow<TypedInstance> {
      val unwrapped = typedInstances.flatMap { typedInstance ->
         when (typedInstance) {
            is TypedCollection -> typedInstance.value
            else -> listOf(typedInstance)
         }
      }
      return unwrapped.asFlow()

   }

   /**
    * Invokes the provided stubRepsonseHandler, and then updates
    * the dataSource of the response to an OperationResult
    */
   private fun updateDataSourceOnResponse(
      remoteOperation: RemoteOperation,
      params: List<Pair<Parameter, TypedInstance>>,
      handler: OperationResponseHandler
   ): List<TypedInstance> {
      require(schema != null) { "Stub service was not created with a schema." }
      val result = handler.invoke(remoteOperation, params)
      val remoteCall = RemoteCall(
         service = OperationNames.serviceName(remoteOperation.qualifiedName).fqn(),
         address = "https://fakeurl.com/",
         operation = remoteOperation.name,
         responseTypeName = remoteOperation.returnType.qualifiedName,
         method = "FAKE",
         requestBody = "Fake response body",
         resultCode = 200,
         durationMs = 0,
         response = Jackson.defaultObjectMapper.writeValueAsString(result),
         timestamp = Instant.now(),
         responseMessageType = ResponseMessageType.FULL
      )
      val dataSource = OperationResult.from(params, remoteCall)
      return result.map { typedInstance ->
         val updated = TypedInstanceConverter(DataSourceMutatingMapper(dataSource)).convert(typedInstance)
         TypedInstance.from(typedInstance.type, updated, schema, source = dataSource)
      }
   }

   fun addHandler(
      operationSpec: OperationSpec,
      modifyDataSource: Boolean = false,
      handler: OperationResponseHandler
   ): CallbackOperationInvoker {
      if (modifyDataSource) {
         this.handlers[operationSpec.operationName] = { remoteOperation, params ->
            // Curry the provided stub
            updateDataSourceOnResponse(
               remoteOperation,
               params,
               handler
            )
         }
      } else {
         this.handlers[operationSpec.operationName] = handler
      }
      operationSpecs.add(operationSpec)

      return this
   }

//   fun addResponseFlow(
//      stubOperationKey: String,
//      handler: OperationResponseFlowProvider
//   ): CallbackOperationInvoker {
//      this.flowHandlers[stubOperationKey] = handler
//      return this
//   }
//
//   fun addHandler(
//      stubOperationKey: String,
//      handler: OperationResponseHandler
//   ): CallbackOperationInvoker {
//      return addHandler(stubOperationKey, false, handler)
//   }

//   fun addHandler(
//      stubOperationKey: String,
//      response: List<TypedInstance>,
//      modifyDataSource: Boolean = false
//   ): CallbackOperationInvoker {
//      if (modifyDataSource) {
//         addHandler(stubOperationKey, handler = justProvide(response), modifyDataSource = true)
//      } else {
//         this.responses.put(stubOperationKey, response)
//      }
//
//      return this
//   }

//   fun addHandler(stubOperationKey: String, response: TypedInstance, modifyDataSource: Boolean = false): CallbackOperationInvoker {
//      if (modifyDataSource) {
//         addHandler(stubOperationKey, handler = justProvide(listOf(response)), modifyDataSource = true)
//      } else {
//         this.responses.put(stubOperationKey, listOf(response))
//      }
//
//      return this
//   }
//
//   fun addHandler(
//      stubOperationKey: String,
//      response: TypedCollection,
//      modifyDataSource: Boolean = false
//   ): CallbackOperationInvoker {
//      if (modifyDataSource) {
//         addHandler(stubOperationKey, handler = justProvide(response.value), modifyDataSource = true)
//      } else {
//         this.responses.put(stubOperationKey, response.value)
//      }
//      return this
//   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return this.handlers.containsKey(operation.qualifiedName) ||
         this.flowHandlers.containsKey(operation.qualifiedName)
   }

   fun operationName(operationName: String): QualifiedName {
      return OperationNames.qualifiedName(this.serviceName.parameterizedName, operationName)
   }

   fun generateSchema(): String {
      val allTypes = operationSpecs.flatMap {
         it.inputTypeNames + it.responseTypeName
      }.flatMap { qualifiedName ->
         qualifiedName.parameters + qualifiedName.fullyQualifiedName.fqn()
      }.filter { !it.namespace.startsWith("lang.taxi") }
      val imports = allTypes.joinToString(separator = "\n") { it.fullyQualifiedName }
      val operations = operationSpecs.joinToString("\n") { operation  ->
         val operationInputs = operation.inputTypeNames.joinToString(", ") { input -> input.parameterizedName }
         """operation ${OperationNames.operationName(operation.operationName)}($operationInputs) : ${operation.responseTypeName.parameterizedName}"""
      }
      val schema = """namespace ${this.serviceName.namespace} {
   service ${this.serviceName.name} {
      $operations
   }
}
""".trimIndent()
      return TaxiCodeFormatter.format(schema)
   }

}

data class OperationSpec(
   val operationName: QualifiedName,
   val inputTypeNames: List<QualifiedName>,
   val responseTypeName: QualifiedName
)
