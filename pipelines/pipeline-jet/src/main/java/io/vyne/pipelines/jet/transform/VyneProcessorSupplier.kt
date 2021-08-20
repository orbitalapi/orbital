package io.vyne.pipelines.jet.transform

import com.hazelcast.jet.core.Inbox
import com.hazelcast.jet.core.Outbox
import com.hazelcast.jet.core.Processor
import com.hazelcast.jet.core.ProcessorSupplier
import com.hazelcast.jet.core.Watermark
import io.vyne.Vyne
import io.vyne.models.TypedCollection
import io.vyne.pipelines.ConsoleLogger
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.TypedInstanceContentProvider
import io.vyne.pipelines.jet.pipelines.PipelineMessage
import io.vyne.spring.SimpleVyneProvider
import io.vyne.spring.VyneProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class VyneProcessorSupplier(private val vyneProvider: VyneProvider) : ProcessorSupplier {
   companion object {
      /**
       * A static factory that expects the value has been set (ie., by Spring)
       * on a static variable.  Don't use this for testing.
       */
      val WITH_STATIC_FACTORY = VyneProcessorSupplier(StaticVyneFactory.vyneFactory)

      fun forVyne(vyne:Vyne) = VyneProcessorSupplier(SimpleVyneProvider(vyne))
   }
   override fun get(count: Int): MutableCollection<out Processor> {
      return (0 until count).map { VyneTransformingProcessor(vyneProvider) }
         .toMutableList()
   }
}

class VyneTransformingProcessor(val vyneFactory: VyneProvider) : Processor {

   private lateinit var outbox: Outbox
   private lateinit var context: Processor.Context
   override fun tryProcessWatermark(watermark: Watermark): Boolean = true

   override fun init(outbox: Outbox, context: Processor.Context) {
      this.outbox = outbox
      this.context = context
   }

   override fun process(ordinal: Int, inbox: Inbox) {
      inbox.drain<PipelineMessage> { message ->
         if (message.inputType.parameterizedName == message.outputType.parameterizedName) {
            outbox.offer(ordinal, message)
         } else {
            val transformed = transformWithVyne(ordinal, message)
            if (transformed != null) {
               outbox.offer(ordinal, message)
            }
         }
      }
   }

   private fun transformWithVyne(ordinal: Int, message: PipelineMessage): MessageContentProvider? {
      val vyne = vyneFactory.createVyne()
      val inputType = vyne.schema.type(message.inputType)
      val outputType = vyne.schema.type(message.outputType)


      val input = try {
         message.messageProvider.readAsTypedInstance(ConsoleLogger, inputType, vyne.schema)
      } catch (e: Exception) {
         context.logger().severe("Failed to read input message $ordinal of type $inputType - ${e.message}")
         return null
      }

      val result = runBlocking {
         vyne.from(input).build(outputType.name.parameterizedName)
            .results.toList()
      }

      return when {
         result.isEmpty() -> {
            context.logger()
               .info("Transformation of message $ordinal to type ${outputType.fullyQualifiedName} returned an empty set.  Nothing will be emitted")
            null
         }
         result.size == 1 -> TypedInstanceContentProvider(result.first())
         else -> TypedInstanceContentProvider(TypedCollection.from(result))
      }

   }

}

