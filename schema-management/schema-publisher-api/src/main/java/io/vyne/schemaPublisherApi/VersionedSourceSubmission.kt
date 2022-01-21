package io.vyne.schemaPublisherApi

import arrow.core.Either
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer
import io.vyne.VersionedSource
import io.vyne.schemaApi.SchemaSet
import io.vyne.schemas.Schema
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.errors
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.io.Serializable
import java.time.Duration

// We gradually want to move away from submission of schemas as a list of source files, to a hollistic package.
// This is a first, gradual step.
// There are existing similar classes, which weren't used for the following reasons:
// lang.taxi.packages.PackageSource: non-nullable Path, which won't serialize to JSON
// lang.taxi.packages.TaxiPackageSources: closer to what we need (as it has dependencies), and ultimately what we should choose
// but a very dense class with references to file systems etc.
data class VersionedSourceSubmission(val sources: List<VersionedSource>, val configuration: PublisherConfiguration) :
   Serializable {
   constructor(sources: List<VersionedSource>, publisherId: String) : this(sources, PublisherConfiguration(publisherId))

   val publisherId = configuration.publisherId
}

data class SourceSubmissionResponse(
   val errors: List<CompilationError>,
   val schemaSet: SchemaSet
) {
   val isValid: Boolean = errors.errors().isEmpty()
   fun asEither(): Either<CompilationException, Schema> {
      if (this.isValid) {
         return Either.right(this.schemaSet.schema)
      }

      return Either.left(CompilationException(this.errors))
   }
}


data class PublisherConfiguration(
   val publisherId: String,
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
   val terminatedInstances: Publisher<PublisherConfiguration>
}

object NoneKeepAliveStrategyMonitor : KeepAliveStrategyMonitor {
   override fun appliesTo(keepAlive: KeepAliveStrategy) = keepAlive.id == KeepAliveStrategyId.None
   override val terminatedInstances: Publisher<PublisherConfiguration> = Flux.empty()
}

