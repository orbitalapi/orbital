package com.orbitalhq.stubbing

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import com.google.common.collect.MultimapBuilder
import com.orbitalhq.Vyne
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.models.DataSourceMutatingMapper
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedInstanceConverter
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.right
import com.orbitalhq.query.HttpExchange
import com.orbitalhq.query.HttpHeaders
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.QueryEngineFactory
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.connectors.OperationInvocationPlanner
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.connectors.OperationResponseFlowProvider
import com.orbitalhq.query.connectors.OperationResponseHandler
import com.orbitalhq.query.graph.operationInvocation.DefaultOperationInvocationService
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.query.graph.operationInvocation.cache.local.LocalCachingInvokerProvider
import com.orbitalhq.query.projection.LocalProjectionProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.utils.orElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import java.time.Instant

/**
 * Stubs out Operation invocation with pre-canned responses.
 * Used primarily in tests, but also used for generating indicative query plans
 * ahead of actual execution
 */
class StubService(
   val responses: MutableMap<String, Either<StreamErrorMessage, List<TypedInstance>>> = mutableMapOf(),
   val handlers: MutableMap<String, OperationResponseHandler> = mutableMapOf(),
   val flowHandlers: MutableMap<String, OperationResponseFlowProvider> = mutableMapOf(),
   // nullable for legacy purposes, you really really should pass a schema here.
   val schema: Schema?,
   val planners: List<OperationInvocationPlanner> = emptyList()
) : OperationInvoker {
   companion object {
      private val logger = KotlinLogging.logger {}

      /**
       * For use outside of test code. (eg., Visualizers, parsers, etc).
       * Inside of tests, call testVyne()
       */
      fun stubbedVyne(
         schema: Schema,
         planners: List<OperationInvocationPlanner> = emptyList(),
         stateStoreProvider: StateStoreProvider? = null
      ): Pair<Vyne, StubService> {
         val stubService = StubService(schema = schema, planners = planners)
         val queryEngineFactory =
            QueryEngineFactory.withOperationInvokers(
               VyneCacheConfiguration.default(),
               formatSpecs = emptyList(),
               invokers = CacheAwareOperationInvocationDecorator.decorateAll(
                  listOf(stubService),
                  cacheProvider = LocalCachingInvokerProvider.default()
               ),
               projectionProvider = LocalProjectionProvider(),
               stateStoreProvider = stateStoreProvider
            )
         val vyne = Vyne(listOf(schema), queryEngineFactory)
         return vyne to stubService
      }
   }

   private var wildcardHandler: OperationResponseHandler? = null

   fun clearAll() {
      clearHandlers()
      clearInvocations()
   }

   fun clearInvocations() {
      invocations.clear()
      calls.clear()
   }

   fun clearHandlers() {
      responses.clear()
      handlers.clear()
      flowHandlers.clear()
   }

   @Deprecated("Don't invoke directly, invoke by calling testVyne()")
   constructor(
      responses: MutableMap<String, Either<StreamErrorMessage, List<TypedInstance>>> = mutableMapOf(),
      handlers: MutableMap<String, OperationResponseHandler> = mutableMapOf(),
      flowHandlers: MutableMap<String, OperationResponseFlowProvider> = mutableMapOf()
   ) : this(responses, handlers, flowHandlers, null)

   private fun justProvide(value: List<TypedInstance>): OperationResponseHandler {
      return { _, _ -> value.map { Either.Right(it) } }
   }

   /**
    * Invokes the provided stubRepsonseHandler, and then updates
    * the dataSource of the response to an OperationResult
    */
   private fun updateDataSourceOnResponse(
      service: Service,
      remoteOperation: RemoteOperation,
      params: List<Pair<Parameter, TypedInstance>>,
      handler: OperationResponseHandler
   ): List<Either<StreamErrorMessage, TypedInstance>> {
      require(schema != null) { "Stub service was not created with a schema." }
      val result = handler.invoke(remoteOperation, params)
      val dataSource = getRemoteCallDataSource(service, remoteOperation, result.map { it.getOrNull()!! }, params)
      return result.map { errorTypedInstance ->
         errorTypedInstance.flatMap { typedInstance ->
            val updated = TypedInstanceConverter(DataSourceMutatingMapper(dataSource)).convert(typedInstance)
            Either.Right(TypedInstance.from(typedInstance.type, updated, schema, source = dataSource))
         }
      }
   }

   private fun getRemoteCallDataSource(
      service: Service,
      remoteOperation: RemoteOperation,
      result: List<TypedInstance>,
      params: List<Pair<Parameter, TypedInstance>>
   ): OperationResultDataSourceWrapper {

      // MP 14-Jan-25: We defer to the operation planners (which in non-test scenarios are
      // the operation invokers) to provide the remove call details, falling back to a default.
      // This is to allow our query planners (which use this stub invoker) to generate a query plan with
      // richer information than this stub can provide.
      val planner = planners.firstOrNull { it.canSupport(service, remoteOperation) } ?: DefaultStubbedOperationPlanner
      val remoteCall = planner.plan(service, remoteOperation, params, schema!!)
      val dataSource = OperationResultDataSourceWrapper(OperationResult.from(params, remoteCall))
      return dataSource
   }

   constructor(vararg responses: Pair<String, List<TypedInstance>>) : this(
      responses.associate { it.first to Either.Right(it.second) }
         .toMutableMap())

   fun toOperationInvocationService(): OperationInvocationService {
      return DefaultOperationInvocationService(
         listOf(this)
      )
   }

   val calls = MultimapBuilder.hashKeys().arrayListValues().build<String, List<TypedInstance>>()

   fun callCount(stubKey: String): Int {
      return if (calls.containsKey(stubKey)) {
         calls.get(stubKey).size
      } else {
         0
      }
   }


   @Deprecated("Only tracks the most recent invocation per service. Prefer calls")
   val invocations = mutableMapOf<String, List<TypedInstance>>()

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val paramDescription = parameters.joinToString { "${it.second.type.name.shortDisplayName} = ${it.second.value}" }
      logger.debug { "Invoking ${service.name} -> ${operation.name}($paramDescription)" }
      val stubResponseKey = if (operation.hasMetadata("StubResponse")) {
         val metadata = operation.firstMetadata("StubResponse")
         (metadata.params["value"] as String?).orElse(operation.name)
      } else {
         operation.name
      }

      val paramValues = parameters.map { it.second }
      invocations[stubResponseKey] = paramValues
      synchronized(calls) {
         calls.put(stubResponseKey, paramValues)
      }

      if (!responses.containsKey(stubResponseKey) && !handlers.containsKey(stubResponseKey) && !flowHandlers.containsKey(
            stubResponseKey
         ) && wildcardHandler == null
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
         wildcardHandler != null -> invokeWildcardHandler(operation, parameters)
         else -> error("No handler found for $stubResponseKey")
      }
      return stubResponse.map { errorOrTypedInstance ->
         when (errorOrTypedInstance) {
            is Either.Left -> errorOrTypedInstance
            is Either.Right -> {
               val value = errorOrTypedInstance.value
               val operationResult = if (value.source is OperationResultDataSourceWrapper) {
                  (value.source as OperationResultDataSourceWrapper).operationResult
               } else {
                  val remoteCall = RemoteCall(
                     service = service.name,
                     address = "http://fakeurl",
                     operation = operation.name,
                     responseTypeName = value.type.name,
                     durationMs = 1,
                     timestamp = Instant.now(),
                     responseMessageType = ResponseMessageType.FULL,
                     response = value,
                     exchange = HttpExchange(
                        url = "http://fakeurl",
                        verb = "GET",
                        requestBody = """{ "stub" : "Not captured" }""",
                        responseCode = 200,
                        responseSize = 1000,
                        headers = HttpHeaders.empty()
                     )
                  )
                  OperationResult.from(parameters, remoteCall)
               }

               eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
               Either.Right(DataSourceUpdater.update(value, operationResult.asOperationReferenceDataSource()))
            }
         }
      }
   }

   private fun invokeWildcardHandler(
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      return wildcardHandler!!.invoke(operation, parameters)
         .asFlow()
   }


   /**
    * If the provided TypedInstance is a TypedCollection, will unwrap it to a list
    * of TypedInstances, otherwise, a Flux of just the provided instance.
    * This is to be consistent with how RestTemplateInvoker handles unwrapping the responses
    * of collectons from HttpServices
    */
   private fun unwrapTypedCollections(errorOrTypedInstances: Either<StreamErrorMessage, List<TypedInstance>>): Flow<Either<StreamErrorMessage, TypedInstance>> {
      return when (errorOrTypedInstances) {
         is Either.Left -> flowOf(Either.Left(errorOrTypedInstances.value))
         is Either.Right -> {
            val typedInstances = errorOrTypedInstances.value
            typedInstances.flatMap { typedInstance ->
               when (typedInstance) {
                  is TypedCollection -> typedInstance.value.map { Either.Right(it) }
                  else -> listOf(Either.Right(typedInstance))
               }
            }.asFlow()
         }
      }
   }

   private fun unwrapTypedCollections(errorOrTypedInstances: List<Either<StreamErrorMessage, TypedInstance>>): Flow<Either<StreamErrorMessage, TypedInstance>> {
      return errorOrTypedInstances.flatMap { errorOrTypedInstance ->
         when (errorOrTypedInstance) {
            is Either.Left -> listOf(Either.Left(errorOrTypedInstance.value))
            is Either.Right -> when (errorOrTypedInstance.value) {
               is TypedCollection -> (errorOrTypedInstance.value as TypedCollection).value.map { Either.Right(it) }
               else -> listOf(Either.Right(errorOrTypedInstance.value))
            }
         }
      }.asFlow()
   }

   fun addResponse(
      stubOperationKey: String,
      modifyDataSource: Boolean = false,
      handler: OperationResponseHandler
   ): StubService {
      if (modifyDataSource) {
         this.handlers[stubOperationKey] = { remoteOperation, params ->
            val service = findServiceFromOperation(remoteOperation)
            // Curry the provided stub
            updateDataSourceOnResponse(
               service,
               remoteOperation,
               params,
               handler
            )
         }
      } else {
         this.handlers[stubOperationKey] = handler
      }

      return this
   }

   private fun findServiceFromOperation(remoteOperation: RemoteOperation): Service {
      // reverse lookup - not efficient, but not called under normal situations, so shouldn't matter.
      return schema!!.services.first {
         it.remoteOperations.contains(remoteOperation)
      }
   }

   fun addResponseThrowing(stubOperationKey: String, error: Throwable) {
      addResponse(stubOperationKey) { _, _ ->
         throw error
      }
   }

   fun addResponseFlow(
      stubOperationKey: String,
      modifyDataSource: Boolean = false,
      handler: OperationResponseFlowProvider,

      ): StubService {
      if (modifyDataSource) {
         this.flowHandlers[stubOperationKey] = { remoteOperation, params ->
            handler.invoke(remoteOperation, params)
               .map { typedInstanceOrError ->
                  typedInstanceOrError.flatMap { typedInstance ->
                     val service = findServiceFromOperation(remoteOperation)
                     val dataSource = getRemoteCallDataSource(service, remoteOperation, listOf(typedInstance), params)
                     val updated = TypedInstanceConverter(DataSourceMutatingMapper(dataSource)).convert(typedInstance)
                     Either.Right(TypedInstance.from(typedInstance.type, updated, schema!!, source = dataSource))
                  }
               }
         }
      } else {
         this.flowHandlers[stubOperationKey] = handler
      }

      return this
   }

   /**
    * Adds multiple responses, where the value returned is determined by the first parameter of the operation
    */
   fun addResponsesByParameter(
      stubOperationKey: String,
      /**
       * A map of parameter value to Json string of the return value
       */
      responses: Map<Any, String>, modifyDataSource: Boolean = false
   ) {
      val operation = schema!!.operations.firstOrNull { it.name == stubOperationKey }
         ?: error("Cannot stub $stubOperationKey as it's not a valid operation")
      val responseTypedInstances = responses.mapValues { (_, json) ->
         parseJson(schema, operation.returnType.paramaterizedName, json)
      }
      addResponse(stubOperationKey, modifyDataSource) { _, parameters: List<Pair<Parameter, TypedInstance>> ->
         val param = parameters.first().second.toRawObject()
         val response = responseTypedInstances[param] ?: error("No response provided for parameter of $param")
         // Note: Haven't checked the List stuff here - if it fails, fix it
         if (response is List<*>) {
            response.map { (it as TypedInstance).right() }
         } else {
            listOf(response.right())
         }

      }
   }

   fun addResponse(stubOperationKey: String, json: String, modifyDataSource: Boolean = false) {
      val operation = schema!!.operations.firstOrNull { it.name == stubOperationKey }
         ?: error("Cannot stub $stubOperationKey as it's not a valid operation")
      val response = parseJson(schema!!, operation.returnType.paramaterizedName, json)
      addResponse(stubOperationKey, response, modifyDataSource)
   }

   fun addTableFindManyResponse(tableName: String, json: String) {
      val operation = schema!!.tableOperations.first { it.name == tableName }
      val response = parseJson(schema!!, operation.returnType.paramaterizedName, json)
      // people_findManyPerson
      val operationName = "${tableName}_findMany${operation.returnType.collectionTypeName}"
      addResponse(operationName, response)
   }

   fun addResponse(
      stubOperationKey: String,
      handler: OperationResponseHandler
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
         this.responses[stubOperationKey] = Either.Right(response)
      }
      return this
   }


   fun addResponse(stubOperationKey: String, response: TypedInstance, modifyDataSource: Boolean = false): StubService {
      if (modifyDataSource) {
         addResponse(stubOperationKey, handler = justProvide(listOf(response)), modifyDataSource = true)
      } else {
         this.responses[stubOperationKey] = Either.Right(listOf(response))
      }

      return this
   }

   fun addResponseReturningInputs(stubOperationKey: String): StubService {
      return addResponse(stubOperationKey) { op, parameters ->
         // find the return type somewhere in the params
         val returnParam = parameters.single { it.first.type.isAssignableTo(op.returnType) }.second
         listOf(Either.Right(returnParam))
      }
   }

   fun addResponse(
      stubOperationKey: String,
      response: TypedCollection,
      modifyDataSource: Boolean = false
   ): StubService {
      if (modifyDataSource) {
         addResponse(stubOperationKey, handler = justProvide(response.value), modifyDataSource = true)
      } else {
         this.responses[stubOperationKey] = Either.Right(response.value)
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
         this.flowHandlers.containsKey(operation.name) ||
         this.wildcardHandler != null
   }

   fun returnStubValuesForAllOperations() {
      wildcardHandler = { remoteOperation: RemoteOperation, params: List<Pair<Parameter, TypedInstance>> ->
         val service = findServiceFromOperation(remoteOperation)
         updateDataSourceOnResponse(service, remoteOperation, params) { _, _ ->
            listOf(Either.Right(MockTypedInstanceBuilder.build(remoteOperation.returnType, schema!!)))
         }

      }
   }
}

object DefaultStubbedOperationPlanner : OperationInvocationPlanner {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean = true
}
