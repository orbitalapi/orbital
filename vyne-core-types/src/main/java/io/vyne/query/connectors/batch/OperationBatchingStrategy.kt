package io.vyne.query.connectors.batch

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlin.time.Duration

/**
 * Checks to see if an operation can be
 * converted into a batched operation.
 *
 * If so, is responsible for receving the
 * unbatched invocation request, batching it,
 * and unbatching the result flow, such that
 * the requestor was unaware of the batching
 * that took place.
 */
interface OperationBatchingStrategy {
   fun canBatch(
      service: Service,
      operation: RemoteOperation,
      schema: Schema,
      preferredParams: Set<TypedInstance>,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): Boolean

   /**
    * Appends the request to a pending batch
    * (or creates a new pending batch if one doesn't
    * exist).
    *
    * Will eventually resolve with the result
    * of the call, having unwrapped back to the
    * single value.
    */
   suspend fun invokeInBatch(
      service: Service,
      operation: RemoteOperation,
      preferredParams: Set<TypedInstance>,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      schema: Schema,
      queryId: String? = null
   ): Flow<TypedInstance>
}

@OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.bufferTimeout(size: Int, duration: Duration): Flow<List<T>> {
   require(size > 0) { "Window size should be greater than 0" }
   require(duration.inWholeMilliseconds > 0) { "Duration should be greater than 0" }

   return flow {
      coroutineScope {
         val events = ArrayList<T>(size)
         val tickerChannel = ticker(duration.inWholeMilliseconds)
         try {
            val upstreamValues = produce { collect { send(it) } }

            while (isActive) {
               var hasTimedOut = false

               select<Unit> {
                  upstreamValues.onReceive {
                     events.add(it)
                  }

                  tickerChannel.onReceive {
                     hasTimedOut = true
                  }
               }

               if (events.size == size || (hasTimedOut && events.isNotEmpty())) {
                  emit(events.toList())
                  events.clear()
               }
            }
         } catch (e: ClosedReceiveChannelException) {
            // drain remaining events
            if (events.isNotEmpty()) emit(events.toList())
         } finally {
            tickerChannel.cancel()
         }
      }
   }
}

