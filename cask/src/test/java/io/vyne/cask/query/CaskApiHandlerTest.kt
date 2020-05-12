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
import org.springframework.web.reactive.function.server.ServerRequest

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
      val projectionValueCaptor = argumentCaptor<Any>()
      whenever(mockCaskService.resolveType(eq("OrderWindowSummary"))).thenReturn(Either.right(mockedVersionedType))
      // When
      caskApiHandler.findBy(request)
      // Then
      verify(mockCaskDao, times(1)).findBy(any(), columnNameCaptor.capture(), projectionValueCaptor.capture())
      "symbol".should.equal(columnNameCaptor.firstValue)
      "BTCUSD".should.equal(projectionValueCaptor.firstValue.toString())
   }
}
