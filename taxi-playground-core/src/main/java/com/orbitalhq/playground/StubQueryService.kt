package com.orbitalhq.playground

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.Vyne
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.formats.csv.CsvAnnotationSpec
import com.orbitalhq.formats.xml.XmlAnnotationSpec
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import com.orbitalhq.stubbing.StubService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import lang.taxi.annotations.HttpService
import lang.taxi.query.QueryMode
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.Arrays
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.time.Duration

class StubQueryService(
   private val streamDelay: Duration = Duration.ofMillis(500),
   private val formatSpecs: List<ModelFormatSpec> = FormatSpecRegistry.default().formats
) {
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
      /**
       * When query calls a stream, this will add a delay on each
       * message, to simulate a 'streaming' response.
       * If false, all data is returned instantly
       */
      addDelayToStreams: Boolean = false
   ): Publisher<Any> {

      val schema = TaxiSchema.fromStrings(query.schema, builtInTypes)
      val (vyne, stub) = StubService.stubbedVyne(schema)
      configureStubs(query, vyne, stub, addDelayToStreams)

      val resultFlux = runBlocking {
         vyne.query(query.query, arguments = query.parameters)
            .results
            .asFlux()
            .mapNotNull { it.toRawObject() } as Flux<Any>
      }
      val (taxiQlQuery, _, _) = vyne.parseQuery(query.query)
      val queryResultType = taxiQlQuery.discoveryType?.type ?: taxiQlQuery.returnType
      return when {
         taxiQlQuery.queryMode == QueryMode.STREAM -> resultFlux
         Arrays.isArray(queryResultType) -> resultFlux
         else -> resultFlux.singleOrEmpty()
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
               if (operationStub.conditionalResponses.isNotEmpty()) {
                  configureConditionalResponses(stub, operationStub, vyne)
               } else {
                  val parsedInstance = TypedInstance.from(
                     operation.returnType,
                     operationStub.response,
                     vyne.schema,
                     formatSpecs = formatSpecs
                  )
                  stub.addResponse(operationStub.operationName, parsedInstance)
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
      stub.addResponse(operationStub.operationName) { operation, params ->

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
            parsedResponse
         } else {
            listOf(parsedResponse)
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
      val collectionType = operation.returnType.typeParameters[0].asArrayType()
      val result = vyne.parseJson(collectionType.paramaterizedName, operationStub.response)
      require(result is TypedCollection) { "Operation ${operationStub.operationName} is a stream, so stubbed results should be provided as an array" }
      stub.addResponseFlow(operationStub.operationName) { _, _ ->
         flow {
            result.forEach {
               emit(it)
               if (addDelayToStreams) {
                  delay(streamDelay.toMillis())
               }
            }
         }
      }
   }
}
