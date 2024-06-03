package com.orbitalhq

import lang.taxi.messages.Severity


data class DefaultResultWithMessage(override val messages: List<Message>) : ResultWithMessage {
   override fun append(message: Message, replaceIfExists: Message?): ResultWithMessage {
      val remainingMessages = messages.filter { it != replaceIfExists }
      return DefaultResultWithMessage(remainingMessages + message)
   }
}

/**
 * A simple object to return from methods where we need
 * to provide messaging (generally, back to the UI).
 *
 * Defined as an interface so that other response objects may also implement,
 * to provide consistent messaging back to the UI.
 *
 * Note - do not use ResultWithMessage on a 200 response to indicate a failure.
 * Always use a 4xx or 5xx to indicate failure (generally by throwing an exception)
 */
interface ResultWithMessage {
   val messages: List<Message>

   fun append(message: Message, replaceIfExists: Message? = null): ResultWithMessage {
      error("ResultWithMessage.append is not implemented by ${this::class.simpleName}")
   }


   val hasWarning: Boolean
      get() {
         return messages.any { it.severity == Severity.WARNING }
      }

   val hasError: Boolean
      get() {
         return messages.any { it.severity == Severity.ERROR }
      }

   companion object {
      val SUCCESS_MESSAGE = Message(Severity.INFO, message = "The operation completed successfully")
      val SUCCESS = DefaultResultWithMessage(listOf(SUCCESS_MESSAGE))
   }
}

data class Message(val severity: Severity, val message: String)

