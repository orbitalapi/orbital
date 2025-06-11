package com.orbitalhq.playground

import com.google.common.cache.CacheBuilder
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.Vyne
import com.orbitalhq.cockpit.core.query.QueryPlanEventHandler
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.formats.csv.CsvAnnotationSpec
import com.orbitalhq.formats.xml.XmlAnnotationSpec
import com.orbitalhq.history.LineageJsonSerializer
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.history.ResultRowPersistenceStrategyFactory
import com.orbitalhq.history.remote.RemoteQueryEventConsumerClient
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.right
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.QueryProfileData
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.query.history.RemoteCallResponse
import com.orbitalhq.query.history.RemoteCallResponseDto
import com.orbitalhq.query.history.toDto
import com.orbitalhq.query.runtime.core.ModelFormatSpecSerializer
import com.orbitalhq.query.runtime.core.RawResultsSerializer
import com.orbitalhq.query.tracing.TraceContext
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.utils.Ids
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import lang.taxi.annotations.HttpService
import lang.taxi.query.QueryMode
import lang.taxi.types.Arrays
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

typealias ContentTypeString = String

class StubQueryService(
   private val streamDelay: Duration = Duration.ofMillis(500),
   private val formatSpecs: List<ModelFormatSpec> = FormatSpecRegistry.default().formats,
) {

   private val queryProfileCache = CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(30))
      .build<String, QueryProfileData>()

   companion object {
      private val logger = KotlinLogging.logger {}
      val builtInTypes: String = listOf(
         VyneQlGrammar.QUERY_TYPE_TAXI,
         CsvAnnotationSpec.taxi,
         XmlAnnotationSpec.taxi,
         ErrorType.ErrorTypeDefinition,
         HttpService.asTaxi()
      ).joinToString("\n")

      val builtInTypesSourcePackage = SourcePackage(
         PackageMetadata.from("org.taxilang", "taxiql", "0.1.0"),
         listOf(
            VersionedSource.sourceOnly(builtInTypes)
         )
      )
   }


   fun submitQuery(
      query: StubQueryMessage,
      queryId: String = Ids.id(prefix = "query", size = 12),

      /**
       * When query calls a stream, this will add a delay on each
       * message, to simulate a 'streaming' response.
       * If false, all data is returned instantly
       */
      addDelayToStreams: Boolean = false
   ): Pair<Publisher<Any>, ContentTypeString> {
      val schema = TaxiSchema.fromStrings(query.schema, builtInTypes)
      val (vyne, stub) = StubService.stubbedVyne(schema)
      return submitQuery(vyne, stub, query, queryId, addDelayToStreams)
   }

   fun submitQuery(
      vyne: Vyne,
      stub: StubService,
      query: StubQueryMessage,
      queryId: String = Ids.id(prefix = "query", size = 12),
      /**
       * When query calls a stream, this will add a delay on each
       * message, to simulate a 'streaming' response.
       * If false, all data is returned instantly
       */
      addDelayToStreams: Boolean = false,
   ): Pair<Publisher<Any>, ContentTypeString> {
      configureStubs(query, vyne, stub, addDelayToStreams)

      val (dispatcher, remoteCallCollector) = buildQueryEventConsumer()
      val queryPlanEventHandler = QueryPlanEventHandler.createFor(vyne.schema)
      val eventBroker = QueryContextEventBroker(traceContext = TraceContext.noOp())
         .addHandlers(listOf(remoteCallCollector, queryPlanEventHandler))
      val resultFlux = runBlocking {
         vyne.query(query.query, arguments = query.parameters, eventBroker = eventBroker)
            .results
            .asFlux()
            .doOnNext { typedInstance -> queryPlanEventHandler.appendResult(typedInstance) }
      }
         .doFinally {
            (dispatcher.executor as ThreadPoolExecutor).shutdown()
            captureQueryProfileData(queryId, remoteCallCollector, queryPlanEventHandler)
         }

      val (taxiQlQuery, queryOptions, querySchema) = vyne.parseQuery(query.query)
      val queryResultType = taxiQlQuery.returnType
      val formatSerializer =
         FormatDetector(formatSpecs).getFormatType(querySchema.type(taxiQlQuery.returnType))?.let { (metadata, spec) ->
            ModelFormatSpecSerializer(spec, metadata)
         } ?: RawResultsSerializer(queryOptions)

      val serializedResults = resultFlux
         .filter { it !is TypedNull }
         .map {
            formatSerializer.serialize(it, querySchema)
         }.filter { it != null } as Flux<Any>
      val publisher = when {
         taxiQlQuery.queryMode == QueryMode.STREAM -> serializedResults
         Arrays.isArray(queryResultType) -> serializedResults
         else -> serializedResults.singleOrEmpty()
      }

      return publisher to formatSerializer.contentType
   }

   private fun captureQueryProfileData(
      queryId: String,
      remoteCallCollector: RemoteCallResponseCollector,
      queryPlanEventHandler: QueryPlanEventHandler
   ) {
      val profileData = QueryProfileData(
         queryId,
         0,
         remoteCalls = remoteCallCollector.events,
         operationStats = emptyList(),
         queryLineageData = queryPlanEventHandler.sankeyViewBuilder.asChartRows(queryId)
      )
      this.queryProfileCache.put(queryId, profileData)
   }

   private fun buildQueryEventConsumer(): Pair<ExecutorCoroutineDispatcher, RemoteCallResponseCollector> {
      val historyDispatcher = Executors
         .newFixedThreadPool(1)
         .asCoroutineDispatcher()
      val config = QueryAnalyticsConfig(
         persistResults = true,
         persistRemoteCallResponses = true,
         persistRemoteCallMetadata = true
      )
      val client = RemoteQueryEventConsumerClient(
         ResultRowPersistenceStrategyFactory.resultRowPersistenceStrategy(
            LineageJsonSerializer.objectMapper,
            null,
            config
         ),
         config,
         CoroutineScope(historyDispatcher)
      )
      return historyDispatcher to RemoteCallResponseCollector(client)
   }

   /**
    * Collects RemoteCallResponse from a query event stream, without persisting them
    */
   class RemoteCallResponseCollector(val eventClient: RemoteQueryEventConsumerClient) :
      QueryEventConsumer by eventClient {
      private val _events = mutableListOf<RemoteCallResponse>()

      init {
         eventClient.queryEvents()
            .filter { it is RemoteCallResponse }
            .subscribe { _events.add(it as RemoteCallResponse) }
      }

      val events: List<RemoteCallResponseDto>
         get() {
            return _events.distinctBy { it.remoteCallId }.map { it.toDto() }
         }
   }

   private fun configureStubs(
      query: StubQueryMessage,
      vyne: Vyne,
      stub: StubService,
      addDelayToStreams: Boolean
   ) {
      query.stubs.forEach { operationStub ->
         val operation = vyne.schema.services
            .singleOrNull { it.hasRemoteOperation(operationStub.operationName) }
            ?.remoteOperation(operationStub.operationName)

         if (operation != null) {
            if (operation.returnType.isStream) {
               configureStubStream(operation, vyne, operationStub, stub, addDelayToStreams)
            } else {
               when {
                  operationStub.echoInput -> stub.addResponseReturningInputs(operationStub.operationName)
                  operationStub.conditionalResponses.isNotEmpty() -> {
                     configureConditionalResponses(stub, operationStub, vyne)
                  }

                  else -> {
                     val parsedInstance = TypedInstance.from(
                        operation.returnType,
                        operationStub.response,
                        vyne.schema,
                        formatSpecs = formatSpecs
                     )
                     stub.addResponse(operationStub.operationName, parsedInstance, modifyDataSource = true)
                  }
               }

            }
         } else {
            logger.warn { "Received a stub for operation ${operationStub.operationName} but such operation was found in the schema" }
         }
      }
   }

   private fun configureConditionalResponses(
      stub: StubService,
      operationStub: OperationStub,
      vyne: Vyne
   ) {
      stub.addResponse(operationStub.operationName, modifyDataSource = true) { operation, params ->

         // Look at the stub configuration, and find a response which
         // matches the inputs we've received
         val response = operationStub.conditionalResponses.firstOrNull { response ->
            val conditionInputsMatch = params.all { (parameter, value) ->
               val conditionParam =
                  response.inputs.firstOrNull { it.name == parameter.name } ?: return@all false
               conditionParam.value == value.toRawObject()
            }
            conditionInputsMatch
         }
         if (response == null) {
            error(
               "No response configured for inputs ${
                  params.mapIndexed { idx, paramPair ->
                     val name = paramPair.first.name ?: "p$idx"
                     "$name = ${paramPair.second.toRawObject()}"
                  }
               }"
            )
         }
         val parsedResponse = TypedInstance.from(
            operation.returnType,
            response.response.body,
            vyne.schema,
            formatSpecs = formatSpecs
         )
         if (parsedResponse is TypedCollection) {
            parsedResponse.map { it.right() }
         } else {
            listOf(parsedResponse.right())
         }
      }
   }

   private fun configureStubStream(
      operation: RemoteOperation,
      vyne: Vyne,
      operationStub: OperationStub,
      stub: StubService,
      addDelayToStreams: Boolean
   ) {
      FormatDetector.get(specs = formatSpecs)
         .getFormatType(operation.returnType.typeParameters[0])
      val collectionType = operation.returnType.typeParameters[0].asArrayType()
      val result = vyne.parseJson(collectionType.paramaterizedName, operationStub.response)
      require(result is TypedCollection) { "Operation ${operationStub.operationName} is a stream, so stubbed results should be provided as an array" }
      stub.addResponseFlow(operationStub.operationName) { _, _ ->
         flow {
            result.forEach {
               emit(it.right())
               if (addDelayToStreams) {
                  delay(streamDelay.toMillis())
               }
            }
         }
      }
   }

   fun getAndPurgeProfileData(queryId: String): QueryProfileData? {
      val profileData = queryProfileCache.getIfPresent(queryId)
      queryProfileCache.invalidate(queryId)
      return profileData
   }
}
