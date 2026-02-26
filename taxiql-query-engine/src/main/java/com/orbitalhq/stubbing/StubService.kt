package com.orbitalhq.stubbing

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
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
import com.orbitalhq.schemas.TableOperation
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.orElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import java.time.Instant

/**
 * A powerful stubbing service that provides pre-canned responses for operation invocations in the Orbital testing framework.
 *
 * StubService allows you to mock remote service calls with configurable responses, making it ideal for:
 * - **Unit and integration testing** - Mock external dependencies and services
 * - **Query plan generation** - Generate indicative execution plans without actual service calls
 * - **Development workflows** - Test complex scenarios without external service dependencies
 *
 * ## Basic Usage
 *
 * The most common pattern is to create a stubbed Vyne instance for testing:
 *
 * ```kotlin
 * val (vyne, stubs) = testVyne(schema)
 *
 * // Add simple typed response
 * stubs.addResponse("getUserById", userInstance)
 *
 * // Add JSON response with data source tracking
 * stubs.addResponse("getUsers", """[{"id": 1, "name": "John"}]""", modifyDataSource = true)
 *
 * // Execute query using stubbed responses
 * val result = vyne.query("find { User }")
 * ```
 *
 * ## Response Types
 *
 * StubService supports multiple response patterns:
 *
 * ### Static Responses
 * ```kotlin
 * // Single typed instance
 * stubs.addResponse("findUser", userTypedInstance)
 *
 * // JSON string (automatically parsed)
 * stubs.addResponse("getProduct", """{"id": 123, "name": "Widget"}""")
 *
 * // Collection responses
 * stubs.addResponse("getAllUsers", listOf(user1, user2, user3))
 * ```
 *
 * ### Dynamic Responses with Logic
 * ```kotlin
 * stubs.addResponse("findByStatus") { operation, parameters ->
 *     val status = parameters.first().second.value as String
 *     when (status) {
 *         "ACTIVE" -> listOf(activeUserInstance.right())
 *         "INACTIVE" -> listOf(inactiveUserInstance.right())
 *         else -> emptyList()
 *     }
 * }
 * ```
 *
 * ### Parameter-based Responses
 * ```kotlin
 * stubs.addResponsesByParameter(
 *     "getUserByType",
 *     mapOf(
 *         "ADMIN" to """{"id": 1, "type": "ADMIN", "permissions": "ALL"}""",
 *         "USER" to """{"id": 2, "type": "USER", "permissions": "LIMITED"}"""
 *     )
 * )
 * ```
 *
 * ### Error Responses
 * ```kotlin
 * stubs.addResponseThrowing("failingOperation", RuntimeException("Service unavailable"))
 * ```
 *
 * ## Verification and Testing
 *
 * Track invocations to verify your code calls the expected operations:
 *
 * ```kotlin
 * // Verify operation was called
 * stubs.callCount("getUserById") shouldBe 1
 *
 * // Verify parameters passed
 * val params = stubs.invocations["getUserById"]!!
 * params[0].value shouldBe "user123"
 * ```
 *
 * ## Data Source Tracking
 *
 * Use `modifyDataSource = true` to track data lineage in query results:
 *
 * ```kotlin
 * stubs.addResponse("getOrders", ordersJson, modifyDataSource = true)
 * // Results will include operation metadata for data lineage tracking
 * ```
 *
 * @param responses Pre-configured static responses keyed by operation name
 * @param handlers Dynamic response handlers for conditional logic
 * @param flowHandlers Streaming response handlers for Flow-based operations
 * @param schema The schema defining operations and types (required for most functionality)
 * @param planners Optional operation planners for generating query execution plans
 *
 * @see stubbedVyne For creating Vyne instances outside of tests
 * @since 0.36.0
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
       * Creates a Vyne instance with StubService for use outside of test environments.
       *
       * This factory method is ideal for scenarios like query plan visualization, schema exploration,
       * or development workflows where you need a functional Vyne instance without external dependencies.
       *
       * **For test code, use `testVyne()` instead** as it provides additional test-specific functionality.
       *
       * ## Example Usage
       *
       * ```kotlin
       * val schema = loadSchema()
       * val (vyne, stubService) = StubService.stubbedVyne(schema)
       *
       * // Configure responses for operations you need
       * stubService.addResponse("getUsers", """[{"id":1,"name":"John"}]""")
       * stubService.addResponse("getProducts", productList)
       *
       * // Use vyne for query execution or plan generation
       * val queryPlan = vyne.generateQueryPlan("find { User }")
       * val results = vyne.query("find { User where id = 1 }")
       * ```
       *
       * @param schema The schema defining the operations and types available to Vyne
       * @param planners Optional operation planners for customizing query execution planning
       * @param stateStoreProvider Optional state store for caching and state management
       * @return A pair containing the configured Vyne instance and the StubService for response configuration
       *
       * @see testVyne For test-specific Vyne instance creation
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

   /**
    * Resolves the key used to look up a response/handler for an operation.
    * Stubs can be registered by either the short operation name (e.g. "getProduct")
    * or the fully qualified name (e.g. "com.foo.ProductsApi@@getProduct").
    * This method checks both forms against the given map, preferring a match on the
    * fully qualified name if both are present.
    */
   private fun <V> resolveKey(map: Map<String, V>, operation: RemoteOperation): String? {
      val qualifiedName = operation.qualifiedName.parameterizedName
      return when {
         map.containsKey(qualifiedName) -> qualifiedName
         map.containsKey(operation.name) -> operation.name
         else -> null
      }
   }

   /**
    * Finds an operation in the schema by either its short name or fully qualified name.
    */
   private fun findOperationByKey(stubOperationKey: String): RemoteOperation? {
      return schema!!.operations.firstOrNull {
         it.name == stubOperationKey || it.qualifiedName.parameterizedName == stubOperationKey
      }
   }

   /**
    * Clears all configured responses, handlers, and invocation history.
    *
    * This method provides a clean slate by removing:
    * - All static responses
    * - All dynamic handlers
    * - All flow handlers
    * - All invocation tracking data
    *
    * Useful for resetting state between test cases or test methods.
    *
    * ```kotlin
    * @BeforeEach
    * fun setup() {
    *     stubService.clearAll()
    *     // Configure fresh responses for each test
    * }
    * ```
    */
   fun clearAll() {
      clearHandlers()
      clearInvocations()
   }

   /**
    * Clears only the invocation history while preserving configured responses and handlers.
    *
    * Use this when you want to reset call tracking between test assertions but keep
    * the same response configuration.
    */
   fun clearInvocations() {
      invocations.clear()
      calls.clear()
   }

   /**
    * Clears all configured responses and handlers while preserving invocation history.
    *
    * Use this when you want to reconfigure responses but keep tracking of previous calls.
    */
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

   fun lastCall(name:String):List<TypedInstance> = calls[name].last()
   val calls = MultimapBuilder.hashKeys().arrayListValues().build<String, List<TypedInstance>>()

   /**
    * Returns the number of times a specific operation has been invoked.
    *
    * This method is essential for verifying that your code calls the expected operations
    * the correct number of times during testing.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // Setup
    * stubs.addResponse("getUserById", userInstance)
    *
    * // Execute code under test
    * val user1 = service.getUser("123")
    * val user2 = service.getUser("456")
    *
    * // Verify
    * stubs.callCount("getUserById") shouldBe 2
    * stubs.callCount("deleteUser") shouldBe 0 // Verify it wasn't called
    * ```
    *
    * @param stubKey The operation name to check (matches the key used in addResponse calls)
    * @return The number of times the operation has been invoked, or 0 if never called
    */
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
      val responseKey = resolveKey(responses, operation)
      val handlerKey = resolveKey(handlers, operation)
      val flowHandlerKey = resolveKey(flowHandlers, operation)

      val stubResponseKey =  responseKey ?: handlerKey ?: flowHandlerKey ?: operation.name

      val paramValues = parameters.map { it.second }
      invocations[stubResponseKey] = paramValues
      synchronized(calls) {
         calls.put(stubResponseKey, paramValues)
      }


      if (responseKey == null && handlerKey == null && flowHandlerKey == null && wildcardHandler == null) {
         throw IllegalArgumentException("No stub response or handler prepared for operation $stubResponseKey")
      }
      val stubResponse = when {
         responseKey != null -> {
            unwrapTypedCollections(responses[responseKey]!!)
         }

         handlerKey != null -> {
            unwrapTypedCollections(handlers[handlerKey]!!.invoke(operation, parameters))
         }

         flowHandlerKey != null -> {
            flowHandlers[flowHandlerKey]!!.invoke(operation, parameters)
         }
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
                        verb = "STUB",
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

   /**
    * Adds a dynamic response handler for an operation with conditional logic.
    *
    * This is the most flexible way to configure responses, allowing you to implement
    * custom logic based on the operation and parameters received.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // Conditional response based on parameter values
    * stubs.addResponse("findUserByStatus") { operation, parameters ->
    *     val status = parameters.first().second.value as String
    *     when (status) {
    *         "ACTIVE" -> listOf(activeUser.right())
    *         "INACTIVE" -> listOf(inactiveUser.right())
    *         else -> emptyList()
    *     }
    * }
    *
    * // Dynamic response with multiple parameters
    * stubs.addResponse("searchProducts") { operation, parameters ->
    *     val category = parameters[0].second.value as String
    *     val minPrice = parameters[1].second.value as Double
    *
    *     productDatabase
    *         .filter { it.category == category && it.price >= minPrice }
    *         .map { it.right() }
    * }
    * ```
    *
    * @param stubOperationKey The operation name that this handler will respond to
    * @param modifyDataSource Whether to modify the data source for lineage tracking (default: false)
    * @param handler Function that receives operation and parameters, returns list of responses
    * @return This StubService instance for method chaining
    *
    * @see addResponse(String, TypedInstance) For simple static responses
    * @see addResponse(String, String) For JSON-based responses
    */
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

   /**
    * Configures an operation to throw an exception when invoked.
    *
    * This is useful for testing error handling scenarios and ensuring your code
    * properly handles failures from external services.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // Test service unavailable scenario
    * stubs.addResponseThrowing(
    *     "getUserById",
    *     RuntimeException("Service temporarily unavailable")
    * )
    *
    * // Test validation errors
    * stubs.addResponseThrowing(
    *     "createUser",
    *     IllegalArgumentException("Invalid email format")
    * )
    *
    * // Verify error handling
    * assertThrows<RuntimeException> {
    *     userService.getUser("123")
    * }
    * ```
    *
    * @param stubOperationKey The operation name that should throw the exception
    * @param error The exception to throw when the operation is invoked
    */
   fun addResponseThrowing(stubOperationKey: String, error: Throwable) {
      addResponse(stubOperationKey) { _, _ ->
         throw error
      }
   }

   /**
    * Adds a streaming response handler that returns a Flow of responses.
    *
    * This method is designed for operations that need to return streaming data
    * or multiple responses over time, such as real-time updates or batch processing.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // Stream of user updates
    * stubs.addResponseFlow("watchUserUpdates") { operation, parameters ->
    *     flowOf(
    *         user1.right(),
    *         user2.right(),
    *         user3.right()
    *     )
    * }
    *
    * // Real-time price updates
    * stubs.addResponseFlow("subscribeToPriceUpdates") { operation, parameters ->
    *     val symbol = parameters.first().second.value as String
    *     flow {
    *         repeat(5) { i ->
    *             val price = TypedInstance.from(
    *                 priceType,
    *                 mapOf("symbol" to symbol, "price" to (100.0 + i)),
    *                 schema
    *             )
    *             emit(price.right())
    *             delay(100) // Simulate real-time updates
    *         }
    *     }
    * }
    * ```
    *
    * @param stubOperationKey The operation name that this flow handler will respond to
    * @param modifyDataSource Whether to modify the data source for lineage tracking (default: false)
    * @param handler Function that returns a Flow of responses
    * @return This StubService instance for method chaining
    *
    * @see addResponse For single response operations
    */
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
    * Adds multiple responses where the returned value is determined by the first parameter of the operation.
    *
    * This method is perfect for operations that behave like lookups or have different responses
    * based on input parameters, such as user lookups by ID or product searches by category.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // User lookup by role
    * stubs.addResponsesByParameter(
    *     "getUserByRole",
    *     mapOf(
    *         "ADMIN" to """{"id": 1, "name": "Admin User", "permissions": ["ALL"]}""",
    *         "USER" to """{"id": 2, "name": "Regular User", "permissions": ["READ"]}""",
    *         "GUEST" to """{"id": 3, "name": "Guest User", "permissions": []}"""
    *     )
    * )
    *
    * // Product lookup by category
    * stubs.addResponsesByParameter(
    *     "getProductsByCategory",
    *     mapOf(
    *         "ELECTRONICS" to """[{"id": 1, "name": "Laptop"}, {"id": 2, "name": "Phone"}]""",
    *         "BOOKS" to """[{"id": 3, "name": "Programming Guide"}]"""
    *     ),
    *     modifyDataSource = true
    * )
    * ```
    *
    * **Note:** This method uses the **first parameter** of the operation to determine which response to return.
    *
    * @param stubOperationKey The operation name to configure
    * @param responses A map where keys are parameter values and values are JSON strings of responses
    * @param modifyDataSource Whether to modify the data source for lineage tracking (default: false)
    *
    * @throws IllegalArgumentException if the operation is not found in the schema
    * @throws RuntimeException if no response is configured for a given parameter value
    */
   fun addResponsesByParameter(
      stubOperationKey: String,
      /**
       * A map of parameter value to Json string of the return value
       */
      responses: Map<Any, String>, modifyDataSource: Boolean = false
   ) {
      val operation = findOperationByKey(stubOperationKey)
         ?: error("Cannot stub $stubOperationKey as it's not a valid operation")
      val responseTypedInstances = responses.mapValues { (_, json) ->
         parseJson(schema!!, operation.returnType.paramaterizedName, json)
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

   fun addResponseEmitter(stubOperationKey: String): ResponseEmitter {
      val operation = schema!!.streamOperations.firstOrNull { it.name == stubOperationKey || it.qualifiedName.parameterizedName == stubOperationKey }
         ?: findOperationByKey(stubOperationKey)
         ?: error("Cannot stub $stubOperationKey as it's not a valid operation")
      val type = if (operation.returnType.isStream) {
         operation.returnType.typeParameters[0]
      } else {
         operation.returnType
      }
      val responseEmitter = ResponseEmitter(schema, type)
      addResponseFlow(stubOperationKey) { _, _ -> responseEmitter.flow }
      return responseEmitter
   }

   /**
    * Adds a JSON-based response for an operation.
    *
    * This is one of the most commonly used methods for configuring responses. The JSON string
    * is automatically parsed according to the operation's return type from the schema.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // Single object response
    * stubs.addResponse(
    *     "getUserById",
    *     """{"id": 123, "name": "John Doe", "email": "john@example.com"}"""
    * )
    *
    * // Array response
    * stubs.addResponse(
    *     "getAllUsers",
    *     """[
    *         {"id": 1, "name": "Alice"},
    *         {"id": 2, "name": "Bob"}
    *     ]""",
    *     modifyDataSource = true
    * )
    *
    * // Complex nested object
    * stubs.addResponse(
    *     "getOrderWithItems",
    *     """{
    *         "orderId": "ORDER-123",
    *         "customer": {"id": 1, "name": "Customer"},
    *         "items": [
    *             {"productId": "PROD-1", "quantity": 2, "price": 29.99}
    *         ]
    *     }"""
    * )
    * ```
    *
    * @param stubOperationKey The operation name to configure
    * @param json The JSON string representing the response data
    * @param modifyDataSource Whether to modify the data source for lineage tracking (default: false)
    *
    * @throws IllegalArgumentException if the operation is not found in the schema
    * @throws RuntimeException if the JSON cannot be parsed according to the operation's return type
    */
   fun addResponse(stubOperationKey: String, json: String, modifyDataSource: Boolean = false) {
      val operation = findOperationByKey(stubOperationKey)
         ?: error("Cannot stub $stubOperationKey as it's not a valid operation")
      val response = parseJson(schema!!, operation.returnType.paramaterizedName, json)
      addResponse(stubOperationKey, response, modifyDataSource)
   }

   /**
    * Adds a response for table-based "findMany" operations.
    *
    * This is a convenience method for configuring responses to table queries that return
    * collections of entities. The method automatically generates the correct operation name
    * following the pattern: `{tableName}_findMany{EntityType}`.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // For a "users" table returning User entities
    * stubs.addTableFindManyResponse(
    *     "users",
    *     """[
    *         {"id": 1, "name": "Alice", "email": "alice@example.com"},
    *         {"id": 2, "name": "Bob", "email": "bob@example.com"}
    *     ]"""
    * )
    * // This configures the operation "users_findManyUser"
    *
    * // For a "products" table returning Product entities
    * stubs.addTableFindManyResponse(
    *     "products",
    *     """[
    *         {"id": 101, "name": "Laptop", "price": 999.99},
    *         {"id": 102, "name": "Mouse", "price": 29.99}
    *     ]"""
    * )
    * // This configures the operation "products_findManyProduct"
    * ```
    *
    * @param tableName The name of the table (used to find the table operation in the schema)
    * @param json JSON array string representing the collection of entities to return
    *
    * @throws NoSuchElementException if no table operation exists for the given table name
    */
   fun addTableFindManyResponse(tableName: String, json: String) {
      val operation = schema!!.tableOperations.firstOrNull { it.name == tableName }
         ?: error("No table operation exists for table $tableName")
      val operationName = TableOperation.findManyOperationName(operation.name, operation.returnType.collectionTypeName ?: error("Operation ${operation.name} should return an array"))
      val response = parseJson(schema, operation.returnType.paramaterizedName, json)
      // people_findManyPerson
//      val operationName = "${tableName}_findMany${operation.returnType.collectionTypeName}"
      addResponse(operationName, response)
   }

   fun addTableFindOneResponse(tableName: String, json: String) {
      val operation = schema!!.tableOperations.firstOrNull { it.name == tableName }
         ?: error("No table operation exists for table $tableName")
      val singleResponseType = operation.returnType.collectionType?.qualifiedName
         ?: error("Expected table operation $tableName to return an array, but it returns ${operation.returnType.qualifiedName.shortDisplayName}")
      val response = parseJson(schema, singleResponseType.parameterizedName, json)
      // people_findOnePerson
      val operationName = TableOperation.findOneOperationName(operation.name, singleResponseType)
      addResponse(operationName, response)
   }

   fun addResponse(
      stubOperationKey: String,
      handler: OperationResponseHandler
   ): StubService {
      return addResponse(stubOperationKey, false, handler)
   }

   /**
    * Adds a response using a list of pre-constructed TypedInstance objects.
    *
    * This method is useful when you already have TypedInstance objects created elsewhere
    * or when you need fine-grained control over the response data and metadata.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // Create typed instances
    * val user1 = TypedInstance.from(userType, userData1, schema)
    * val user2 = TypedInstance.from(userType, userData2, schema)
    *
    * // Add as list response
    * stubs.addResponse("getAllUsers", listOf(user1, user2))
    *
    * // With data source tracking
    * stubs.addResponse(
    *     "getUsersWithHistory",
    *     listOf(user1, user2),
    *     modifyDataSource = true
    * )
    * ```
    *
    * @param stubOperationKey The operation name to configure
    * @param response List of TypedInstance objects to return
    * @param modifyDataSource Whether to modify the data source for lineage tracking (default: false)
    * @return This StubService instance for method chaining
    *
    * @see addResponse(String, TypedInstance) For single instance responses
    * @see addResponse(String, String) For JSON-based responses
    */
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


   /**
    * Adds a single TypedInstance as a response for an operation.
    *
    * This is the most direct way to configure a response when you already have a TypedInstance
    * object. Commonly used when you need to return a single entity or object.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // Create a user instance
    * val user = TypedInstance.from(
    *     userType,
    *     mapOf("id" to 123, "name" to "John Doe"),
    *     schema
    * )
    *
    * // Add as single response
    * stubs.addResponse("getUserById", user)
    *
    * // With data source tracking for lineage
    * stubs.addResponse("getCurrentUser", user, modifyDataSource = true)
    *
    * // Chaining multiple responses
    * stubs.addResponse("getAdmin", adminUser)
    *      .addResponse("getGuest", guestUser)
    *      .addResponse("getManager", managerUser)
    * ```
    *
    * @param stubOperationKey The operation name to configure
    * @param response The TypedInstance to return
    * @param modifyDataSource Whether to modify the data source for lineage tracking (default: false)
    * @return This StubService instance for method chaining
    *
    * @see addResponse(String, List<TypedInstance>) For multiple instance responses
    * @see addResponse(String, String) For JSON-based responses
    */
   fun addResponse(stubOperationKey: String, response: TypedInstance, modifyDataSource: Boolean = false): StubService {
      if (modifyDataSource) {
         addResponse(stubOperationKey, handler = justProvide(listOf(response)), modifyDataSource = true)
      } else {
         this.responses[stubOperationKey] = Either.Right(listOf(response))
      }

      return this
   }

   /**
    * Configures an operation to return one of its input parameters as the response.
    *
    * This method automatically finds a parameter whose type matches the operation's return type
    * and returns it as the response. This is useful for operations like "save" or "update"
    * where the operation returns the same object that was passed in.
    *
    * ## Example Usage
    *
    * ```kotlin
    * // For a "saveUser" operation that takes a User and returns a User
    * stubs.addResponseReturningInputs("saveUser")
    *
    * // For an "updateProduct" operation that takes a Product and returns a Product
    * stubs.addResponseReturningInputs("updateProduct")
    *
    * // Usage in test
    * val user = User(id = 123, name = "John")
    * val savedUser = userService.saveUser(user)
    * // savedUser will be the same User object that was passed in
    * ```
    *
    * **Note:** This method requires that exactly one parameter matches the operation's return type.
    *
    * @param stubOperationKey The operation name to configure
    * @return This StubService instance for method chaining
    *
    * @throws IllegalArgumentException if no parameter matches the return type
    * @throws IllegalArgumentException if multiple parameters match the return type
    */
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
      val operationFullName = operation.qualifiedName.parameterizedName
      return this.responses.containsKey(operation.name) ||
         this.responses.containsKey(operationFullName) ||
         this.handlers.containsKey(operation.name) ||
         this.handlers.containsKey(operationFullName) ||
         this.flowHandlers.containsKey(operation.name) ||
         this.flowHandlers.containsKey(operationFullName) ||
         this.wildcardHandler != null
   }

   /**
    * Configures the StubService to automatically generate mock responses for ALL operations.
    *
    * This method sets up a wildcard handler that will automatically create mock TypedInstance
    * objects for any operation that doesn't have a specific response configured. The mock
    * data is generated based on the operation's return type from the schema.
    *
    * This is particularly useful for:
    * - **Query plan generation** - Generate execution plans without needing to configure every operation
    * - **Rapid prototyping** - Quickly test complex queries without extensive setup
    * - **Schema exploration** - Understand query behavior across entire schemas
    *
    * ## Example Usage
    *
    * ```kotlin
    * val (vyne, stubs) = StubService.stubbedVyne(schema)
    *
    * // Enable automatic mock responses for all operations
    * stubs.returnStubValuesForAllOperations()
    *
    * // Now you can execute any query without pre-configuring responses
    * val users = vyne.query("find { User }")
    * val products = vyne.query("find { Product  }")
    * val orders = vyne.query("find { Order }")
    *
    * // Specific operations can still be overridden
    * stubs.addResponse("getUserById", specificUserInstance)
    * ```
    *
    * **Important:** This method enables data source tracking automatically to provide
    * meaningful operation metadata in the generated responses.
    *
    * @see MockTypedInstanceBuilder For details on how mock data is generated
    */
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


/**
 * Class which allows convenient emitting of results for stubbed
 * stream operations
 */
class ResponseEmitter(private val schema: Schema, val resultType: Type) {
   val flow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 1000)

   fun next(json: String): Boolean {
      val typedInstance = parseJson(schema, resultType.paramaterizedName, json)
      return flow.tryEmit(typedInstance.right())
   }

   fun next(typedInstance: TypedInstance): Boolean {
      return flow.tryEmit(typedInstance.right())
   }

   fun error(message: String): Boolean {
      val error = StreamErrorMessage(
         Instant.now(),
         RuntimeException(message),
         message,
         resultType.paramaterizedName,
         "",
      )
      return flow.tryEmit(error.left())
   }

   fun error(error: StreamErrorMessage): Boolean {
      return flow.tryEmit(error.left())
   }
}
