package io.vyne.schemas

fun String.synonymFullQualifiedName() = split(".").dropLast(1).joinToString(".")
fun String.synonymValue() = split(".").last()

data class EnumValue(val name: String, val value: Any,  val synonyms: List<String>, val typeDoc: String?)
