package io.vyne

import io.vyne.models.DataSourceMutatingMapper
import io.vyne.models.OperationResult
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.json.Jackson
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.RemoteCall
import io.vyne.query.connectors.OperationInvoker
import io.vyne.query.graph.operationInvocation.DefaultOperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.fqn
import io.vyne.utils.orElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

typealias StubResponseHandler = (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> List<TypedInstance>
typealias StubResponseFlowProvider = (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> Flow<TypedInstance>

class StubService(
   val responses: MutableMap<String, List<TypedInstance>> = mutableMapOf(),
   val handlers: MutableMap<String, StubResponseHandler> = mutableMapOf(),
   val flowHandlers: MutableMap<String, StubResponseFlowProvider> = mutableMapOf(),
   // nullable for legacy purposes, you really really should pass a schema here.
   val schema: Schema?
) : OperationInvoker {

   @Deprecated("Don't invoke directly, invoke by calling testVyne()")
   constructor(
      responses: MutableMap<String, List<TypedInstance>> = mutableMapOf(),
      handlers: MutableMap<String, StubResponseHandler> = mutableMapOf(),
      flowHandlers: MutableMap<String, StubResponseFlowProvider> = mutableMapOf()
   ) : this(responses, handlers, flowHandlers, null)

   private fun justProvide(value: List<TypedInstance>): StubResponseHandler {
      return { _, _ -> value }
   }

   /**
    * Invokes the provided stubRepsonseHandler, and then updates
    * the dataSource of the response to an OperationResult
    */
   private fun updateDataSourceOnResponse(
      remoteOperation: RemoteOperation,
      params: List<Pair<Parameter, TypedInstance>>,
      handler: StubResponseHandler
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
         timestamp = Instant.now()
      )
      val dataSource = OperationResult.from(params, remoteCall)
      return result.map { typedInstance ->
         val updated = TypedInstanceConverter(DataSourceMutatingMapper(dataSource)).convert(typedInstance)
         TypedInstance.from(typedInstance.type, updated, schema, source = dataSource)
      }
   }

   constructor(vararg responses: Pair<String, List<TypedInstance>>) : this(responses.toMap().toMutableMap())

   fun toOperationInvocationService(): OperationInvocationService {
      return DefaultOperationInvocationService(
         listOf(this)
      )
   }

   val invocations = mutableMapOf<String, List<TypedInstance>>()

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?
   ): Flow<TypedInstance> {
      val paramDescription = parameters.joinToString { "${it.second.type.name.shortDisplayName} = ${it.second.value}" }
      logger.debug { "Invoking ${service.name} -> ${operation.name}($paramDescription)" }
      val stubResponseKey = if (operation.hasMetadata("StubResponse")) {
         val metadata = operation.metadata("StubResponse")
         (metadata.params["value"] as String?).orElse(operation.name)
      } else {
         operation.name
      }

      invocations.put(stubResponseKey, parameters.map { it.second })

      if (!responses.containsKey(stubResponseKey) && !handlers.containsKey(stubResponseKey) && !flowHandlers.containsKey(
            stubResponseKey
         )
      ) {
         throw IllegalArgumentException("No stub response or handler prepared for operation $stubResponseKey")
      }

      val stubResponse = when {
         responses.containsKey(stubResponseKey) -> {
            unwrapTypedCollections(responses[stubResponseKey]!!)
         }
         handlers.containsKey(stubResponseKey) -> {
            unwrapTypedCollections(handlers[stubResponseKey]!!.invoke(operation, parameters))
         }
         flowHandlers.containsKey(stubResponseKey) -> flowHandlers[stubResponseKey]!!.invoke(operation, parameters)
         else -> error("No handler found for $stubResponseKey")
      }
      return stubResponse
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

   fun addResponse(
      stubOperationKey: String,
      modifyDataSource: Boolean = false,
      handler: StubResponseHandler
   ): StubService {
      if (modifyDataSource) {
         this.handlers.put(stubOperationKey) { remoteOperation, params ->
            // Curry the provided stub
            updateDataSourceOnResponse(
               remoteOperation,
               params,
               handler
            )
         }
      } else {
         this.handlers.put(stubOperationKey, handler)
      }

      return this
   }

   fun addResponseFlow(
      stubOperationKey: String,
      handler: StubResponseFlowProvider
   ): StubService {
      this.flowHandlers[stubOperationKey] = handler
      return this
   }

   fun addResponse(
      stubOperationKey: String,
      handler: StubResponseHandler
   ): StubService {
      return addResponse(stubOperationKey, false, handler)
   }

   fun addResponse(
      stubOperationKey: String,
      response: List<TypedInstance>,
      modifyDataSource: Boolean = false
   ): StubService {
      if (modifyDataSource) {
         addResponse(stubOperationKey, handler = justProvide(response), modifyDataSource = true)
      } else {
         this.responses.put(stubOperationKey, response)
      }

      return this
   }

   fun addResponse(stubOperationKey: String, response: TypedInstance, modifyDataSource: Boolean = false): StubService {
      if (modifyDataSource) {
         addResponse(stubOperationKey, handler = justProvide(listOf(response)), modifyDataSource = true)
      } else {
         this.responses.put(stubOperationKey, listOf(response))
      }

      return this
   }

   fun addResponse(
      stubOperationKey: String,
      response: TypedCollection,
      modifyDataSource: Boolean = false
   ): StubService {
      if (modifyDataSource) {
         addResponse(stubOperationKey, handler = justProvide(response.value), modifyDataSource = true)
      } else {
         this.responses.put(stubOperationKey, response.value)
      }
      return this
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      // TODO : why did I need to do this?
      // This was working by using annotations on the method to indicate that there was a stub response
      // Why do I care about that?  I could just use the method name as a default?!
      // I've changed this to look for a match with responses against the method name - revert if that turns out to be dumb.
      return operation.metadata.any { it.name.name == "StubResponse" } ||
         this.responses.containsKey(operation.name) ||
         this.handlers.containsKey(operation.name) ||
         this.flowHandlers.containsKey(operation.name)
   }


}
