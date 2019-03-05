package io.vyne

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.expect
import io.vyne.query.Query
import io.vyne.query.TypeNameListQueryExpression
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
      whenever(queryService.submitQuery(any())).thenReturn(QueryClientResponse(false, emptyMap()))
      client.discover<List<Book>>()

      argumentCaptor<Query>().apply {
         verify(queryService).submitQuery(capture())

         val query = lastValue
         expect(query.expression).to.equal(TypeNameListQueryExpression(listOf("foo.Book[]")))
      }
   }

   @Test
   fun when_queryingForList_then_itIsResolveFromResponseCorrectly() {
      whenever(queryService.submitQuery(any())).thenReturn(QueryClientResponse(true, mapOf("lang.taxi.Array<foo.Book>" to emptyList<Any>())))
      val response = client.discover<List<Book>>()
   }
}

@DataType("foo.Book")
data class Book(val title: String)
