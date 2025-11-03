package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.mock
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class ProjectStoreLifecycleManagerTest {
   // Relates to ORB-494
   @Test
   fun `can call sink concurrently without exception`() {
      val manager = ProjectStoreLifecycleManager()
      val event: FileSpecAddedEvent = mock { }
      val eventCountA = AtomicInteger(0)
      val eventCountB = AtomicInteger(0)

      // Register multiple consumers
      manager.fileSpecAdded.subscribe {
         eventCountA.incrementAndGet()
      }
      manager.fileSpecAdded.subscribe {
         eventCountB.incrementAndGet()
      }

      val threadCount = 10
      val repetitions = 50
      // Get a bunch of worker threads
      val workerThreads = (0..threadCount).map {
         Thread {
            repeat(repetitions) {
               manager.fileRepositorySpecAdded(event)
               sleep(Random.nextInt(25,65).toLong())
            }
         }
      }
      workerThreads.forEach { it.start() }
      workerThreads.forEach { it.join() }
      val expected = (threadCount + 1) * repetitions
      Awaitility.await().atMost(Duration.TEN_SECONDS).until<Boolean> {
         eventCountA.get() == expected && eventCountB.get() == expected
      }
      eventCountA.get().shouldBe(expected)
   }
}
