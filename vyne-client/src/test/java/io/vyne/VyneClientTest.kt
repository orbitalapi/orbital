package io.vyne

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.query.Query
import io.vyne.query.TypeNameListQueryExpression
import lang.taxi.TypeNames
import lang.taxi.annotations.DataType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VyneClientTest {

   @Mock
   lateinit var queryService: VyneQueryService

   lateinit var client: VyneClient
   @Before
   fun setup() {
      this.client = VyneClient(queryService)
   }

   @Test
   fun when_queryingForListType_then_typeIsSubmittedCorrectly() {
      whenever(queryService.submitQuery(any(),any())).thenReturn(QueryClientResponse(false, emptyMap()))
      client.discover<List<Book>>()

      argumentCaptor<Query>().apply {
         verify(queryService).submitQuery(capture(), any())

         val query = lastValue
         expect(query.expression).to.equal(TypeNameListQueryExpression(listOf("lang.taxi.Array<foo.Book>")))
      }
   }

   @Test
   fun when_queryingForList_then_itIsResolveFromResponseCorrectly() {
      whenever(queryService.submitQuery(any(),any())).thenReturn(QueryClientResponse(true, mapOf("lang.taxi.Array<foo.Book>" to emptyList<Any>())))
      val response = client.discover<List<Book>>()
   }

   @Test
   fun when_queryingWithVyneQL_then_itIsWorwardedToQueryService() {
      val expectedRespone = QueryClientResponse(true, mapOf("lang.taxi.Array<foo.Book>" to emptyList<Any>()));
      whenever(queryService.submitVyneQl(any(), any())).thenReturn(expectedRespone)

      val response = client.submitVyneQl("findAll { foo.Book[] }")

      verify(queryService).submitVyneQl("findAll { foo.Book[] }")
      response.should.be.equal(expectedRespone)
   }

   @Test
   fun when_querying_then_ItIsWorwardedToQueryService() {
      val expectedRespone = QueryClientResponse(true, mapOf("lang.taxi.Array<foo.Book>" to emptyList<Any>()));
      whenever(queryService.submitQuery(any(),any())).thenReturn(expectedRespone)

      val query = Query(TypeNameListQueryExpression(listOf("Book")), emptyMap())
      val response = client.submitQuery(query)

      verify(queryService).submitQuery(query)
      response.should.be.equal(expectedRespone)
   }

   @Test
   fun `Can return vyneQL results a Map List`() {
      val titleMap = mapOf("title" to mapOf("value" to "War and Peace"))
      val resultItem = mapOf("value" to titleMap, "typeName" to TypeNames.deriveTypeName(Book::class.java))
      val results = listOf(resultItem)
      val expectedResponse = QueryClientResponse(true, mapOf("lang.taxi.Array<foo.Book>" to results));
      whenever(queryService.submitQuery(any(), any())).thenReturn(expectedResponse)
      val query = Query(TypeNameListQueryExpression(listOf("Book")), emptyMap())
      val response = client.submitQuery(query)
      val resultMapList = response.getResultMapListFor(Book::class.java)
      resultMapList.size.should.equal(1)
   }
}

@DataType("foo.Book")
data class Book(val title: String)
