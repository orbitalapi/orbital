package io.vyne.query

import com.nhaarman.mockito_kotlin.*
import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemas.*
import lang.taxi.types.PrimitiveType
import org.junit.Test

class CacheAwareOperationInvocationDecoratorTest {

   @Test
   fun testKeyGenerator() {
      val mockOperationInvoker = mock<OperationInvoker>()
      val mockProfilerOperation = mock<ProfilerOperation>()
      val cacheAware = CacheAwareOperationInvocationDecorator(mockOperationInvoker)

      val type = Type(name = QualifiedName("type1"), sources = listOf(), taxiType = PrimitiveType.STRING, typeDoc = null)
      val mockedTypeInstance = mock<TypedInstance>()
      val service = Service(QualifiedName("srv1"), listOf(), listOf(), listOf())
      val operation = Operation(
         qualifiedName = QualifiedName("op1@@op1"),
         returnType = type,
         parameters = listOf(),
         sources =  listOf())
      val params = listOf(
         element = Pair(
            first = Parameter(type),
            second = TypedInstance.from(type, null, mock())
         )
      )
      whenever(mockOperationInvoker.invoke(any(), any(), any(), any())).thenReturn(mockedTypeInstance)
      cacheAware.invoke(service, operation, params, mockProfilerOperation)
      cacheAware.invoke(service, operation, params, mockProfilerOperation)
      verify(mockOperationInvoker, times(1)).invoke(service, operation, params, mockProfilerOperation)
   }
}
