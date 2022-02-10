package io.vyne.connectors.aws.sqs

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.services.sqs.model.Message
import java.util.function.BiFunction
import java.util.function.Supplier

class SqsReceiver(private val receiverOptions: SnsReceiverOptions) {
   private var consumerHandler: SqsConsumerHandler? = null

   fun receive(): Flux<Message> {
      return withHandler { scheduler: Scheduler, handler: SqsConsumerHandler ->
         handler
            .receive()
            .filter { it.hasMessages() }
            .flatMapIterable { it.messages() }
            .publishOn(scheduler)
      }
   }

   private fun <T> withHandler(function: BiFunction<Scheduler,SqsConsumerHandler, Flux<T>>): Flux<T> {
      return Flux.usingWhen(
         Mono.fromCallable {
            consumerHandler = SqsConsumerHandler(SqsConnection(receiverOptions))
            consumerHandler
         },
         { handler: SqsConsumerHandler ->
            Flux.using(
               { Schedulers.single(Supplier { Schedulers.immediate() }.get()) },
               { scheduler: Scheduler -> function.apply(scheduler, handler) }) { obj: Scheduler -> obj.dispose() }
         }
      ) { handler: SqsConsumerHandler -> handler.close().doFinally { consumerHandler = null } }
   }
}
