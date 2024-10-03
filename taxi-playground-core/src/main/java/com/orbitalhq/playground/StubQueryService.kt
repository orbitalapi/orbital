package com.orbitalhq.playground

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.formats.csv.CsvAnnotationSpec
import com.orbitalhq.formats.xml.XmlAnnotationSpec
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.taxi.TaxiSchema
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

class StubQueryService(private val streamDelay: Duration = Duration.ofMillis(500)) {
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
      query.stubs.forEach { operationStub ->
         val operation = vyne.schema.services
            .singleOrNull { it.hasRemoteOperation(operationStub.operationName) }
            ?.remoteOperation(operationStub.operationName)

         if (operation != null) {
            if (operation.returnType.isStream) {
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
            } else {
               val result = vyne.parseJson(operation.returnType.paramaterizedName, operationStub.response)
               stub.addResponse(operationStub.operationName, result)
            }
         } else {
            logger.warn { "Received a stub for operation ${operationStub.operationName} but such operation was found in the schema" }
         }
      }

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
}
