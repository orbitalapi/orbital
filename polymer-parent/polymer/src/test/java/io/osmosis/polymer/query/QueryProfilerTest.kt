package io.osmosis.polymer.query

import com.winterbe.expekt.expect
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class QueryProfilerTest {

   lateinit var profiler : QueryProfiler
   lateinit var clock:ManualClock
   @Before
   fun setup() {
      clock = ManualClock(Instant.now())
      profiler = QueryProfiler(clock)
   }
   @Test
   fun when_recordingOperation_then_aNewOperationIsCreated() {
      val recorder = profiler.startOperation("Foo")
      clock.advanceMillis(10)
      val operation = recorder.stop()

      expect(operation.children).to.have.size(0)
      expect(operation.name).to.equal("Foo")
      expect(operation.duration).to.equal(10)
   }

   @Test
   fun given_recordingAnOperation_when_newOperationIsRecorded_then_itIsCapturedAsAChild() {
      val recorder = profiler.startOperation("Parent")
      clock.advanceMillis(10)

      val child = recorder.startChild("Child")
      clock.advanceMillis(10)
      child.stop()

      clock.advanceMillis(10)
      val operation = recorder.stop()

      expect(operation.children).to.have.size(1)
      expect(operation.name).to.equal("Parent")
      expect(operation.duration).to.equal(30)

      val childOp = operation.children[0]
      expect(childOp.name).to.equal("Child")
      expect(childOp.duration).to.equal(10)

   }

}


class ManualClock(private val startTime:Instant) : Clock() {
   private var time : Long = startTime.toEpochMilli()
   fun advanceMillis(millis:Long) {
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
