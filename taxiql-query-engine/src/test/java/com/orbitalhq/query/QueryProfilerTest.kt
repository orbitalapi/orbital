package com.orbitalhq.query

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import com.orbitalhq.utils.ManualClock
import org.junit.Before
import org.junit.Test
import java.time.Instant

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
      val result = profiler.startChild("Test", "First", OperationType.GRAPH_TRAVERSAL) { rec ->
         clock.advanceMillis(10)
         rec.startChild("Test", "Child", OperationType.GRAPH_TRAVERSAL) {
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
   fun `Profile Record should stop properly even if wrapped operation throws`() {
      val result = try {
         profiler.startChild("Test", "First", OperationType.GRAPH_TRAVERSAL) { rec ->
            clock.advanceMillis(10)
            rec.startChild("Test", "Child", OperationType.GRAPH_TRAVERSAL) {
               clock.advanceMillis(20)
               "Child Completed"
            }
            clock.advanceMillis(10)
            throw IllegalArgumentException("Parent completed with Error")
         }
      } catch (e: Exception) { }

      val operations = profiler.children
      result.should.equal(result)
      expect(operations).to.have.size(1)
      expect(operations.first().operationName).to.equal("First")
      expect(operations.first().children).to.have.size(1)
      val firstChild = operations.first().children.first()
      expect(firstChild.operationName).to.equal("Child")
      expect(firstChild.result!!.value).to.equal("Child Completed")
   }

   @Test
   fun when_recordingOperation_then_aNewOperationIsCreated() {
      val operation = profiler.startChild("Test", "Foo", OperationType.GRAPH_TRAVERSAL)
      clock.advanceMillis(10)
      operation.stop()

      expect(operation.children).to.have.size(0)
      expect(operation.operationName).to.equal("Foo")
      expect(operation.duration).to.equal(10)
   }

   @Test
   fun given_recordingAnOperation_when_newOperationIsRecorded_then_itIsCapturedAsAChild() {
      val operation = profiler.startChild("Test", "Parent", OperationType.GRAPH_TRAVERSAL)
      clock.advanceMillis(10)

      val child = operation.startChild("Test", "Child", OperationType.GRAPH_TRAVERSAL)
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
      val root = profiler.startChild("test", "parent", OperationType.GRAPH_TRAVERSAL)
      val child1 = profiler.startChild("test", "child1", OperationType.GRAPH_TRAVERSAL)
      child1.stop()
      val child2 = profiler.startChild("test", "child2", OperationType.GRAPH_TRAVERSAL)
      val grandChild1 = profiler.startChild("test", "grandChild1", OperationType.GRAPH_TRAVERSAL)
      expect(root.children).to.have.size(2)
      expect(root.children[1].children).to.have.size(1)
   }

   @Test
   fun canGetTimingsByType() {
      val root = profiler.startChild("test", "parent", OperationType.ROOT)
      clock.advanceMillis(10)

      val child1 = profiler.startChild("test", "child1", OperationType.GRAPH_TRAVERSAL)
      clock.advanceMillis(10)
      child1.stop()

      val child2 = profiler.startChild("test", "child2", OperationType.GRAPH_BUILDING)
      clock.advanceMillis(10)

      val grandChild1 = profiler.startChild("test", "grandChild1", OperationType.GRAPH_TRAVERSAL)
      clock.advanceMillis(10)

      val greatGrandChild = profiler.startChild("test", "greatGrandchild", OperationType.REMOTE_CALL)
      clock.advanceMillis(30)

      greatGrandChild.stop()
      grandChild1.stop()
      child2.stop()

      val timings = root.timings
      expect(timings[OperationType.REMOTE_CALL]!!).to.equal(30)
      // Note: There were two graph traversal oeprations, each lasting 10ms before invoking a child
      expect(timings[OperationType.GRAPH_TRAVERSAL!!]).to.equal(20)
   }


}

