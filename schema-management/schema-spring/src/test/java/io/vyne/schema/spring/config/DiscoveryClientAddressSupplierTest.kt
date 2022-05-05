package io.vyne.schema.spring.config

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.schema.rsocket.TcpAddress
import org.junit.Test
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import reactor.kotlin.test.test
import reactor.test.StepVerifier
import reactor.test.scheduler.VirtualTimeScheduler
import reactor.util.retry.Retry
import java.time.Duration

class DiscoveryClientAddressSupplierTest {

   private val mockServiceInstance: ServiceInstance = mock {
      on { host } doReturn "mock-host"
   }

   @Test
   fun `when discoveryClient has instance then address is provided`() {
      val discoveryClient = mock<DiscoveryClient> {
         on { getInstances(any()) } doReturn listOf(mockServiceInstance)
      }
      val addressSupplier = addressSupplier(discoveryClient)
      val next = addressSupplier.nextAddress().block()!!
      next.host.should.equal("mock-host")
   }

   @Test
   fun `when discovery client does not have instance then it keeps retrying`() {
      val mutableList = mutableListOf<ServiceInstance>()
      val discoveryClient = mock<DiscoveryClient> {
         on { getInstances(any()) } doReturn mutableList
      }
      val addressSupplier = addressSupplier(
         discoveryClient,
         Retry.fixedDelay(500, Duration.ofMillis(250))
      )
      addressSupplier
         .nextAddress()
         .test()
         .expectSubscription()
         .expectNoEvent(Duration.ofSeconds(2))
         .then {
            mutableList.add(mockServiceInstance)
         }.expectNextMatches { address ->
            address.host == "mock-host"
         }.verifyComplete()
   }


   private fun addressSupplier(
      discoveryClient: DiscoveryClient, retry: Retry = Retry.fixedDelay(
         Long.MAX_VALUE, Duration.ofSeconds(3)
      )
   ): DiscoveryClientAddressSupplier<TcpAddress> {
      return DiscoveryClientAddressSupplier(
         discoveryClient, "my-service", DiscoveryClientAddressSupplier.TcpAddressConverter(2000), retry = retry
      )
   }


}

