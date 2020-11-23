package io.vyne.spring.invokers

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import io.vyne.schemas.Metadata
import io.vyne.schemas.Operation
import io.vyne.schemas.Service
import io.vyne.schemas.fqn
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class ServiceUrlResolverTest {
   @Mock
   lateinit var discoveryClient: ServiceDiscoveryClient
   @Mock
   lateinit var  noopOperation: Operation

   private val serviceDiscoveryClientName = "bar-service"
   private val service = Service("fooService".fqn(),
      operations = listOf(),
      queryOperations = listOf(),
      metadata = listOf(Metadata("ServiceDiscoveryClient".fqn(), mapOf("serviceName" to serviceDiscoveryClientName))),
      sourceCode = listOf())

   @Test
   fun `ServiceDiscoveryClient returns url that does not end with forward slash and provided url starts with forward slash`() {
      // given
      val discoveryClientUrl = "http://192.168.225.31:8800"
      val serviceUrl = "/api/foo/bar/{ClientId}"
      whenever(discoveryClient.resolve(eq(serviceDiscoveryClientName))).thenReturn(discoveryClientUrl)
      val serviceDiscoveryClientUrlResolver = ServiceDiscoveryClientUrlResolver(discoveryClient)
      // when
      val absoluteUrl = serviceDiscoveryClientUrlResolver.makeAbsolute(serviceUrl, service, noopOperation)
      // then
      assertEquals("http://192.168.225.31:8800/api/foo/bar/{ClientId}", absoluteUrl)
   }

   @Test
   fun `ServiceDiscoveryClient returns url that ends with forward slash and provided url starts with forward slash`() {
      // given
      val discoveryClientUrl = "http://192.168.225.31:8800/"
      val serviceUrl = "/api/foo/bar/{ClientId}"
      whenever(discoveryClient.resolve(eq(serviceDiscoveryClientName))).thenReturn(discoveryClientUrl)
      val serviceDiscoveryClientUrlResolver = ServiceDiscoveryClientUrlResolver(discoveryClient)
      // when
      val absoluteUrl = serviceDiscoveryClientUrlResolver.makeAbsolute(serviceUrl, service, noopOperation)
      // then
      assertEquals("http://192.168.225.31:8800/api/foo/bar/{ClientId}", absoluteUrl)
   }

   @Test
   fun `ServiceDiscoveryClient returns url that ends with forward slash and provided url does not start with forward slash`() {
      // given
      val discoveryClientUrl = "http://192.168.225.31:8800/"
      val serviceUrl = "api/foo/bar/{ClientId}"
      whenever(discoveryClient.resolve(eq(serviceDiscoveryClientName))).thenReturn(discoveryClientUrl)
      val serviceDiscoveryClientUrlResolver = ServiceDiscoveryClientUrlResolver(discoveryClient)
      // when
      val absoluteUrl = serviceDiscoveryClientUrlResolver.makeAbsolute(serviceUrl, service, noopOperation)
      // then
      assertEquals("http://192.168.225.31:8800/api/foo/bar/{ClientId}", absoluteUrl)
   }


}
