package io.vyne.query

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * This class serializes query results based on the
 * result mode that was used to execute the query.
 *
 * In earlier iterations, we modified the behaviour of the
 * query context based on the result mode.  this approach
 * defers the decisions until serializzation time, meaning
 * we caputre richer data up-front, but can choose to omit it
 * at serialization time, but still include it again later.
 *
 * Note that the client expects the results to be present
 * in a field called "results", regardless of which mode the query
 * was executed in.  So, we use a little bit of trickery to
 * switch the value we write, based on the query mode (passed in
 * through Jackson context).
 *
 */
@Deprecated("Use QueryResultSerializer instead (accessed from ResultMode)")
class ResultModeAwareResultSerializer : JsonSerializer<QueryResultProvider>() {
   override fun serialize(value: QueryResultProvider, gen: JsonGenerator, serializers: SerializerProvider) {
      val resultMode = serializers.getAttribute(ResultMode::class) as ResultMode? ?: ResultMode.SIMPLE
      when (resultMode) {
         ResultMode.SIMPLE -> gen.writeObject(value.simple())
         ResultMode.VERBOSE -> gen.writeObject(value.verbose())
      }
   }

}

data class QueryResultProvider(
   val verbose: () -> Map<String, Any?>,
   val simple: () -> Map<String, Any?>
)
