package com.orbitalhq.licensing

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class QuotaHealthTest {
   @Test
   fun `calculates quota health correctly`() {
      QuotaHealth.calculateFor(consumedUsage = 0, quota = 0)
         .shouldBe(QuotaHealth.WARNING)
      QuotaHealth.calculateFor(consumedUsage = 10, quota = 0)
         .shouldBe(QuotaHealth.EXCEEDED)
      QuotaHealth.calculateFor(consumedUsage = 10, quota = 100)
         .shouldBe(QuotaHealth.HEALTHY)

      QuotaHealth.calculateFor(consumedUsage = 74, quota = 100)
         .shouldBe(QuotaHealth.HEALTHY)
      QuotaHealth.calculateFor(consumedUsage = 75, quota = 100)
         .shouldBe(QuotaHealth.WARNING)
      QuotaHealth.calculateFor(consumedUsage = 100, quota = 100)
         .shouldBe(QuotaHealth.WARNING)
      QuotaHealth.calculateFor(consumedUsage = 101, quota = 100)
         .shouldBe(QuotaHealth.EXCEEDED)
   }
}
