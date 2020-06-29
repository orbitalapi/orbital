package io.vyne.cask.query

import arrow.core.Either
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.winterbe.expekt.should
import io.vyne.cask.CaskService
import io.vyne.schemas.VersionedType
import org.junit.Test
import org.springframework.http.HttpMethod
import org.springframework.http.ReactiveHttpInputMessage
import org.springframework.web.reactive.function.BodyExtractor
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono

class CaskApiHandlerTest {
   private val mockCaskService = mock<CaskService>()
   private val mockCaskDao = mock<CaskDAO>()
   @Test
   fun `handler can map request uri to relevant type`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao)
      val request = mock<ServerRequest>() {
         on { path() } doReturn "/api/cask/OrderWindowSummary/symbol/BTCUSD"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val projectionValueCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(Either.right(mockedVersionedType))
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBy(any(), columnNameCaptor.capture(), projectionValueCaptor.capture())
      "symbol".should.equal(columnNameCaptor.firstValue)
      "BTCUSD".should.equal(projectionValueCaptor.firstValue)
   }

   @Test
   fun `handler can map a between request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao)
      val request = mock<ServerRequest>() {
         on { path() } doReturn "/api/cask/OrderWindowSummary/orderDate/Between/2020-12-01/2020-12-20"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val startArgumentCaptor = argumentCaptor<String>()
      val endArgumentCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(Either.right(mockedVersionedType))
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBetween(any(), columnNameCaptor.capture(), startArgumentCaptor.capture(), endArgumentCaptor.capture())
      "orderDate".should.equal(columnNameCaptor.firstValue)
      "2020-12-01".should.equal(startArgumentCaptor.firstValue)
      "2020-12-20".should.equal(endArgumentCaptor.firstValue)
   }

   @Test
   fun `handler can map a after request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao)
      val request = mock<ServerRequest>() {
         on { path() } doReturn "/api/cask/OrderWindowSummary/orderDate/After/2020-12-01"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val afterArgumentCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(Either.right(mockedVersionedType))
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findAfter(any(), columnNameCaptor.capture(), afterArgumentCaptor.capture())
      "orderDate".should.equal(columnNameCaptor.firstValue)
      "2020-12-01".should.equal(afterArgumentCaptor.firstValue)
   }

   @Test
   fun `handler can map a before request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao)
      val request = mock<ServerRequest>() {
         on { path() } doReturn "/api/cask/OrderWindowSummary/orderDate/Before/2020-12-01"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val beforeArgumentCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(Either.right(mockedVersionedType))
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBefore(any(), columnNameCaptor.capture(), beforeArgumentCaptor.capture())
      "orderDate".should.equal(columnNameCaptor.firstValue)
      "2020-12-01".should.equal(beforeArgumentCaptor.firstValue)
   }

   @Test
   fun `handler can map findOne Request`() {
      // Given  findOneBy/OrderWindowSummary/symbol/BTCUSD
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao)
      val request = mock<ServerRequest>() {
         on { path() } doReturn "/api/cask/findOneBy/OrderWindowSummary/symbol/BTCUSD"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val projectionValueCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(Either.right(mockedVersionedType))
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findOne(any(), columnNameCaptor.capture(), projectionValueCaptor.capture())
      "symbol".should.equal(columnNameCaptor.firstValue)
      "BTCUSD".should.equal(projectionValueCaptor.firstValue)
   }

   @Test
   fun `handler can map findMany Request`() {
      // Given  findMultipleBy/OrderWindowSummary/symbol
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao)
      val request = mock<ServerRequest>() {
         on { path() } doReturn "/api/cask/findMultipleBy/OrderWindowSummary/symbol"
         on { method() } doReturn HttpMethod.POST
         on {body(any<BodyExtractor<Mono<List<String>>, ReactiveHttpInputMessage>>())} doReturn Mono.just(listOf("id"))
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val projectionValueCaptor = argumentCaptor<List<String>>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(Either.right(mockedVersionedType))
      // When
      caskApiHandler.findBy(request).subscribe {
         // Then
         verify(mockCaskDao, times(1)).findMultiple(any(), columnNameCaptor.capture(), projectionValueCaptor.capture())
         "symbol".should.equal(columnNameCaptor.firstValue)
         listOf("id").should.equal(projectionValueCaptor.firstValue)
      }
   }
}
