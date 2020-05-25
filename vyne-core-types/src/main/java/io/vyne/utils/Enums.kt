package io.vyne.utils

fun String.synonymFullQualifiedName() = split(".").dropLast(1).joinToString(".")
fun String.synonymValue() = split(".").last()
