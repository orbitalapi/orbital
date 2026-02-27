package com.orbitalhq.history.db.tracing

import com.orbitalhq.query.history.tracing.TraceEventRow
import com.orbitalhq.query.history.tracing.TraceSpanRecord
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TraceSpanBuilderTest : DescribeSpec({
   val builder = TraceSpanBuilder()
   describe("getSpansForQuery") {

      context("when no events exist for query") {
         it("should return empty list") {
            val result = builder.buildTraceSpans(emptyList())

            result.shouldBeEmpty()
         }

         context("when spans have different start times") {
            it("should calculate correct offset times") {
               val baseTime = ZonedDateTime.parse("2024-01-01T10:00:00Z")
               val span1Time = baseTime
               val span2Time = baseTime.plusSeconds(2)
               val span3Time = baseTime.plusSeconds(5)

               val events = listOf(
                  createTraceEvent(eventId = "event1", spanId = "span1", timestamp = span1Time),
                  createTraceEvent(eventId = "event2", spanId = "span2", timestamp = span2Time),
                  createTraceEvent(eventId = "event3", spanId = "span3", timestamp = span3Time)
               )

               val result = builder.buildTraceSpans(events)

               result shouldHaveSize 3

               val sortedSpans = result.sortedBy { it.spanId }
               sortedSpans[0].offsetMs shouldBe 0L      // span1 starts at trace beginning
               sortedSpans[1].offsetMs shouldBe 2000L   // span2 starts 2 seconds later
               sortedSpans[2].offsetMs shouldBe 5000L   // span3 starts 5 seconds later
            }
         }
      }

      context("when single span with complete start/end events exists") {
         it("should create span with correct timestamps") {
            val startTime = ZonedDateTime.parse("2024-01-01T10:00:00Z")
            val endTime = ZonedDateTime.parse("2024-01-01T10:00:01Z")

            val events = listOf(
               createTraceEvent(
                  eventId = "event1",
                  spanId = "span1",
                  spanState = SpanState.ACTIVE,
                  timestamp = startTime,
                  eventVerb = "Start",
                  eventResource = "TestResource"
               ),
               createTraceEvent(
                  eventId = "event2",
                  spanId = "span1",
                  spanState = SpanState.COMPLETE,
                  timestamp = endTime,
                  eventVerb = "End",
                  eventResource = "TestResource"
               )
            )

            val result = builder.buildTraceSpans(events)

            result shouldHaveSize 1
            val span = result.first()
            span.spanId shouldBe "span1"
            span.events shouldHaveSize 2
            span.spanStartTimestamp shouldBe startTime.toInstant()
            span.spanEndTimestamp shouldBe endTime.toInstant()
            span.firstEventTimestamp shouldBe startTime.toInstant()
            span.lastEventTimestamp shouldBe endTime.toInstant()
            span.eventVerb shouldBe "Start"
            span.eventResource shouldBe "TestResource"
            span.isComplete shouldBe true
            span.durationMs shouldBe 1000L // Duration between first and last events
            span.spanDurationMs shouldBe 1000L // Duration between start and end span events
            span.offsetMs shouldBe 0L // First span starts at offset 0
            span.hasErrors shouldBe false
         }
      }

      context("when span has only start event") {
         it("should handle missing end event gracefully") {
            val startTime = ZonedDateTime.parse("2024-01-01T10:00:00Z")

            val events = listOf(
               createTraceEvent(
                  eventId = "event1",
                  spanId = "span1",
                  spanState = SpanState.ACTIVE,
                  timestamp = startTime
               )
            )

            val result = builder.buildTraceSpans(events)

            result shouldHaveSize 1
            val span = result.first()
            span.spanStartTimestamp shouldBe startTime.toInstant()
            span.spanEndTimestamp.shouldBeNull()
            span.isComplete shouldBe false
            span.durationMs shouldBe 0L // Same first and last event
            span.spanDurationMs.shouldBeNull()
         }
      }

      context("when span has error events") {
         it("should detect errors correctly") {
            val events = listOf(
               createTraceEvent(
                  eventId = "event1",
                  spanId = "span1",
                  tracingEventKind = TracingEventKind.ERROR
               )
            )

            val result = builder.buildTraceSpans(events)

            result shouldHaveSize 1
            result.first().hasErrors shouldBe true
         }
      }

      context("when multiple spans with parent-child relationship exist") {
         it("should build correct hierarchy") {
            val parentTime = ZonedDateTime.parse("2024-01-01T10:00:00Z")
            val childTime = ZonedDateTime.parse("2024-01-01T10:00:00.5Z")

            val events = listOf(
               createTraceEvent(
                  eventId = "parent-event",
                  spanId = "parent-span",
                  parentSpanId = null,
                  timestamp = parentTime,
                  eventVerb = "ParentAction"
               ),
               createTraceEvent(
                  eventId = "child-event",
                  spanId = "child-span",
                  parentSpanId = "parent-span",
                  timestamp = childTime,
                  eventVerb = "ChildAction"
               )
            )

            val result = builder.buildTraceSpans(events)

            result shouldHaveSize 1 // Only root spans
            val parentSpan = result.first()
            parentSpan.spanId shouldBe "parent-span"
            parentSpan.parentSpanId.shouldBeNull()
            parentSpan.children shouldHaveSize 1

            val childSpan = parentSpan.children.first()
            childSpan.spanId shouldBe "child-span"
            childSpan.parentSpanId shouldBe "parent-span"
            childSpan.eventVerb shouldBe "ChildAction"
         }

         it("should correctly nest children that are multiple layers deep") {
            // Arrange - Create 3-level hierarchy: A -> B -> C
            val traceId = "trace-123"
            val spanA = "span-a"
            val spanB = "span-b"
            val spanC = "span-c"
            val baseTime = ZonedDateTime.parse("2024-01-01T10:00:00Z")

            val events = listOf(
               // Span A events (root)
               createTraceEvent(
                  spanId = spanA,
                  parentSpanId = null,
                  timestamp = baseTime,
                  spanState = SpanState.ACTIVE,
               ),
               createTraceEvent(
                  spanId = spanA,
                  parentSpanId = null,
                  spanState = SpanState.COMPLETE,
                  timestamp = baseTime.plusSeconds(5),
               ),

               // Span B events (child of A)
               createTraceEvent(
                  spanId = spanB,
                  parentSpanId = spanA,
                  spanState = SpanState.ACTIVE,
                  timestamp = baseTime.plusSeconds(1),
               ),
               createTraceEvent(
                  spanId = spanB,
                  parentSpanId = spanA,
                  spanState = SpanState.COMPLETE,
                  timestamp = baseTime.plusSeconds(4),
               ),

               // Span C events (child of B, grandchild of A)
               createTraceEvent(
                  spanId = spanC,
                  parentSpanId = spanB,
                  spanState = SpanState.ACTIVE,
                  timestamp = baseTime.plusSeconds(2),
               ),
               createTraceEvent(
                  spanId = spanC,
                  parentSpanId = spanB,
                  spanState = SpanState.COMPLETE,
                  timestamp = baseTime.plusSeconds(3), // 2.5 seconds
               )
            )

            val root = builder.buildTraceSpans(events)
            root.shouldHaveSize(1) // should be a single root
            root.single().events.shouldHaveSize(2)
            root.single().children.shouldHaveSize(1) // root (span-a) should have a single child (span-b)
            val firstChild = root.single().children.first()
            firstChild.events.shouldHaveSize(2)
            firstChild.children.shouldHaveSize(1) // span-b should have a single child (span-c)
            firstChild.children.single().events.shouldHaveSize(2)

         }

      }

      context("when span has multiple events of same type") {
         it("should handle multiple events correctly") {
            val time1 = ZonedDateTime.parse("2024-01-01T10:00:00Z")
            val time2 = ZonedDateTime.parse("2024-01-01T10:00:01Z")
            val time3 = ZonedDateTime.parse("2024-01-01T10:00:02Z")

            val events = listOf(
               createTraceEvent(
                  eventId = "event1",
                  spanId = "span1",
                  spanState = SpanState.ACTIVE,
                  timestamp = time1,
                  eventVerb = "Subscribe"
               ),
               createTraceEvent(
                  eventId = "event2",
                  spanId = "span1",
                  spanState = SpanState.ACTIVE, // Another active event (e.g., Kafka message)
                  timestamp = time2,
                  eventVerb = "Receive"
               ),
               createTraceEvent(
                  eventId = "event3",
                  spanId = "span1",
                  spanState = SpanState.COMPLETE,
                  timestamp = time3,
                  eventVerb = "Complete"
               )
            )

            val result = builder.buildTraceSpans(events)

            result shouldHaveSize 1
            val span = result.first()
            span.events shouldHaveSize 3
            span.spanStartTimestamp shouldBe time1.toInstant() // First ACTIVE event
            span.spanEndTimestamp shouldBe time3.toInstant() // First COMPLETE event
            span.firstEventTimestamp shouldBe time1.toInstant()
            span.lastEventTimestamp shouldBe time3.toInstant()
            span.eventVerb shouldBe "Subscribe" // From first event chronologically
            span.durationMs shouldBe 2000L // Duration between first and last events
            span.spanDurationMs shouldBe 2000L // Duration between start and end span events
         }
      }
   }

   describe("flattenSpansForWaterfall") {

      context("when given hierarchical spans") {
         it("should flatten while maintaining chronological order") {
            val parentTime = ZonedDateTime.parse("2024-01-01T10:00:00Z")
            val child1Time = ZonedDateTime.parse("2024-01-01T10:00:00.5Z")
            val child2Time = ZonedDateTime.parse("2024-01-01T10:00:01Z")
            val traceStartTime = parentTime.toInstant() // Parent starts first

            val child1 = TraceSpanRecord(
               spanId = "child1",
               parentSpanId = "parent",
               events = listOf(createTraceEvent(spanId = "child1", timestamp = child1Time)),
               traceStartTime = traceStartTime
            )

            val child2 = TraceSpanRecord(
               spanId = "child2",
               parentSpanId = "parent",
               events = listOf(createTraceEvent(spanId = "child2", timestamp = child2Time)),
               traceStartTime = traceStartTime
            )

            val parent = TraceSpanRecord(
               spanId = "parent",
               parentSpanId = null,
               events = listOf(createTraceEvent(spanId = "parent", timestamp = parentTime)),
               children = mutableListOf(child1, child2),
               traceStartTime = traceStartTime
            )

            val result = builder.flattenSpansForWaterfall(listOf(parent))

            result shouldHaveSize 3
            result[0].spanId shouldBe "parent"
            result[0].offsetMs shouldBe 0L
            result[1].spanId shouldBe "child1"
            result[1].offsetMs shouldBe 500L // 0.5 seconds after parent
            result[2].spanId shouldBe "child2"
            result[2].offsetMs shouldBe 1000L // 1 second after parent
         }
      }
   }

   describe("getTraceSummary") {

      context("when trace has multiple spans with errors") {
         it("should calculate correct summary statistics") {
            val events = listOf(
               createTraceEvent(
                  eventId = "event1",
                  spanId = "span1",
                  spanState = SpanState.ACTIVE,
                  timestamp = ZonedDateTime.parse("2024-01-01T10:00:00Z")
               ),
               createTraceEvent(
                  eventId = "event2",
                  spanId = "span1",
                  spanState = SpanState.COMPLETE,
                  timestamp = ZonedDateTime.parse("2024-01-01T10:00:01Z")
               ),
               createTraceEvent(
                  eventId = "event3",
                  spanId = "span2",
                  spanState = SpanState.ACTIVE,
                  tracingEventKind = TracingEventKind.ERROR,
                  timestamp = ZonedDateTime.parse("2024-01-01T10:00:02Z")
               )
            )

            val result = builder.getTraceSummary("queryId", events)

            result.queryId shouldBe "queryId"
            result.totalSpans shouldBe 2
            result.totalEvents shouldBe 3
            result.totalDurationMs shouldBe 2000L // 2 seconds from first to last event
            result.hasErrors shouldBe true
            result.completedSpans shouldBe 1
            result.incompleteSpans shouldBe 1
         }
      }

      context("when no events exist") {
         it("should return empty summary") {
            val result = builder.getTraceSummary("empty-query", emptyList())

            result.queryId shouldBe "empty-query"
            result.totalSpans shouldBe 0
            result.totalEvents shouldBe 0
            result.totalDurationMs shouldBe 0
            result.hasErrors shouldBe false
            result.completedSpans shouldBe 0
            result.incompleteSpans shouldBe 0
         }
      }
   }
})

// Helper function to create test trace events
private fun createTraceEvent(
   eventId: String = "test-event",
   queryId: String = "query-id",
   traceId: String = "test-trace",
   spanId: String = "test-span",
   parentSpanId: String? = null,
   tracingEventKind: TracingEventKind = TracingEventKind.OK,
   spanState: SpanState = SpanState.ACTIVE,
   timestamp: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
   exchangeMetadata: String = "{}",
   eventVerb: String = "TestVerb",
   eventResource: String = "TestResource",
   eventSourceQualifiedName: String = "com.example.TestSource"
) = TraceEventRow(
   eventId = eventId,
   queryId = queryId,
   traceId = traceId,
   spanId = spanId,
   parentSpanId = parentSpanId,
   tracingEventKind = tracingEventKind,
   spanState = spanState,
   timestamp = timestamp,
   exchangeMetadata = exchangeMetadata,
   eventVerb = eventVerb,
   eventResource = eventResource,
   eventSourceQualifiedName = eventSourceQualifiedName,
   linkedEventId = null,
   direction = TraceEventDirection.OUTBOUND,
   remoteCallId = "remote-call-id"
)
