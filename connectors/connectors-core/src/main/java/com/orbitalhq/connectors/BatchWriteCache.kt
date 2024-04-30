package com.orbitalhq.connectors

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap


private val logger = KotlinLogging.logger {  }

class BatchWriteCacheProvider {
    private val batchDataCache = ConcurrentHashMap<String, BatchWriteCache>()

    fun withBatchSize(queryId: String,
                      batchSize: Int,
                      batchTimeoutInMillis: Long,
                      bulkCallBack:  (List<Pair<TypedInstance, FluxSink<TypedInstance>>>) -> Mono<Any>
    ): BatchWriteCache {
        return batchDataCache.getOrPut(queryId) {
            val sink = Sinks.many().multicast().onBackpressureBuffer<Pair<TypedInstance, FluxSink<TypedInstance>>>()
            sink
                .asFlux()
                .bufferTimeout(batchSize, Duration.ofMillis(batchTimeoutInMillis))
                .doFinally {
                    logger.info { "Removing sink for $queryId" }
                batchDataCache.remove(queryId)
            }.subscribeOn(Schedulers.boundedElastic())
                .flatMap {
                    bulkCallBack(it)
                }
                .subscribe()

            BatchWriteCache(sink)
        }
    }

    fun hasBatchCache(queryId: String) = batchDataCache.contains(queryId)
}

data class BatchWriteCache(private val sink: Sinks.Many<Pair<TypedInstance, FluxSink<TypedInstance>>>) {
    fun emit(item: TypedInstance): Flux<TypedInstance> {
        return Flux.create { fluxSink ->
            sink.emitNext(Pair(item, fluxSink), RetryFailOnSerializeEmitHandler)
        }
    }
}

@OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.bufferTimeout(size: Int, duration: Duration): Flow<List<T>> {
    require(size > 0) { "Window size should be greater than 0" }
    require(duration.toMillis() > 0) { "Duration should be greater than 0" }

    return flow {
        coroutineScope {
            val events = ArrayList<T>(size)
            val tickerChannel = ticker(duration.toMillis())
            try {
                val upstreamValues = produce { collect { send(it) } }

                while (isActive) {
                    var hasTimedOut = false

                    select<Unit> {
                        upstreamValues.onReceive {
                            events.add(it)
                        }

                        tickerChannel.onReceive {
                            hasTimedOut = true
                        }
                    }

                    if (events.size == size || (hasTimedOut && events.isNotEmpty())) {
                        emit(events.toList())
                        events.clear()
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // drain remaining events
                if (events.isNotEmpty()) emit(events.toList())
            } finally {
                tickerChannel.cancel()
            }
        }
    }
}