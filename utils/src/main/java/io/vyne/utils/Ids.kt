package io.vyne.utils

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import java.util.*

object Ids {
   /**
    * Must only contain letters that would produce a valid taxi identifier. ie - exclude "-", since that's
    * invalid as the first character in an indentifier.
    */
   val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
   fun id(prefix: String, size: Int = 6): String =
      prefix + NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, ALPHABET, size)

   private val random: Random = Random()

   /**
    * Returns a UUID that is fast to generate, but with less guarantee of guessability.
    * ie., Where a true UUID is guaranteed to be both probably unique and unguessable, these
    * UUIDs are probably unique, but guessable.
    *
    * Based off discussion here:
    * https://stackoverflow.com/a/14534126/59015
    */
   fun fastUuid() = UUID(random.nextLong(), random.nextLong()).toString()
}

