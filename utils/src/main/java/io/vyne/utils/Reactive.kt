package io.vyne.utils

import reactor.core.publisher.Flux

inline fun <reified O> Flux<*>.filterIsInstance(): Flux<O> {
   return this.mapNotNull { message ->
      if (message is O) {
         message as O
      } else {
         null
      }
   }
}
