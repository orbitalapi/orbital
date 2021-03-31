package io.vyne.schemas

fun String.synonymFullQualifiedName() = split(".").dropLast(1).joinToString(".")
fun String.synonymValue() = split(".").last()

fun String.toSynonymTypeAndValue():Pair<String,String> {
   return this.synonymFullQualifiedName() to this.synonymValue()
}

data class EnumValue(val name: String, val value: Any,  val synonyms: List<String>, val typeDoc: String?)
