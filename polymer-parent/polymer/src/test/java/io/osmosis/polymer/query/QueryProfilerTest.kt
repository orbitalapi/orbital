package io.osmosis.polymer.query

import com.winterbe.expekt.expect
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class QueryProfilerTest {

   lateinit var profiler: QueryProfiler
   lateinit var clock: ManualClock
   @Before
   fun setup() {
      clock = ManualClock(Instant.now())
      profiler = QueryProfiler(clock)
   }

   @Test
   fun when_recordingOperatesAsClosure_that_itisCaptured() {
      val result = profiler.startChild("Test", "First") { rec ->
         clock.advanceMillis(10)
         rec.startChild("Test", "Child") {
            clock.advanceMillis(20)
            "Child Completed"
         }
         clock.advanceMillis(10)
         "Parent completed"
      }
      expect(result).to.equal("Parent completed")
      val operations = profiler.children
      expect(operations).to.have.size(1)
      expect(operations.first().operationName).to.equal("First")
      expect(operations.first().result!!.value).to.equal("Parent completed")
      expect(operations.first().children).to.have.size(1)
      val firstChild = operations.first().children.first()
      expect(firstChild.operationName).to.equal("Child")
      expect(firstChild.result!!.value).to.equal("Child Completed")

   }

   @Test
   fun when_recordingOperation_then_aNewOperationIsCreated() {
      val operation = profiler.startChild("Test", "Foo")
      clock.advanceMillis(10)
      operation.stop()

      expect(operation.children).to.have.size(0)
      expect(operation.operationName).to.equal("Foo")
      expect(operation.duration).to.equal(10)
   }

   @Test
   fun given_recordingAnOperation_when_newOperationIsRecorded_then_itIsCapturedAsAChild() {
      val operation = profiler.startChild("Test", "Parent")
      clock.advanceMillis(10)

      val child = operation.startChild("Test", "Child")
      clock.advanceMillis(10)
      child.stop()

      clock.advanceMillis(10)
      operation.stop()

      expect(operation.children).to.have.size(1)
      expect(operation.operationName).to.equal("Parent")
      expect(operation.duration).to.equal(30)

      val childOp = operation.children[0]
      expect(childOp.operationName).to.equal("Child")
      expect(childOp.duration).to.equal(10)
   }

   @Test
   fun canStackOperations() {
      val root = profiler.startChild("test", "parent")
      val child1 = profiler.startChild("test", "child1")
      child1.stop()
      val child2 = profiler.startChild("test", "child2")
      val grandChild1 = profiler.startChild("test", "grandChild1")
      expect(root.children).to.have.size(2)
      expect(root.children[1].children).to.have.size(1)
   }

}


class ManualClock(private val startTime: Instant) : Clock() {
   private var time: Long = startTime.toEpochMilli()
   fun advanceMillis(millis: Long) {
      this.time += millis
   }

   override fun withZone(zone: ZoneId?): Clock {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun getZone(): ZoneId {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun instant(): Instant {
      return Instant.ofEpochMilli(time)
   }

}
