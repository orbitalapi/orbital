package io.vyne.pipelines.runner.transport.cask

import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.PipelineLogger
import io.vyne.utils.log
import reactor.core.publisher.FluxSink
import java.io.OutputStream
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Consumer

// This is to resolve the issue where a pipeline input (e.g. KafkaInput) pushes message to
// CaskOutput when CaskOutput hasn't established the web socket session with Cask.
// This can happen when there are 'multiple' CaskOutputs associated with the given pipeline.
// As an example:
// Pipeline:  Input -> Kafka, Output: CaskOutput for type1, CaskOutput for type2
// 1. Pipeline is started
// 2. Kafka Input connects to Kafka and hence becomes UP
// 3. CaskOutput for type 1 established the websocket session with Cask and publishes UP message
// 4. Pipeline see both input and output as UP, so starts the input
// 5. KafkaInput gets message for type2 from Kafka and pushes it to CaskOutput for type2
// 6. CaskOutput can't push that message to Cask as web socket session not established yet.
// I've looked into using existing Reactor constructs like ReplayProcessor to resolve the issue, but there are two potential issues with it:
// ReplayProcessor needs either a size based or a time based setting to determine the number of messages to replay once the websocket session
// established and neither of these can't be known beforehand (We don't know how much time we need to establish the socket session with Cask for
// Cask Output 2 and we also don't know the number of Kafka messages that we receive in Cask Output 2 in the mean time)
// Also, same 'ReplayProcessor' would be used if we ever need to reconnect causing CaskOuput to send duplicates to the Cask upon reconnection.
class CaskOutputMessageProvider(
   private val executor: Executor,
   private val messageQueue: BlockingQueue<MessageContentProvider> = LinkedBlockingQueue()):
   Consumer<FluxSink<MessageContentProvider>>{
   fun write(message: MessageContentProvider) {
      messageQueue.offer(message)
   }

   // Upon establising web socket session.
   // websocketsession.send invokes this.
   override fun accept(sink: FluxSink<MessageContentProvider>) {
      executor.execute {
         while (true) try {
            val event: MessageContentProvider? = messageQueue.take()
            if (event is PoisonPill) {
               // web socket session is terminated, corresponding sink is cancelled.
               // exit from this loop, so that upon reconnection, new flux for
               // websocketsession.send can enter into processing loop.
               log().info("exiting cask message publishing loop, this must be due to websocket termination (Cask restart?)")
               break
            }
            sink.next(event)
         } catch (e: InterruptedException) {
            log().error("error in dispatching web socket events", e)
         }
      }
   }
}

// Used to terminate CaskOutputMessageProvider processing loop, when the underlying websocket session is terminated.
// CaskOutput injects an instance of this in its handleWebsocketTermination method.
class PoisonPill: MessageContentProvider {
   override fun asString(logger: PipelineLogger): String {
      TODO("Not yet implemented")
   }

   override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
      TODO("Not yet implemented")
   }

}
