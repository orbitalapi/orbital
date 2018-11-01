package io.vyne.query

import io.vyne.schemas.QualifiedName
import io.vyne.utils.log
import java.time.Clock
import java.util.*


class QueryProfiler(private val clock: Clock = Clock.systemDefaultZone(), val root: ProfilerOperation = DefaultProfilerOperation(QueryProfiler::class.java.name, "Root", OperationType.ROOT, clock, path = "/")) : ProfilerOperation by root {
   private val operationStack: Deque<ProfilerOperation> = ArrayDeque(listOf(root))
   override fun startChild(ownerInstance: Any, name: String, type: OperationType): ProfilerOperation = startChild(ownerInstance::class.java.simpleName, name, type)
   override fun startChild(clazz: Class<Any>, name: String, type: OperationType): ProfilerOperation = startChild(clazz.simpleName, name, type)

   override fun <R> startChild(ownerInstance: Any, name: String, type: OperationType, closure: (ProfilerOperation) -> R): R = startChild(ownerInstance::class.java.simpleName, name, type, closure)
   override fun <R> startChild(clazz: Class<Any>, name: String, type: OperationType, closure: (ProfilerOperation) -> R): R = startChild(clazz.simpleName, name, type, closure)


   override fun startChild(componentName: String, operationName: String, type: OperationType): ProfilerOperation {
      val child = operationStack.peekLast().startChild(componentName, operationName, type)
      operationStack.offerLast(child)
      return StackedOperation(operationStack, child)
   }

   override fun <R> startChild(componentName: String, operationName: String, type: OperationType, closure: (ProfilerOperation) -> R): R {
      val recorder = startChild(componentName, operationName, type)
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
            log().error("Stopped an operation ${operation.description}, but the head of the stack is ${stack.peekLast().description}")
         }
      }
   }
}


private typealias OperationId = String
private typealias OperationName = String

enum class OperationType {
   ROOT,
   GRAPH_TRAVERSAL,
   LOOKUP,
   REMOTE_CALL
}

interface ProfilerOperation {
   fun startChild(ownerInstance: Any, name: String, type: OperationType): ProfilerOperation = startChild(ownerInstance::class.java.simpleName, name, type)
   fun startChild(clazz: Class<Any>, name: String, type: OperationType): ProfilerOperation = startChild(clazz.simpleName, name, type)
   fun startChild(componentName: String, operationName: String, type: OperationType): ProfilerOperation

   fun <R> startChild(ownerInstance: Any, name: String, type: OperationType, closure: (ProfilerOperation) -> R): R = startChild(ownerInstance::class.java.simpleName, name, type, closure)
   fun <R> startChild(clazz: Class<Any>, name: String, type: OperationType, closure: (ProfilerOperation) -> R): R = startChild(clazz.simpleName, name, type, closure)
   fun <R> startChild(componentName: String, operationName: String, type: OperationType, closure: (ProfilerOperation) -> R): R
   fun stop(result: Any? = null)

   val componentName: String
   val operationName: String
   val children: List<ProfilerOperation>
   val result: Result?
   val type: OperationType

   val duration: Long

   val context: Map<String, Any?>
   val remoteCalls: List<RemoteCall>

   fun addContext(key: String, value: Any?)

   fun addRemoteCall(remoteCall: RemoteCall)

   val description: String
      get() {
         return "$componentName.$operationName"
      }
}

data class RemoteCall(
   val service: QualifiedName,
   val addresss:String,
   val operation: String,
   val method: String,
   val requestBody: Any?,
   val resultCode: Int,
   val durationMs: Long,
   val response: Any?
)

data class Result(
   val startTime: Long,
   val endTime: Long,
   val value: Any? = null
) {
   val duration = endTime - startTime
}

class DefaultProfilerOperation(override val componentName: String,
                               override val operationName: String,
                               override val type: OperationType,
                               private val clock: Clock,
                               val path: String = "/") : ProfilerOperation {
   override val context: MutableMap<String, Any?> = mutableMapOf()
   override val remoteCalls: MutableList<RemoteCall> = mutableListOf()

   override var result: Result? = null
      private set
   private val stopped: Boolean
      get() {
         return result != null
      }

   val name: String = "$componentName:$operationName"
   val fullPath = "$path/$name"

   val id: OperationId = UUID.randomUUID().toString()

   private val startTime: Long = clock.millis()
   override val children = mutableListOf<ProfilerOperation>()

   override fun startChild(componentName: String, operationName: String, type: OperationType): ProfilerOperation {
      val child = DefaultProfilerOperation(componentName, operationName, type, clock, this.fullPath)
      children.add(child)
      return child
   }

   override val duration: Long
      get() {
         return result?.duration ?: clock.millis()-startTime
      }

   override fun <R> startChild(componentName: String, operationName: String, type: OperationType, closure: (ProfilerOperation) -> R): R {
      val recorder = startChild(componentName, operationName, type)
      val result = closure.invoke(recorder)
      recorder.stop(result)
      return result
   }

   override fun stop(result: Any?) {
      // TODO : Assert that all running children have stopped too.
      if (stopped) {
         log().error("Attempted to stop operation $fullPath which is already stopped")
      }
      this.result = Result(this.startTime, clock.millis(), result)
   }

   override fun addContext(key: String, value: Any?) {
      context[key] = value
   }

   override fun addRemoteCall(remoteCall: RemoteCall) {
      remoteCalls.add(remoteCall)
   }

}
