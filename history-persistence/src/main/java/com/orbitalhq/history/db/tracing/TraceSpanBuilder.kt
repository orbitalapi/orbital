package com.orbitalhq.history.db.tracing

import com.orbitalhq.query.history.tracing.TraceEventRow
import com.orbitalhq.query.history.tracing.TraceSpanRecord
import java.time.Duration

class TraceSpanBuilder {
   fun buildTraceSpans(events: List<TraceEventRow>):List<TraceSpanRecord> {
      if (events.isEmpty()) {
         return emptyList()
      }
      // Find the earliest timestamp across all events to use as trace start time
      val traceStartTime = events.minByOrNull { it.timestamp }?.timestamp?.toInstant()
         ?: throw IllegalStateException("Events list should not be empty")

      // Group events by span ID
      val eventsBySpanId = events.groupBy { it.spanId }

      // Create span records
      val spanRecords = eventsBySpanId.map { (spanId, spanEvents) ->
         TraceSpanRecord(
            spanId = spanId,
            parentSpanId = spanEvents.first().parentSpanId,
            events = spanEvents.sortedBy { it.timestamp },
            traceStartTime = traceStartTime
         )
      }

      // Build hierarchy
      return buildSpanHierarchy(spanRecords)
   }
   /**
    * Builds a hierarchical structure of spans where children are nested under their parents.
    * Returns the root spans (those without parents).
    */
   private fun buildSpanHierarchy(spans: List<TraceSpanRecord>): List<TraceSpanRecord> {
      val spanMap = spans.associateBy { it.spanId }.toMutableMap()
      val rootSpans = mutableListOf<TraceSpanRecord>()

      for (span in spans) {
         val parentSpanId = span.parentSpanId
         if (parentSpanId != null) {
            val parentSpan = spanMap[parentSpanId]
            if (parentSpan != null) {
               parentSpan.children.add(span)
            } else {
               // Parent span not found, treat as root
               rootSpans.add(span)
            }
         } else {
            // No parent, this is a root span
            rootSpans.add(span)
         }
      }

      // Sort root spans by first event timestamp
      return rootSpans.sortedBy { it.firstEventTimestamp }
   }

   /**
    * Flattens the hierarchical span structure into a list ordered by start time.
    * Useful for waterfall chart display where you want to show all spans in chronological order
    * while maintaining hierarchy information.
    */
   fun flattenSpansForWaterfall(spans: List<TraceSpanRecord>): List<TraceSpanRecord> {
      val result = mutableListOf<TraceSpanRecord>()

      fun addSpanAndChildren(span: TraceSpanRecord) {
         result.add(span)
         span.children.sortedBy { it.firstEventTimestamp }.forEach { child ->
            addSpanAndChildren(child)
         }
      }

      spans.forEach { addSpanAndChildren(it) }
      return result
   }

   /**
    * Gets summary statistics for the trace
    */
   fun getTraceSummary(queryId: String, events: List<TraceEventRow>): TraceSummary {
      val spans = buildTraceSpans(events)
      val allSpans = flattenSpansForWaterfall(spans)

      if (allSpans.isEmpty()) {
         return TraceSummary(
            queryId = queryId,
            totalSpans = 0,
            totalEvents = 0,
            totalDurationMs = 0,
            hasErrors = false,
            completedSpans = 0,
            incompleteSpans = 0
         )
      }

      val firstTimestamp = allSpans.minByOrNull { it.firstEventTimestamp }?.firstEventTimestamp
      val lastTimestamp = allSpans.maxByOrNull { it.lastEventTimestamp }?.lastEventTimestamp
      val totalDuration = if (firstTimestamp != null && lastTimestamp != null) {
         Duration.between(firstTimestamp, lastTimestamp).toMillis()
      } else 0L

      return TraceSummary(
         queryId = queryId,
         totalSpans = allSpans.size,
         totalEvents = allSpans.sumOf { it.events.size },
         totalDurationMs = totalDuration,
         hasErrors = allSpans.any { it.hasErrors },
         completedSpans = allSpans.count { it.isComplete },
         incompleteSpans = allSpans.count { !it.isComplete }
      )
   }
}


// Summary data class
data class TraceSummary(
   val queryId: String,
   val totalSpans: Int,
   val totalEvents: Int,
   val totalDurationMs: Long,
   val hasErrors: Boolean,
   val completedSpans: Int,
   val incompleteSpans: Int
)
