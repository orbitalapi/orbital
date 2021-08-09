package io.vyne.queryService

import io.vyne.VyneCacheConfiguration
import io.vyne.spring.SimpleTaxiSchemaProvider
import org.apache.commons.io.IOUtils
import org.junit.Test
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor


class TaxiGraphServiceTest {

   val taxi = """
       type Author {
         firstName : FirstName as String
         lastName : LastName as String
       }
       type Book {
         author : Author
      }
      service AuthorService {
         operation lookupByName(authorName:FirstName):Author
      }

   """.trimIndent()
   @Test
   fun when_producingTaxiGraphSchema_that_verticesAreFiltered() {
      val fullSchema = IOUtils.toString(this::class.java.getResourceAsStream("/schema.taxi"))
      val service = TaxiGraphService(SimpleTaxiSchemaProvider(fullSchema), VyneCacheConfiguration.default())
      service.getLinksFromType("io.vyne.ClientJurisdiction")
   }

   var executor: ThreadPoolExecutor = Executors.newFixedThreadPool(10) as ThreadPoolExecutor
   val parallelScheduler = Schedulers.newParallel("dynamicHazelcastScheduler", 2)
   val hzExecutor = Schedulers.newParallel("dynamicHazelcastScheduler", 10)

   @Test
   fun fluxTest() {
      Flux
         .range(1,10000)
         .parallel(2)
         .runOn( parallelScheduler )
         .map{
            println("Compressing $it")
            it
         } //
         .map { longRunningHardWork(it) }
         .runOn(hzExecutor)
         .map { it.get() }
         .subscribe {
            //println("Received ${it}")
         }
      Thread.sleep(1000000)
   }

   fun pause(int:Int):Int {
      Thread.sleep(1000)
      return int
   }

   fun longRunningHardWork(int:Int):Future<String> {
      return executor.submit<String> {
            //println("Starting work on HZ node ${Thread.currentThread().name} for input ${int}")
            Thread.sleep(1000)
            val ret = "Processed ${int}"
            ret
      }
   }

}
