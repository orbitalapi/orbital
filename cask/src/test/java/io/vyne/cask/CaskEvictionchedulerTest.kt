package io.vyne.cask

import com.nhaarman.mockito_kotlin.*
import io.vyne.cask.api.CaskConfig
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant


class CaskEvictionchedulerTest {
   lateinit var caskEvictionScheduler: CaskEvictionScheduler
   lateinit var caskService: CaskService
   lateinit var clock: Clock

   @Before
   fun setup() {
      clock = mock()
      caskService = mock()
      caskEvictionScheduler = CaskEvictionScheduler(caskService, clock)
   }

   @Test
   fun `Should not evict if no casks`() {

      // act
      caskEvictionScheduler.evict()

      // assert
      verify(caskService, never()).evict(any(), any())
   }

   @Test
   fun `Should evict when casks`() {
      whenever(clock.instant()).thenReturn(Instant.parse("2020-08-07T22:11:00Z"))
      whenever(caskService.getCasks()).thenReturn(listOf(
         CaskConfig("table1", "com.type1", "#E1A90", emptyList(), emptyList(), null, 10, Instant.now()),
         CaskConfig("table2", "com.type2", "#E1A90", emptyList(), emptyList(), null, 20, Instant.now()),
         CaskConfig("table3", "com.type3", "#E1A90", emptyList(), emptyList(), null, 30, Instant.now())
      ))

      // act
      caskEvictionScheduler.evict()

      // assert
      verify(caskService, times(3)).evict(any(), any())
      verify(caskService, times(1)).evict(eq("table1"), eq(Instant.parse("2020-07-28T22:11:00Z")))
      verify(caskService, times(1)).evict(eq("table2"), eq(Instant.parse("2020-07-18T22:11:00Z")))
      verify(caskService, times(1)).evict(eq("table3"), eq(Instant.parse("2020-07-08T22:11:00Z")))
   }

}
