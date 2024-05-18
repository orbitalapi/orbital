package com.orbitalhq.playground

import com.orbitalhq.formats.csv.CsvAnnotationSpec
import com.orbitalhq.formats.xml.XmlAnnotationSpec
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.testVyne
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import lang.taxi.query.QueryMode
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.Arrays
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.time.Duration

class StubQueryService(private val streamDelay: Duration = Duration.ofMillis(500)) {
   val builtInTypes = listOf(
      VyneQlGrammar.QUERY_TYPE_TAXI,
      CsvAnnotationSpec.taxi,
      XmlAnnotationSpec.taxi
   ).joinToString("\n")

   fun parseQuery(query: StubQueryMessage): TaxiQlQuery {
      val (vyne) =  testVyne(query.schema, builtInTypes)
      val (query) = vyne.parseQuery(query.query)
      return query
   }


   fun submitQuery(query: StubQueryMessage): Publisher<Any> {
      val (vyne, stub) = testVyne(query.schema, builtInTypes)
      query.stubs.forEach { operationStub ->
         val operation = vyne.schema.services
            .filter { it.hasRemoteOperation(operationStub.operationName) }
            .single()
            .remoteOperation(operationStub.operationName)

         if (operation.returnType.isStream) {
            val collectionType = operation.returnType.typeParameters[0].asArrayType()
            val result = vyne.parseJson(collectionType.paramaterizedName, operationStub.response)
            require(result is TypedCollection) { "Operation ${operationStub.operationName} is a stream, so stubbed results should be provided as an array" }
            stub.addResponseFlow(operationStub.operationName) { _, _ ->
               flow {
                  result.forEach {
                     emit(it)
                     delay(streamDelay.toMillis())
                  }
               }
            }
         } else {
            val result = vyne.parseJson(operation.returnType.paramaterizedName, operationStub.response)
            stub.addResponse(operationStub.operationName, result)
         }

      }

      val resultFlux = runBlocking {
         vyne.query(query.query, arguments = query.parameters)
            .results
            .asFlux()
            .mapNotNull { it.toRawObject() } as Flux<Any>
      }
      val (taxiQlQuery, _, _) = vyne.parseQuery(query.query)
      return when {
         taxiQlQuery.queryMode == QueryMode.STREAM -> resultFlux
         Arrays.isArray(taxiQlQuery.returnType.toQualifiedName()) -> resultFlux
         else -> resultFlux.singleOrEmpty()
      }
   }
}
