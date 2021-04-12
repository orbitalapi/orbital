package io.vyne.query

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import lang.taxi.types.PrimitiveType
import org.junit.Test

class CacheAwareOperationInvocationDecoratorTest {

   @Test
   fun testKeyGenerator() = runBlocking {
      val mockOperationInvoker = mock<OperationInvoker>()
      val mockProfilerOperation = mock<ProfilerOperation>()
      val cacheAware = CacheAwareOperationInvocationDecorator(mockOperationInvoker)

      val type = Type(name = QualifiedName("type1"), sources = listOf(), taxiType = PrimitiveType.STRING, typeDoc = null)
      val mockedTypeInstance = mock<TypedInstance>()
      val service = Service(QualifiedName("srv1"), listOf(), listOf(), listOf(), listOf())
      val operation = Operation(
         qualifiedName = QualifiedName("op1@@op1"),
         returnType = type,
         parameters = listOf(),
         sources =  listOf())
      val params = listOf(
         element = Pair(
            first = Parameter(type),
            second = TypedInstance.from(type, null, mock(), source = Provided)
         )
      )
      whenever(mockOperationInvoker.invoke(any(), any(), any(), any(), any())).thenReturn( flow { emit(mockedTypeInstance) })
      cacheAware.invoke(service, operation, params, mockProfilerOperation, "MOCK_QUERY_ID")
      cacheAware.invoke(service, operation, params, mockProfilerOperation,"MOCK_QUERY_ID")
      verify(mockOperationInvoker, times(1)).invoke(service, operation, params, mockProfilerOperation,"MOCK_QUERY_ID")
   }
}
