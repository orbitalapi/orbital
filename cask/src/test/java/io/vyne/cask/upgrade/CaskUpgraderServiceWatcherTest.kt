package io.vyne.cask.upgrade

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.ControlSchemaPollEvent
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CaskUpgraderServiceWatcherTest {
   private val caskConfigRepository: CaskConfigRepository = mock()
   private val caskDAO: CaskDAO = mock()
   private val upgraderService: CaskUpgraderService = mock()
   private val eventPublisher: ApplicationEventPublisher = mock()

   @Test
   fun `Fires resume schema polling event when upgrade operation is completed`() {
      // Given
      val upgradeServiceWatcher = CaskUpgraderServiceWatcher(caskConfigRepository,caskDAO, upgraderService, eventPublisher)
      val caskConfigV1 = CaskConfig("Order_hash1", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val caskConfigV2 = CaskConfig("Order_hash2", "Order", "hash1", emptyList(), emptyList(), null, Instant.now())
      val latch: CountDownLatch = CountDownLatch(1)
      whenever(caskConfigRepository.findAllByStatus(eq(CaskStatus.MIGRATING))).thenReturn(
         listOf(caskConfigV1, caskConfigV2))
      whenever(eventPublisher.publishEvent(any<ControlSchemaPollEvent>())).then {
         it.getArgument(0, ControlSchemaPollEvent::class.java).poll.should.be.`true`
         latch.countDown()
      }
      //When
      upgradeServiceWatcher.onUpgradeWorkDetected(CaskUpgradesRequiredEvent())
      //Then
      latch.await(1, TimeUnit.SECONDS).should.be.`true`
   }
}
