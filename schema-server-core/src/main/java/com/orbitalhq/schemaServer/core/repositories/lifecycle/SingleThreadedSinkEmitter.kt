package com.orbitalhq.schemaServer.core.repositories.lifecycle

import mu.KotlinLogging
import reactor.core.publisher.Sinks
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * As a workaround to reactor.core.publisher.Sinks$EmissionException in usecases where
 * concurrent emission is valid (and BusyLooping hasn't worked), this emitter has your back.
 *
 * Queues all events, then emits them sequentially from a dedicated thread.
 *
 * See ProjectStoreLifecycleManagerTest for tests
 */
object SingleThreadedSinkEmitter {
   private val logger = KotlinLogging.logger {}
   private val backingQueue = LinkedBlockingQueue<Triple<Sinks.Many<Any>, Any, CompletableFuture<Sinks.EmitResult>>>()

   init {
      thread(name = "SingleThreadedSinkEmitter") {
         while (true) {
            val (sink, payload, future) = backingQueue.take()
            val emitResult = sink.tryEmitNext(payload)
            if (emitResult != Sinks.EmitResult.OK) {
               logger.error { "Emitting event of ${payload::class.simpleName} failed: $emitResult" }
            }
            future.complete(emitResult)
         }

      }
   }

   fun <T : Any> queue(sink: Sinks.Many<T>, payload: T): CompletableFuture<Sinks.EmitResult> {
      val future = CompletableFuture<Sinks.EmitResult>()
      backingQueue.add(Triple(sink as Sinks.Many<Any>, payload as Any, future))
      return future
   }
}

// Emits events on a single thread, removing any parallelism on emission.
// Use this when errors occur around
// reactor.core.publisher.Sinks$EmissionException: Spec. Rule 1.3 - onSubscribe, onNext, onError and onComplete signaled to a Subscriber MUST be signaled serially.
// Generally, you should try to use busy looping instead before adopting this approach.
// (See ProjectStoreLifecycleManagerTest).
fun <T : Any> Sinks.Many<T>.emitOnSingleThread(payload: T): CompletableFuture<Sinks.EmitResult> {
   return SingleThreadedSinkEmitter.queue(this, payload)
}
