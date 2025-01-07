package com.orbitalhq.utils.files

import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import mu.KotlinLogging
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {  }
class SuspendableEventPublisher<T>(private val sink: Sinks.Many<T>,
                                   private val publishOnlyOneWhenResumed: Boolean = true,
                                   comparator: Comparator<T>) {
    private val suspended: AtomicBoolean = AtomicBoolean(false)
    private val suspendedEvents = ConcurrentSkipListSet(comparator)

    fun suspend() {
        suspended.compareAndExchange(false, true)
    }

    fun resume() {
        if (suspended.compareAndExchange(true, false)) {
            logger.debug { "resuming the suspendable event injection with stored even count => ${suspendedEvents.size}" }
            if (suspendedEvents.isNotEmpty() && publishOnlyOneWhenResumed) {
                logger.debug {"injecting only one event after resumed"}
                sink.emitNext(suspendedEvents.first(), RetryFailOnSerializeEmitHandler)
            } else {
                suspendedEvents.forEach {
                    sink.emitNext(it, RetryFailOnSerializeEmitHandler)
                }
            }
            suspendedEvents.clear()
        }
    }

    fun publish(item: T) {
        if (suspended.get()) {
            suspendedEvents.add(item)
        } else {
            sink.emitNext(item, RetryFailOnSerializeEmitHandler)
        }
    }

    companion object {
       fun forFileSystemChangeEvents(sink: Sinks.Many<List<FileSystemChangeEvent>>): SuspendableEventPublisher<List<FileSystemChangeEvent>> {
           return SuspendableEventPublisher(sink) { o1, o2 ->
               when {
                   o1 == null && o2 == null -> 0
                   o1 != null && o2 == null -> 1
                   o1 == null && o2 != null -> -1
                   else -> o1.first().path.compareTo(o2.first().path)
               }
           }
       }

    }

}