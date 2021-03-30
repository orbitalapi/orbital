package io.vyne.spring.invokers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.http.UriVariableProvider
import io.vyne.http.UriVariableProvider.Companion.buildRequestBody
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.UndefinedSource
import io.vyne.query.ProfilerOperation
import io.vyne.query.RemoteCall
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.*
import io.vyne.spring.hasHttpMetadata
import io.vyne.spring.isServiceDiscoveryClient
import io.vyne.utils.orElse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

class RestTemplateInvoker(val schemaProvider: SchemaProvider,
                          val webClient: WebClient,
                          private val serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver()),
                          private val enableDataLineageForRemoteCalls: Boolean) : OperationInvoker {

   @Autowired
   constructor(schemaProvider: SchemaProvider,
               webClientBuilder: WebClient.Builder,
               serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver()),
               @Value("\${vyne.data-lineage.remoteCalls.enabled:false}") enableDataLineageForRemoteCalls: Boolean)
      : this(schemaProvider, webClientBuilder.exchangeStrategies(
         ExchangeStrategies.builder().codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }.build()
      ).build(), serviceUrlResolvers, enableDataLineageForRemoteCalls)

   private val uriVariableProvider = UriVariableProvider()
   private val defaultUriBuilderFactory = DefaultUriBuilderFactory()

   init {
      log().info("Rest template invoker starter")
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.isServiceDiscoveryClient() || operation.hasHttpMetadata()
   }


   override fun invoke(service: Service, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation): Flow<TypedInstance> {
      log().debug("Invoking Operation ${operation.name} with parameters: ${parameters.joinToString(",") { (_, typedInstance) -> typedInstance.type.fullyQualifiedName + " -> " + typedInstance.toRawObject() }}")

      val (_, url, method) = operation.httpOperationMetadata()
      val httpMethod = HttpMethod.resolve(method)
      //val httpResult = profilerOperation.startChild(this, "Invoke HTTP Operation", OperationType.REMOTE_CALL) { httpInvokeOperation ->

         val absoluteUrl = makeUrlAbsolute(service, operation, url)
         val uriVariables = uriVariableProvider.getUriVariables(parameters, url)

         log().debug("Operation ${operation.name} resolves to $absoluteUrl")
         //httpInvokeOperation.addContext("AbsoluteUrl", absoluteUrl)

         val requestBody = buildRequestBody(operation, parameters.map { it.second })
         //httpInvokeOperation.addContext("Service", service)
         //httpInvokeOperation.addContext("Operation", operation)

         val expandedUri = defaultUriBuilderFactory.expand(absoluteUrl,uriVariables)

      println("Calling : ${expandedUri}")

         //TODO - On upgrade to Spring boot 2.4.X replace usage of exchange with exchangeToFlow LENS-473
         val requset = webClient
            .method(httpMethod)
            .uri(absoluteUrl, uriVariables)
            .contentType(MediaType.APPLICATION_JSON)
            if (requestBody.first.hasBody()) {
               requset.bodyValue(requestBody.first.body)
            }

         val results = requset
            .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
            .exchange()
            .metrics()
            .publishOn(Schedulers.elastic())
            .flatMapMany { clientResponse ->
               (

                  if (clientResponse.headers().contentType().orElse(MediaType.APPLICATION_JSON)
                        .isCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                  ) {
                     clientResponse.bodyToFlux(String::class.java)
                  } else {
                     // Assume the response is application/json
                        clientResponse.bodyToMono(typeReference<Any>())
                        //TODO This is not right we should marshall to a list of T, not, Object then back to String
                        .flatMapMany {
                           if (it is ArrayList<*>) {
                              Flux.fromIterable(it as List<*>)
                           } else {
                              Flux.just(it)
                           }
                        }
                        .map {
                              jacksonObjectMapper().writeValueAsString(it)
                        }
                  }
                  )

                  .map {

                     //TODO - Should we create a remote call for each response?
                     val remoteCall = RemoteCall(
                        service.name,
                        expandedUri.toASCIIString(),
                        operation.name,
                        operation.returnType.name,
                        httpMethod.name,
                        requestBody.first.body,
                        clientResponse.rawStatusCode(),
                        20,
                        it
                     )

                     handleSuccessfulHttpResponse(
                        it.toString(),
                        operation,
                        parameters,
                        remoteCall,
                        clientResponse.headers()
                     )
                  }
            }
      //}

      return results.asFlow()

   }

   private fun handleSuccessfulHttpResponse(result: String, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, remoteCall: RemoteCall, headers: ClientResponse.Headers): TypedInstance {
      // TODO : Handle scenario where we get a 2xx response, but no body

      log().debug("Result of ${operation.name} was $result")

      val isPreparsed = headers
         .header(io.vyne.http.HttpHeaders.CONTENT_PREPARSED).let { headerValues ->
            headerValues != null && headerValues.isNotEmpty() && headerValues.first() == true.toString()
         }
      // If the content has been pre-parsed upstream, we don't evaluate accessors
      val evaluateAccessors = !isPreparsed
      val dataSource = remoteCallDataLineage(parameters, remoteCall)

      var type: Type?

      if (operation.returnType.collectionType != null) {
         type = operation.returnType.collectionType!!
      } else {
         type = operation.returnType
      }

      val instance =  TypedInstance.from(type!!, result, schemaProvider.schema(), source = dataSource, evaluateAccessors = evaluateAccessors)

      return instance
   }


   private fun remoteCallDataLineage(parameters: List<Pair<Parameter, TypedInstance>>, remoteCall: RemoteCall): DataSource {
      return if (enableDataLineageForRemoteCalls) {
         // TODO: WE should be validating that the response we received conforms with the expected schema,
         // and doing...something? if it doesn't.
         // See https://gitlab.com/vyne/vyne/issues/54
         OperationResult(remoteCall, parameters.map { (param, instance) ->
            OperationResult.OperationParam(param.name.orElse("Unnamed"), instance)
         })
      } else {
         UndefinedSource
      }
   }

   private fun makeUrlAbsolute(service: Service, operation: RemoteOperation, url: String): String {
      return this.serviceUrlResolvers.first { it.canResolve(service, operation) }.makeAbsolute(url, service, operation)
   }
}

