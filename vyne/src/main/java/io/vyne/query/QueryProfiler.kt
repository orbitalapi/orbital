package io.vyne.query

import io.vyne.models.TypedCollection
import arrow.core.MapKOf
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import io.vyne.utils.log
import java.time.Clock
import java.util.*
import kotlin.collections.HashMap

// TODO Make it configurable https://projects.notional.uk/youtrack/issue/LENS-164
// Disabling atm as the data is needed by the UI
class NoOpQueryProfiler(clock: Clock = Clock.systemDefaultZone(), root: ProfilerOperation = DefaultProfilerOperation.root(clock)) : QueryProfiler(clock, root) {
   override fun startChild(componentName: String, operationName: String, type: OperationType): ProfilerOperation {
      return this
   }

   override fun <R> startChild(componentName: String, operationName: String, type: OperationType, closure: (ProfilerOperation) -> R): R {
      val start = System.currentTimeMillis()
      val result = closure.invoke(this)
      val executionTime = System.currentTimeMillis() - start
      if (executionTime > 500/*milliseconds*/) {
         log().info("startChild $componentName/$operationName/$type took ${executionTime}[ms] to complete")
      }
      return result
   }

   override fun stop(result: Any?) {
   }

   override val componentName: String = ""
   override val operationName: String = ""
   override val children: List<ProfilerOperation> = emptyList()
   override val result: Result? = null
   override val type: OperationType = OperationType.ROOT
   override val duration: Long = 0
   override val context: Map<String, Any?> = emptyMap()
   override val remoteCalls: List<RemoteCall> = emptyList()

   override fun addContext(key: String, value: Any?) {
   }

   override fun addRemoteCall(remoteCall: RemoteCall) {
   }
}

open class QueryProfiler(private val clock: Clock = Clock.systemDefaultZone(), val root: ProfilerOperation = DefaultProfilerOperation.root(clock)) : ProfilerOperation by root {
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
      log().debug("Profiler start: {} / {}", componentName, operationName)
      val recorder = startChild(componentName, operationName, type)
      val result = closure.invoke(recorder)
      recorder.stop(result)
      log().debug("Profiler stop: {} / {} ({}ms)", componentName, operationName, recorder.duration)
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

enum class OperationType(
   /**
    * Indicates if the cost is part of Vyne doing work, or would've normally occurred anyway
    */
   val isInternal: Boolean) {
   ROOT(false),
   GRAPH_BUILDING(true),
   GRAPH_TRAVERSAL(true),
   LOOKUP(true),
   POLICY_EVALUATION(isInternal = true),
   REMOTE_CALL(false)
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

   /**
    * Returns a map of operationType to processing duration.
    * Note that the time spent on children operations is removed, to avoid
    * double-counting.
    */
   val timings: Map<OperationType, Long>
      get() {
         val childOperations = children.flatMap { child ->
            child.timings.toList()
         }
         val durationOfChildOperations = childOperations.map { it.second }.sum()
         val myDuration = duration - durationOfChildOperations
         val operations = childOperations + listOf(this.type to myDuration)
         return operations
            .groupingBy { it.first }
            .fold(0L) { accumulator, element -> accumulator + element.second }
      }

   val vyneCost: Long
      get() = timings.filterKeys { it.isInternal }.values.sum()

   fun addContext(key: String, value: Any?)

   fun addRemoteCall(remoteCall: RemoteCall)

   val description: String
      get() {
         return "$componentName.$operationName"
      }

   fun toDto(): ProfilerOperationDTO {
      return ProfilerOperationDTO(
         componentName,
         operationName,
         children.map { it.toDto() },
         result,
         type,
         duration,
         remoteCalls,
         context,
         timings)
   }
}

class ProfilerOperationDTO(val componentName: String,
                           val operationName: String,
                           val children: List<ProfilerOperationDTO> = listOf(),
                           val result: Result?,
                           val type: OperationType,
                           val duration: Long,
                           val remoteCalls: List<RemoteCall> = listOf(),
                           val context: Map<String, Any?> = mapOf(),
                           val timings: Map<OperationType, Long> = mapOf()) {
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

   companion object {
      fun merge(resultA: Result?, resultB: Result?): Result? {
         if (resultA == null && resultB == null) return null;
         if (resultA != null && resultB == null) return resultA;
         if (resultA == null && resultB != null) return resultB;

         val values = listOfNotNull(resultA!!.value, resultB!!.value)
         val resultValue = when {
            values.isEmpty() -> null
            values.size == 1 -> values.first()
            else -> values
         }
         return Result(
            Math.min(resultA.startTime, resultB.startTime),
            Math.max(resultA.endTime, resultB.endTime),
            resultValue
         )
      }
   }
}


class DefaultProfilerOperation(override val componentName: String,
                               override val operationName: String,
                               override val type: OperationType,
                               private val clock: Clock,
                               val path: String = "/") : ProfilerOperation {
   companion object {
      fun mergeChildren(operationA: ProfilerOperation?, operationB: ProfilerOperation?): ProfilerOperation? {
         if (operationA == null && operationB == null) return null
         if (operationA != null && operationB == null) return operationA
         if (operationA == null && operationB != null) return operationB;

         val root = DefaultProfilerOperation.root()


         root.children.addAll(operationA!!.children + operationB!!.children)
         root.context.putAll(operationA.context + operationB.context)
         root.remoteCalls.addAll(operationA.remoteCalls + operationB.remoteCalls)
         root.result = Result.merge(operationA.result, operationB.result)
         return root
      }

      fun root(clock: Clock = Clock.systemDefaultZone()): DefaultProfilerOperation {
         return DefaultProfilerOperation(QueryProfiler::class.java.name, "Root", OperationType.ROOT, clock, path = "/")
      }

   }

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
         return result?.duration ?: clock.millis() - startTime
      }

   override fun <R> startChild(componentName: String, operationName: String, type: OperationType, closure: (ProfilerOperation) -> R): R {
      val recorder = startChild(componentName, operationName, type)
      val result = closure.invoke(recorder)

      // Operations may choose to stop themselves
      if (recorder.result == null) {
         recorder.stop(result)
      } else {
         log().warn("Operation $operationName stopped itself - really shouldn't do that")
      }
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
