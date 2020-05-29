package io.vyne.models.csv

import java.lang.Exception

class ParsingException(message: String, exception: Exception?) : RuntimeException(message, exception) {
   constructor(message: String) : this(message, null)
}
