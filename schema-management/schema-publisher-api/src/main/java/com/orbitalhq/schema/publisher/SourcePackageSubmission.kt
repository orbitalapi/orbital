package com.orbitalhq.schema.publisher

import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer
import com.orbitalhq.SourcePackage
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schemas.Schema
import com.orbitalhq.utils.Ids
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.io.Serializable
import java.time.Duration

/**
 * An Id which the publisher provides (or we generate if missing).
 */
typealias PublisherId = String

/**
 * A connection Id, generally assigned by the protocol layer.
 * Eg: the Rsocket Id.  Publishers wouldn't know this
 */
typealias TransportConnectionId = String

data class KeepAlivePackageSubmission(
   val sourcePackage: SourcePackage,
   val keepAlive: KeepAliveStrategy = ManualRemoval,
   val publisherId: PublisherId = Ids.id("publisher-")
) : Serializable {
   fun publisherConfig() = PublisherConfiguration(publisherId, keepAlive)
}

data class SourceSubmissionResponse(
   val errors: List<CompilationError>,
   val schemaSet: SchemaSet
) {
   val isValid: Boolean = errors.isEmpty()
   fun asEither(): Either<CompilationException, Schema> {
      if (this.isValid) {
         return this.schemaSet.schema.right()
      }

      return CompilationException(this.errors).left()
   }

   companion object {
      fun fromEither(either: Either<CompilationException, SchemaSet>): SourceSubmissionResponse {
         return either
            .map { schemaSet -> SourceSubmissionResponse(emptyList(), schemaSet) }
            .getOrHandle { compilationException ->
               SourceSubmissionResponse(
                  compilationException.errors,
                  SchemaSet.EMPTY
               )
            }
      }
   }
}


data class PublisherConfiguration(
   val publisherId: PublisherId,
   val keepAlive: KeepAliveStrategy = ManualRemoval
) : Serializable


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
interface KeepAliveStrategy : Serializable {
   val id: KeepAliveStrategyId
}

enum class KeepAliveStrategyId {
   None, HttpPoll, RSocket;

   companion object {
      fun tryParse(value: String): KeepAliveStrategyId? {
         return try {
            valueOf(value)
         } catch (e: Exception) {
            None
         }
      }
   }
}

object ManualRemoval : KeepAliveStrategy {
   override val id = KeepAliveStrategyId.None
}

object RSocketKeepAlive : KeepAliveStrategy {
   override val id: KeepAliveStrategyId = KeepAliveStrategyId.RSocket
}

data class HttpPollKeepAlive(
   // For Some reason when schema publisher posts this, http encoding pipeline uses jackson beanserialiser
   // rather than DurationSerializer, so we have to explicit state the serializer class here.
   @JsonSerialize(using = DurationSerializer::class)
   val pollFrequency: Duration,
   val pollUrl: String
) : KeepAliveStrategy {
   override val id = KeepAliveStrategyId.HttpPoll
}

interface KeepAliveStrategyMonitor {
   fun appliesTo(keepAlive: KeepAliveStrategy): Boolean
   fun monitor(publisherConfiguration: PublisherConfiguration) {}
   val healthUpdateMessages: Publisher<PublisherHealthUpdateMessage>
}

object NoneKeepAliveStrategyMonitor : KeepAliveStrategyMonitor {
   override fun appliesTo(keepAlive: KeepAliveStrategy) = keepAlive.id == KeepAliveStrategyId.None
   override val healthUpdateMessages: Publisher<PublisherHealthUpdateMessage> = Flux.empty()
}

data class PublisherHealthUpdateMessage(
   val id: PublisherId,
   val health: PublisherHealth
)
