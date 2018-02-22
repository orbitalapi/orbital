package io.osmosis.polymer.query

import java.time.Clock
import java.util.*


class QueryProfiler(private val clock: Clock = Clock.systemDefaultZone(), val root: ProfilerOperation = DefaultProfilerOperation(QueryProfiler::class.java.name, "Root", clock)) : ProfilerOperation by root {
   private val operationStack: Deque<ProfilerOperation> = ArrayDeque(listOf(root))
   override fun startChild(ownerInstance: Any, name: String): ProfilerOperation = startChild(ownerInstance::class.java.simpleName, name)
   override fun startChild(clazz: Class<Any>, name: String): ProfilerOperation = startChild(clazz.simpleName, name)

   override fun <R> startChild(ownerInstance: Any, name: String, closure: (ProfilerOperation) -> R): R = startChild(ownerInstance::class.java.simpleName, name, closure)
   override fun <R> startChild(clazz: Class<Any>, name: String, closure: (ProfilerOperation) -> R): R = startChild(clazz.simpleName, name, closure)



   override fun startChild(componentName: String, operationName: String): ProfilerOperation {
      val child = operationStack.peekLast().startChild(componentName, operationName)
      operationStack.offerLast(child)
      return StackedOperation(operationStack, child)
   }

   override fun <R> startChild(componentName: String, operationName: String, closure: (ProfilerOperation) -> R): R {
      val recorder = startChild(componentName, operationName)
      val result = closure.invoke(recorder)
      recorder.stop(result)
      return result
   }

   private class StackedOperation(private val stack: Deque<ProfilerOperation>, private val operation: ProfilerOperation) : ProfilerOperation by operation {
      override fun stop(result: Any?) {
         operation.stop(result)
         if (stack.peekLast() == operation) {
            stack.pollLast()
         } else {
            error("Stopped an operation ${operation.description}, but the head of the stack is ${stack.peekLast().description}")
         }
      }
   }
}


private typealias OperationId = String
private typealias OperationName = String

interface ProfilerOperation {
   fun startChild(ownerInstance: Any, name: String): ProfilerOperation = startChild(ownerInstance::class.java.simpleName, name)
   fun startChild(clazz: Class<Any>, name: String): ProfilerOperation = startChild(clazz.simpleName, name)
   fun startChild(componentName: String, operationName: String): ProfilerOperation

   fun <R> startChild(ownerInstance: Any, name: String, closure: (ProfilerOperation) -> R): R = startChild(ownerInstance::class.java.simpleName, name, closure)
   fun <R> startChild(clazz: Class<Any>, name: String, closure: (ProfilerOperation) -> R): R = startChild(clazz.simpleName, name, closure)
   fun <R> startChild(componentName: String, operationName: String, closure: (ProfilerOperation) -> R): R
   fun stop(result: Any? = null)

   val componentName: String
   val operationName: String
   val children: List<ProfilerOperation>
   val result: Result?

   val duration: Long

   val context: Map<String, Any?>

   fun addContext(key: String, value: Any?)

   val description: String
      get() {
         return "$componentName.$operationName"
      }
}

data class Result(
   val startTime: Long,
   val endTime: Long,
   val value: Any? = null
) {
   val duration = endTime - startTime
}

class DefaultProfilerOperation(override val componentName: String,
                               override val operationName: String,
                               private val clock: Clock) : ProfilerOperation {
   override val context: MutableMap<String, Any?> = mutableMapOf()
   override var result: Result? = null
      private set
   private val stopped: Boolean
      get() {
         return result != null
      }

   val id: OperationId = UUID.randomUUID().toString()

   private val startTime: Long = clock.millis()
   override val children = mutableListOf<ProfilerOperation>()

   override fun startChild(componentName: String, operationName: String): ProfilerOperation {
      val child = DefaultProfilerOperation(componentName, operationName, clock)
      children.add(child)
      return child
   }

   override val duration: Long
      get() {
         return result?.duration ?: clock.millis() - startTime
      }

   override fun <R> startChild(componentName: String, operationName: String, closure: (ProfilerOperation) -> R): R {
      val recorder = startChild(componentName, operationName)
      val result = closure.invoke(recorder)
      recorder.stop(result)
      return result
   }

   override fun stop(result: Any?) {
      // TODO : Assert that all running children have stopped too.
      if (stopped) error("Already stopped")
      this.result = Result(this.startTime, clock.millis(), result)
   }

   override fun addContext(key: String, value: Any?) {
      context[key] = value
   }
}
