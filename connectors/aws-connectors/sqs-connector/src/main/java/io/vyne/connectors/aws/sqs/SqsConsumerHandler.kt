package io.vyne.connectors.aws.sqs

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

class SqsConsumerHandler(consumer: SqsConnection) {
   private val sink: Sinks.Many<ReceiveMessageResponse> = Sinks.many().unicast().onBackpressureBuffer()
   private val eventScheduler: Scheduler = Schedulers.newSingle(SqsEventThreadFactory())
   private val consumerEventLoop = SqsConsumerEventLoop(sink, eventScheduler, consumer)
   init {
       eventScheduler.start()
   }

   fun  receive(): Flux<ReceiveMessageResponse> {
      return sink.asFlux().doOnRequest(consumerEventLoop::onRequest)
   }

   fun close(): Mono<Void> {
      return consumerEventLoop.stop()!!.doFinally { eventScheduler.dispose() }
   }
}

class SqsEventThreadFactory: ThreadFactory {
   companion object {
      val threadCounter = AtomicLong()
   }
   override fun newThread(r: Runnable): Thread {
      return SqsEmitterThread(r, "sqs-emitter-${threadCounter.incrementAndGet()}")
   }

}

class SqsEmitterThread(target: Runnable, name: String): Thread(target, name)
