package com.orbitalhq.connectors.soap

import arrow.core.Either
import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.cache.CacheBuilder
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.HttpExchange
import com.orbitalhq.query.HttpHeaders
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.HttpRequest
import com.orbitalhq.query.tracing.HttpResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schemas.OperationInvocationException
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import lang.taxi.generators.soap.SoapAnnotations
import lang.taxi.generators.soap.SoapLanguage
import mu.KotlinLogging
import org.apache.cxf.endpoint.Client
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory
import org.apache.cxf.interceptor.Fault
import reactor.core.publisher.Flux
import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}
class SoapClientCache(
   private val inboundInterceptor: InboundPayloadCapturingInterceptor = InboundPayloadCapturingInterceptor(),
   private val outboundInterceptor: OutboundPayloadCapturingInterceptor = OutboundPayloadCapturingInterceptor(),
   private val clientFactory: DynamicClientFactory = DynamicClientFactory.newInstance(),
   schemaChangedEventProvider: SchemaChangedEventProvider? = null
) {
   private val cache = CacheBuilder.newBuilder()
      .build<QualifiedName, Client>()


   init {
      if (schemaChangedEventProvider != null) {
         Flux.from(schemaChangedEventProvider.schemaChanged)
            .subscribe {
               logger.info { "Schema changed.  Invalidating soap clients" }
               cache.invalidateAll()
            }
         logger.info { "Soap client is listening for schema changes" }
      } else {
         logger.warn { "Soap client is not listening for schema changes - changes to schemas may result in out-of-date soap clients" }
      }
   }

   fun get(service: Service): Client {
      return cache.get(service.name) {

         // The only way to create a CXF dynamic client is by loading the WSDL from a URL.
         // So, we have to write the wsdl out to a temp file first.
         val wsdlSource = service.sourceCode.singleOrNull { it.language == SoapLanguage.WSDL }
            ?: error("Service ${service.name} is expected to have WSDL source attached, but it was not found")

         val tmpWsdlFile = Files.createTempFile("tmp-servicedef-${service.fullyQualifiedName}", "wsdl")
         tmpWsdlFile.writeText(wsdlSource.content)
         val tmpFileUrl = tmpWsdlFile.toUri().toURL()

         val client = clientFactory.createClient(tmpFileUrl)
         client.inInterceptors.add(inboundInterceptor)
         client.outInterceptors.add(outboundInterceptor)
         logger.info { "Created new SoapClient for service ${service.fullyQualifiedName}" }
         client
      }
   }
}


