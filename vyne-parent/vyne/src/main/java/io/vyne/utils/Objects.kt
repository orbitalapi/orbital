package io.vyne.utils

inline fun <T> T.assertingThat(condition: (T) -> Boolean, message:String? = null): T {
   if (!condition.invoke(this)) {
      throw IllegalStateException(message.orElse("Illegal param passed"))
   }
   return this
}

fun <T> T?.orElse(other:T):T {
   if (this == null) return other else return this
}
