package io.vyne.schemaStore.eureka

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.httpSchemaConsumer.HttpVersionedSchemaProvider
import io.vyne.schemaApi.ControlSchemaPollEvent
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class EurekaHttpSchemaStoreTest {
   private val applicationEventPublisher: ApplicationEventPublisher = mock()
   private val httpVersionedSchemaProvider: HttpVersionedSchemaProvider = mock()
   private lateinit var store: EurekaHttpSchemaStore
   private lateinit var latch: CountDownLatch

   @Before
   fun setUp() {
      whenever(applicationEventPublisher.publishEvent(any<ControlSchemaPollEvent>()))
         .then { invocationOnMock -> store.controlPoll(invocationOnMock.getArgument(0, ControlSchemaPollEvent::class.java))  }

      whenever(httpVersionedSchemaProvider.getVersionedSchemas()).thenAnswer {
         latch.countDown()
         Mono.just(listOf<List<VersionedSource>>())
      }
      store = EurekaHttpSchemaStore(httpVersionedSchemaProvider, applicationEventPublisher, Duration.ofMillis(500))
   }

   @After
   fun tearDown() {
      store.stopPolling()
   }

   @Test
   fun `schema polling can be enabled by default`() {
      latch = CountDownLatch(1)
      store.startPolling()
      latch.await(1, TimeUnit.SECONDS).should.be.`true`
   }

   @Test
   fun `schema polling can be disabled through publication of ControlSchemaPollEvent`() {
      latch = CountDownLatch(1)
      applicationEventPublisher.publishEvent(ControlSchemaPollEvent(false))
      store.startPolling()
      latch.await(1, TimeUnit.SECONDS).should.be.`false`
      applicationEventPublisher.publishEvent(ControlSchemaPollEvent(true))
      latch.await(1, TimeUnit.SECONDS).should.be.`true`
   }
}
