package io.osmosis.polymer.query

import java.time.Clock
import java.util.*


data class Operation(
   val id: String = UUID.randomUUID().toString(),
   val startTime: Long,
   val endTime: Long,
   val name: String,
   val children: List<Operation>,
   val result: Any? = null
) {
   val duration = endTime - startTime
}


class QueryProfiler(private val clock: Clock = Clock.systemDefaultZone()) {

   private val operations = mutableListOf<Operation>()
   private val activeOperation: Operation? = null
   fun operations(): List<Operation> {
      return operations.toList()
   }

   fun startOperation(name:String = "Root"): OperationRecorder {
      return OperationRecorder(name,clock,parent = null)
   }
}


private typealias OperationId = String
private typealias OperationName = String
class OperationRecorder(private val name:OperationName,
                        private val clock: Clock,
                        private val parent:OperationRecorder?) {
   private var stopped: Boolean = false
   val id:OperationId = UUID.randomUUID().toString()

   private val started:Long = clock.millis()
   private val completedChildren = mutableListOf<Operation>()
   private val startedChildren = mutableMapOf<OperationId,OperationName>()
   fun startChild(name:String):OperationRecorder {
      return OperationRecorder(name,clock,this)
   }
   fun stop(result:Any? = null):Operation {
      // TODO : Assert that all running children have stopped too.
      if (stopped) error("Already stopped")
      val operation = Operation(
         id, started, clock.millis(),name,completedChildren,result)
      if (parent != null) {
         parent.appendCompletedChild(operation)
      }
      stopped = true
      return operation
   }

   private fun appendCompletedChild(operation: Operation) {
      if (stopped) error("Cannot append child when already stopped")
      completedChildren.add(operation)
   }
}
