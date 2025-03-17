package com.orbitalhq.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.time.Instant

class StreamErrorPublisher: AutoCloseable {
    private val streamErrorsSink = Sinks.many()
        .replay()
        // Limit is quite short - we just want
        // to allow any late UI subscribers to get recent events
        .limit<StreamQueryErrorEvent>(Duration.ofSeconds(30))

    val errors: Flux<StreamQueryErrorEvent>
        get() = streamErrorsSink.asFlux()

    fun onError(queryId: String, error: StreamErrorMessage) {
        streamErrorsSink.emitNext(
            StreamQueryErrorEvent(queryId, error),
            RetryFailOnSerializeEmitHandler
        )
    }

    override fun close() {
        streamErrorsSink.tryEmitComplete()
    }
}

class StreamErrorException(streamErrorMessage: StreamErrorMessage): IllegalStateException(streamErrorMessage.exception)
data class StreamErrorMessage(
    val timestamp: Instant,
    @JsonIgnore
    val exception: Exception,
    val message: String,
    val typeName: String,
    val payload: Any
) {
    companion object {
        fun fromException(ex: Exception, typeName: String): StreamErrorMessage {
            return StreamErrorMessage(Instant.now(),
                ex,
                ex.message ?: "",
                typeName,
                ex.message ?: "")
        }

        fun fromThrowable(throwable: Throwable, typeName: String): StreamErrorMessage {
            return StreamErrorMessage(Instant.now(),
                IllegalStateException(throwable),
                throwable.message ?: "",
                typeName,
                throwable.message ?: "")
        }

    }

    fun toException(): StreamErrorException = StreamErrorException(this)
}
data class StreamQueryErrorEvent(val queryId: String, val error: StreamErrorMessage)