package com.orbitalhq.query

fun String.asFact(typeName: String): Fact {
   return Fact(typeName, this)
}

fun Int.asFact(typeName: String): Fact {
   return Fact(typeName, this)
}
