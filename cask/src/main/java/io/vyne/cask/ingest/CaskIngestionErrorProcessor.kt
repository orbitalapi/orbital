package io.vyne.cask.ingest

import io.vyne.utils.log
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import reactor.core.publisher.UnicastProcessor
import reactor.core.scheduler.Schedulers.boundedElastic
import reactor.core.scheduler.Schedulers.elastic

@Service
class CaskIngestionErrorProcessor(private val repository: IngestionErrorRepository): InitializingBean {
   private val ingestionErrorStream: UnicastProcessor<IngestionError> = UnicastProcessor.create()
   private val ingestionErrorSink = ingestionErrorStream.sink()
   override fun afterPropertiesSet() {
      ingestionErrorStream
         .parallel()
         .runOn(boundedElastic())
         .subscribe { ingestionError ->
         try {
            repository.save(ingestionError)
         } catch (e: Exception) {
            log().error("error in persisting ingestion error", e)
         }
      }
   }

   fun sink() = this.ingestionErrorSink
}
