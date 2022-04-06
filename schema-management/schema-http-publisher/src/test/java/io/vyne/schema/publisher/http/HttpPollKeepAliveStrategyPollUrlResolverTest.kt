package io.vyne.schema.publisher.http

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import org.junit.Before
import org.junit.Test
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.web.util.UriComponentsBuilder
import java.util.Optional

class HttpPollKeepAliveStrategyPollUrlResolverTest {
   private val mockDiscoveryClient = mock<DiscoveryClient>()
   private val mockServiceInstance = mock<ServiceInstance>()
   private val classUnderTest = HttpPollKeepAliveStrategyPollUrlResolver(Optional.of(mockDiscoveryClient))

   @Before
   fun setUpMocks() {
      whenever(mockServiceInstance.uri).thenReturn(UriComponentsBuilder.fromUriString("http://localhost:8800").build().toUri())
      whenever(mockDiscoveryClient.getInstances("cask")).thenReturn(listOf(
         mockServiceInstance
      ))
   }
   @Test
   fun `resolve discovery client based http keep alive end points`() {
      classUnderTest
         .absoluteUrl("cask/api/actuator/info")
         .should
         .equal("http://localhost:8800/api/actuator/info")
   }

   @Test
   fun `resolve absolute http keep alive end points`() {
      classUnderTest
         .absoluteUrl("http://localhost:8800/api/actuator/info")
         .should
         .equal("http://localhost:8800/api/actuator/info")
   }
}
