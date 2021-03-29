package io.vyne.cask

import java.util.*

object MessageIds  {
   fun uniqueId(): String {
      return UUID.randomUUID().toString()
   }
}
