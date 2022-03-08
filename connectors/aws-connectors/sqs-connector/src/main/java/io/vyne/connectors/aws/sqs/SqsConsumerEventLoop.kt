package io.vyne.connectors.aws.sqs

import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.core.publisher.Operators
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitResult
import reactor.core.scheduler.Scheduler
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLongFieldUpdater

private val logger = KotlinLogging.logger {  }
class SqsConsumerEventLoop(
   private val sink: Sinks.Many<ReceiveMessageResponse>,
   private val eventScheduler: Scheduler,
   private val consumer: SqsConnection): Sinks.EmitFailureHandler {
   val isActive = AtomicBoolean(true)
   private val pollEvent = SqsPollEvent()
   @Volatile
   @JvmField
   var requested: Long = 0

   companion object {
      private val Requested = AtomicLongFieldUpdater.newUpdater(SqsConsumerEventLoop::class.java, "requested")
   }
   override fun onEmitFailure(p0: SignalType, result: EmitResult): Boolean {
      return if (!isActive.get()) {
         false
      } else {
         result == EmitResult.FAIL_NON_SERIALIZED
      }
   }

   fun onRequest(toAdd: Long) {
      Operators.addCap(Requested, this, toAdd)
      pollEvent.schedule()
   }

   fun stop(): Mono<Void>? {
      return Mono
         .defer {
            logger.info { "dispose $isActive" }
            if (!isActive.compareAndSet(true, false)) {
               return@defer Mono.empty<Void>()
            }

            return@defer Mono.empty<Void>()

         }
         .onErrorResume { e: Throwable ->
            logger.error(e) { "Cancel Exception" }
            Mono.empty()
         }
   }

   inner class SqsPollEvent: Runnable {
      private val scheduled = AtomicBoolean()

      override fun run() {
         try {
            this.scheduled.set(false)
            if (isActive.get()) {
               val records = consumer.poll()
               if (isActive.get()) {
                  schedule()
               }
               if (records.hasMessages()) {
                  val receiptHandles = records.messages().map { it.receiptHandle() }
                  Operators.produced(Requested, this@SqsConsumerEventLoop, 1)
                  sink.emitNext(records, this@SqsConsumerEventLoop)
                  consumer.deleteProcessedMessages(receiptHandles)
               }
            }
         } catch (e: Exception) {
            logger.error(e) { "Unexpected Sns Exception"  }
         }
      }

      fun schedule() {
         if (!scheduled.getAndSet(true)) {
            eventScheduler.schedule(this)
         }
      }

   }
}


