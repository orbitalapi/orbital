package io.vyne.query

import com.nhaarman.mockito_kotlin.*
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemas.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.types.PrimitiveType
import org.junit.Test

class CacheAwareOperationInvocationDecoratorTest {

   @Test
   fun testKeyGenerator() {
      val mockOperationInvoker = mock<OperationInvoker>()
      val mockQueryContext = mock<QueryContext>()
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
      runBlocking {
         whenever(mockOperationInvoker.invoke(any(), any(), any(), any(), any())).thenReturn(flow {

            emit(
               mockedTypeInstance
            )
         })
         cacheAware.invoke(service, operation, params, mockQueryContext, "MOCK_QUERY_ID").toList()
         cacheAware.invoke(service, operation, params, mockQueryContext, "MOCK_QUERY_ID").toList()
         verify(mockOperationInvoker, times(1)).invoke(
            service,
            operation,
            params,
            mockQueryContext,
            "MOCK_QUERY_ID"
         )
      }
   }
}
