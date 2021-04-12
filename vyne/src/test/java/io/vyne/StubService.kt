package io.vyne

import io.vyne.models.*
import io.vyne.models.json.Jackson
import io.vyne.query.ProfilerOperation
import io.vyne.query.RemoteCall
import io.vyne.query.graph.operationInvocation.DefaultOperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemas.*
import io.vyne.utils.log
import io.vyne.utils.orElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

typealias StubResponseHandler = (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> List<TypedInstance>

class StubService(
   val responses: MutableMap<String, List<TypedInstance>> = mutableMapOf(),
   val handlers: MutableMap<String, StubResponseHandler> = mutableMapOf(),
   // nullable for legacy purposes, you really really should pass a schema here.
   val schema:Schema?
) : OperationInvoker {

   @Deprecated("Don't invoke directly, invoke by calling testVyne()")
   constructor(responses: MutableMap<String, List<TypedInstance>> = mutableMapOf(),
               handlers: MutableMap<String, StubResponseHandler> = mutableMapOf()) : this(responses, handlers, null)
   private fun justProvide(value: List<TypedInstance>): StubResponseHandler {
      return { _, _ -> value }
   }

   /**
    * Invokes the provided stubRepsonseHandler, and then updates
    * the dataSource of the response to an OperationResult
    */
   private fun modifyDataSource(
      remoteOperation: RemoteOperation,
      params: List<Pair<Parameter, TypedInstance>>,
      handler: StubResponseHandler
   ): List<TypedInstance> {
      require(schema != null) { "Stub service was not created with a schema."}
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
         response = Jackson.defaultObjectMapper.writeValueAsString(result)
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
      profiler: ProfilerOperation
   , queryId: String?): Flow<TypedInstance> {
      val paramDescription = parameters.joinToString { "${it.second.type.name.shortDisplayName} = ${it.second.value}" }
      log().info("Invoking ${service.name} -> ${operation.name}($paramDescription)")
      val stubResponseKey = if (operation.hasMetadata("StubResponse")) {
         val metadata = operation.metadata("StubResponse")
         (metadata.params["value"] as String?).orElse(operation.name)
      } else {
         operation.name
      }
      invocations.put(stubResponseKey, parameters.map { it.second })

      if (!responses.containsKey(stubResponseKey) && !handlers.containsKey(stubResponseKey)) {
         throw IllegalArgumentException("No stub response or handler prepared for operation $stubResponseKey")
      }

      return if (responses.containsKey(stubResponseKey)) {
         flow {
            responses[stubResponseKey]!!.forEach {
               emit(it)
            }
         }
      } else {
         flow {
            handlers[stubResponseKey]!!.invoke(operation, parameters).forEach {
               emit(it)
            }
         }
      }
   }

   fun addResponse(
      stubOperationKey: String,
      modifyDataSource: Boolean = false,
      handler: StubResponseHandler
   ): StubService {
      if (modifyDataSource) {
         this.handlers.put(stubOperationKey) { remoteOperation, params ->
            // Curry the provided stub
            modifyDataSource(
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
      return operation.metadata.any { it.name.name == "StubResponse" } || this.responses.containsKey(operation.name) || this.handlers.containsKey(
         operation.name
      )
   }


}