class SoapInvoker(
   val schemaProvider: SchemaProvider,
   private val clientCache: SoapClientCache = SoapClientCache(),
) : OperationInvoker {


   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(SoapAnnotations.SERVICE_ANNOTATION)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {

      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, "")
      val soapClient = clientCache.get(service)
      require(parameters.size <= 1) { "SOAP services expect 0 or 1 parameters, but got ${parameters.size}" }
      val parameterValues = paramToOrderedArray(parameters.singleOrNull())
      try {

         OutboundPayloadCapturingInterceptor.resetCapturedPayload()
         InboundPayloadCapturingInterceptor.resetCapturedPayload()

         val timestamp = Instant.now()
         val (result: Any, duration: Duration) = measureTimedValue {
            // Emit request event before invoking
            val outboundPayload = if (parameterValues.isNotEmpty()) {
               Jackson.defaultObjectMapper.writeValueAsString(parameterValues.first())
            } else {
               "{}"
            }

            val resultList = soapClient.invoke(operation.name, *parameterValues)
            require(resultList.size == 1) {
               "Expected a single result, but got ${resultList.size}"
            }
            resultList.single()
         }
         val outboundMessage = OutboundPayloadCapturingInterceptor.getCapturedPayload()
         traceContext.emitEvent(
            kind = TracingEventKind.OK,
            spanState = SpanState.ACTIVE,
            payloadType = parameters.firstOrNull()?.first?.type,
            exchangeMetadata = HttpRequest(outboundMessage.url, outboundMessage.method,{ outboundMessage.payload },outboundMessage.payload.length.toLong(),emptyMap()),
            verb = outboundMessage.method
         )

         val inboundMessage = InboundPayloadCapturingInterceptor.getCapturedPayload()

         val resultEvent = traceContext.emitEvent(
            TracingEventKind.OK,
            SpanState.COMPLETE,
            operation.returnType,
            HttpResponse(200, { inboundMessage.payload }, inboundMessage.payload.length.toLong(), emptyMap()),
            "Invoke response"
         )

         val schema = schemaProvider.schema

         val remoteCall = buildRemoteCall(
            service, operation, outboundMessage, inboundMessage, duration, timestamp
         )
         val operationResult = OperationResult.from(parameters, remoteCall)

         eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
         val resultTypedInstance = try {
            Either.Right(createTypedInstance(schema, result, operation, operationResult, resultEvent))
         } catch (e: Exception) {
            Either.Left(StreamErrorMessage.fromException(e, operation.returnType.paramaterizedName))
         }

         return flowOf(resultTypedInstance)
      } catch (e: Exception) {
         val (message, responseCode) = when (e) {
            is Fault -> (e.cause?.message ?: e.message) to e.statusCode
            else -> e.message to -1
         }

         traceContext.emitEvent(
            TracingEventKind.ERROR,
            SpanState.COMPLETE,
            null,
            HttpResponse(responseCode, { message ?: "Unknown error" }, (message?.length ?: 0).toLong(), emptyMap()),
            "Invoke error"
         )

         val remoteCall = RemoteCall(
            service = service.name,
            operation = operation.name,
            address = "Unknown",
            responseTypeName = operation.returnType.name,
            durationMs = 0,
            timestamp = Instant.now(),
            responseMessageType = ResponseMessageType.FULL,
            exchange = HttpExchange(
               url = "",
               verb = "",
               requestBody = null,
               responseCode = responseCode,
               responseSize = 0,
               headers = HttpHeaders.empty()
            ),
            response = message,
            isFailed = true
         )
         val operationResult = OperationResult.from(parameters, remoteCall)
         eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
         throw OperationInvocationException(
            "Failed to invoke service ${operation.name}  - $message",
            0,
            remoteCall,
            parameters
         )
      }
   }

   private fun createTypedInstance(
      schema: Schema,
      result: Any,
      operation: RemoteOperation,
      source: OperationResult,
      resultEvent: com.orbitalhq.query.tracing.TracingEvent
   ): TypedInstance {
      // See SoapNamingStrategy for why we have to instatiate a new object mapper each time.
      val mapper = Jackson.newObjectMapperWithDefaults()
      mapper.propertyNamingStrategy = SoapNamingStrategy(schema)
      val resultAsMap = mapper.convertValue<Any>(result)

      return TypedInstance.from(
         operation.returnType, resultAsMap, schema, source = source.asOperationReferenceDataSource(resultEvent.idSet)
      )
   }

   private fun buildRemoteCall(
      service: Service,
      operation: RemoteOperation,
      outboundMessage: OutboundSoapRequest,
      inboundMessage: InboundSoapResponse,
      duration: Duration,
      timestamp: Instant,
      failed: Boolean = false
   ): RemoteCall {
      return RemoteCall(
         service = service.name,
         address = outboundMessage.url,
         operation = operation.name,
         responseTypeName = operation.returnType.name,
         durationMs = duration.inWholeMilliseconds,
         timestamp = timestamp,
         responseMessageType = ResponseMessageType.FULL,
         exchange = HttpExchange(
            outboundMessage.url,
            outboundMessage.method,
            outboundMessage.payload,
            inboundMessage.resultCode,
            inboundMessage.payload.length,
            HttpHeaders.empty()
         ),
         response = inboundMessage.payload,
         isFailed = failed
      )
   }

   /**
    * It seems that SOAP doesn't take named arguments,
    * instead expects them delivered in the same order as defined in the WSDL.
    *
    * (The above to be confirmed, as it seems suss).
    *
    * Therefore, this converts the params to a list in the correct order
    */
   private fun paramToOrderedArray(
      paramAndValue: Pair<Parameter, TypedInstance>?
   ): Array<Any> {
      if (paramAndValue == null) {
         return arrayOf()
      }
      val (_, value) = paramAndValue
      require(value is TypedObject) { "Expected a TypedObject, but got ${value::class.java.simpleName}" }
      val rawValue = value.toRawObject()!! as Map<String, Any>
      if (rawValue.keys.size > 1) {
         TODO("Handle multiple keys")
      }
      return rawValue.values.toTypedArray()
   }
}
