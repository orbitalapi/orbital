package io.vyne.schemas

fun String.synonymFullyQualifiedName() = split(".").dropLast(1).joinToString(".")
fun String.synonymValue() = split(".").last()

fun String.toSynonymTypeAndValue():Pair<String,String> {
   return this.synonymFullyQualifiedName() to this.synonymValue()
}

data class EnumValue(val name: String, val value: Any,  val synonyms: List<String>, val typeDoc: String?) {
   override fun toString(): String {
      return "$name($value)"
   }
}
