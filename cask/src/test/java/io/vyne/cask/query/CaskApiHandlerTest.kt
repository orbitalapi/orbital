package io.vyne.cask.query

import arrow.core.right
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
import io.vyne.cask.config.CaskQueryDispatcherConfiguration
import io.vyne.cask.query.generators.BetweenVariant
import io.vyne.cask.services.QueryMonitor
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
   private val mockCaskRecordCountDAO = mock<CaskRecordCountDAO>()
   @Test
   fun `handler can map request uri to relevant type`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/OrderWindowSummary/symbol/BTCUSD"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val projectionValueCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
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
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/OrderWindowSummary/orderDate/Between/2020-12-01/2020-12-20"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val startArgumentCaptor = argumentCaptor<String>()
      val endArgumentCaptor = argumentCaptor<String>()
      val variantArgumentCaptor = argumentCaptor<BetweenVariant>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBetween(any(), columnNameCaptor.capture(), startArgumentCaptor.capture(), endArgumentCaptor.capture(), variantArgumentCaptor.capture())
      "orderDate".should.equal(columnNameCaptor.firstValue)
      "2020-12-01".should.equal(startArgumentCaptor.firstValue)
      "2020-12-20".should.equal(endArgumentCaptor.firstValue)
      variantArgumentCaptor.firstValue.should.be.`null`
   }

   @Test
   fun `handler can map a after request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/OrderWindowSummary/orderDate/After/2020-12-01"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val afterArgumentCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
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
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/OrderWindowSummary/orderDate/Before/2020-12-01"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val beforeArgumentCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
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
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/findOneBy/OrderWindowSummary/symbol/BTCUSD"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val projectionValueCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
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
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/findMultipleBy/OrderWindowSummary/symbol"
         on { method() } doReturn HttpMethod.POST
         on {body(any<BodyExtractor<Mono<List<String>>, ReactiveHttpInputMessage>>())} doReturn Mono.just(listOf("id"))
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val projectionValueCaptor = argumentCaptor<List<String>>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request).subscribe {
         // Then
         verify(mockCaskDao, times(1)).findMultiple(any(), columnNameCaptor.capture(), projectionValueCaptor.capture())
         "symbol".should.equal(columnNameCaptor.firstValue)
         listOf("id").should.equal(projectionValueCaptor.firstValue)
      }
   }

   @Test
   fun `handler can map findSingleBy Request`() {
      // Given  findSingleBy/OrderWindowSummary/symbol/BTCUSD
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/findSingleBy/OrderWindowSummary/symbol/BTCUSD"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val projectionValueCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findOne(any(), columnNameCaptor.capture(), projectionValueCaptor.capture())
      "symbol".should.equal(columnNameCaptor.firstValue)
      "BTCUSD".should.equal(projectionValueCaptor.firstValue)
   }

   @Test
   fun `handler can map finadAll Request`() {
      // Given  findSingleBy/OrderWindowSummary/symbol/BTCUSD
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/findAll/OrderWindowSummary"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val projectionValueCaptor = argumentCaptor<String>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findAll(any<VersionedType>())
   }

   @Test
   fun `handler can map a between CaskInsertedAt request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/foo/orders/Order/CaskInsertedAt/Between/2010-03-27T11:01:09Z/2030-03-27T11:01:11Z"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val startArgumentCaptor = argumentCaptor<String>()
      val endArgumentCaptor = argumentCaptor<String>()
      val variantArgumentCaptor = argumentCaptor<BetweenVariant>()
      whenever(mockCaskService.resolveType(eq("foo.orders.Order"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBetween(any(), columnNameCaptor.capture(), startArgumentCaptor.capture(), endArgumentCaptor.capture(), variantArgumentCaptor.capture())
      "CaskInsertedAt".should.equal(columnNameCaptor.firstValue)
      "2010-03-27T11:01:09Z".should.equal(startArgumentCaptor.firstValue)
      "2030-03-27T11:01:11Z".should.equal(endArgumentCaptor.firstValue)
      variantArgumentCaptor.firstValue.should.be.`null`
   }

   @Test
   fun `handler can map temporal greater than start and less than end request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/OrderWindowSummary/orderDate/BetweenGtLt/2020-12-01/2020-12-20"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val startArgumentCaptor = argumentCaptor<String>()
      val endArgumentCaptor = argumentCaptor<String>()
      val variantArgumentCaptor = argumentCaptor<BetweenVariant>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBetween(
         any(),
         columnNameCaptor.capture(),
         startArgumentCaptor.capture(),
         endArgumentCaptor.capture(),
         variantArgumentCaptor.capture())
      "orderDate".should.equal(columnNameCaptor.firstValue)
      "2020-12-01".should.equal(startArgumentCaptor.firstValue)
      "2020-12-20".should.equal(endArgumentCaptor.firstValue)
      BetweenVariant.GtLt.should.equal(variantArgumentCaptor.firstValue)
   }

   @Test
   fun `handler can map temporal greater than  start and less than equals end request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/OrderWindowSummary/orderDate/BetweenGtLte/2020-12-01/2020-12-20"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val startArgumentCaptor = argumentCaptor<String>()
      val endArgumentCaptor = argumentCaptor<String>()
      val variantArgumentCaptor = argumentCaptor<BetweenVariant>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBetween(
         any(),
         columnNameCaptor.capture(),
         startArgumentCaptor.capture(),
         endArgumentCaptor.capture(),
         variantArgumentCaptor.capture())
      "orderDate".should.equal(columnNameCaptor.firstValue)
      "2020-12-01".should.equal(startArgumentCaptor.firstValue)
      "2020-12-20".should.equal(endArgumentCaptor.firstValue)
      BetweenVariant.GtLte.should.equal(variantArgumentCaptor.firstValue)
   }

   @Test
   fun `handler can map temporal greater than equals start and less than equals end request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/OrderWindowSummary/orderDate/BetweenGteLte/2020-12-01/2020-12-20"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val startArgumentCaptor = argumentCaptor<String>()
      val endArgumentCaptor = argumentCaptor<String>()
      val variantArgumentCaptor = argumentCaptor<BetweenVariant>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBetween(
         any(),
         columnNameCaptor.capture(),
         startArgumentCaptor.capture(),
         endArgumentCaptor.capture(),
         variantArgumentCaptor.capture())
      "orderDate".should.equal(columnNameCaptor.firstValue)
      "2020-12-01".should.equal(startArgumentCaptor.firstValue)
      "2020-12-20".should.equal(endArgumentCaptor.firstValue)
      BetweenVariant.GteLte.should.equal(variantArgumentCaptor.firstValue)
   }

   @Test
   fun `handler can map a greater than start and less than end CaskInsertedAt request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/foo/orders/Order/CaskInsertedAt/BetweenGtLt/2010-03-27T11:01:09Z/2030-03-27T11:01:11Z"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val startArgumentCaptor = argumentCaptor<String>()
      val endArgumentCaptor = argumentCaptor<String>()
      val variantArgumentCaptor = argumentCaptor<BetweenVariant>()
      whenever(mockCaskService.resolveType(eq("foo.orders.Order"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBetween(
         any(),
         columnNameCaptor.capture(),
         startArgumentCaptor.capture(),
         endArgumentCaptor.capture(),
         variantArgumentCaptor.capture())
      "CaskInsertedAt".should.equal(columnNameCaptor.firstValue)
      "2010-03-27T11:01:09Z".should.equal(startArgumentCaptor.firstValue)
      "2030-03-27T11:01:11Z".should.equal(endArgumentCaptor.firstValue)
      variantArgumentCaptor.firstValue.should.equal(BetweenVariant.GtLt)
   }

   @Test
   fun `handler can map a greater than start and less than equals end CaskInsertedAt request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/foo/orders/Order/CaskInsertedAt/BetweenGtLte/2010-03-27T11:01:09Z/2030-03-27T11:01:11Z"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val startArgumentCaptor = argumentCaptor<String>()
      val endArgumentCaptor = argumentCaptor<String>()
      val variantArgumentCaptor = argumentCaptor<BetweenVariant>()
      whenever(mockCaskService.resolveType(eq("foo.orders.Order"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBetween(
         any(),
         columnNameCaptor.capture(),
         startArgumentCaptor.capture(),
         endArgumentCaptor.capture(),
         variantArgumentCaptor.capture())
      "CaskInsertedAt".should.equal(columnNameCaptor.firstValue)
      "2010-03-27T11:01:09Z".should.equal(startArgumentCaptor.firstValue)
      "2030-03-27T11:01:11Z".should.equal(endArgumentCaptor.firstValue)
      variantArgumentCaptor.firstValue.should.equal(BetweenVariant.GtLte)
   }

   @Test
   fun `handler can map a greater than equals start and less than equals end CaskInsertedAt request`() {
      // Given
      val caskApiHandler = CaskApiHandler(mockCaskService, mockCaskDao, mockCaskRecordCountDAO, queryMonitor = QueryMonitor(null,null, CaskQueryDispatcherConfiguration()))
      val request = mock<ServerRequest> {
         on { path() } doReturn "/api/cask/foo/orders/Order/CaskInsertedAt/BetweenGteLte/2010-03-27T11:01:09Z/2030-03-27T11:01:11Z"
      }
      val mockedVersionedType = mock<VersionedType>()
      val columnNameCaptor = argumentCaptor<String>()
      val startArgumentCaptor = argumentCaptor<String>()
      val endArgumentCaptor = argumentCaptor<String>()
      val variantArgumentCaptor = argumentCaptor<BetweenVariant>()
      whenever(mockCaskService.resolveType(eq("foo.orders.Order"))).thenReturn(mockedVersionedType.right())
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBetween(
         any(),
         columnNameCaptor.capture(),
         startArgumentCaptor.capture(),
         endArgumentCaptor.capture(),
         variantArgumentCaptor.capture())
      "CaskInsertedAt".should.equal(columnNameCaptor.firstValue)
      "2010-03-27T11:01:09Z".should.equal(startArgumentCaptor.firstValue)
      "2030-03-27T11:01:11Z".should.equal(endArgumentCaptor.firstValue)
      variantArgumentCaptor.firstValue.should.equal(BetweenVariant.GteLte)
   }
}
