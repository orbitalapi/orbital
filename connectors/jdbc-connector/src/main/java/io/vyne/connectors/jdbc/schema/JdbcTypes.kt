package io.vyne.connectors.jdbc.schema

import lang.taxi.types.ArrayType
import lang.taxi.types.CompilationUnit
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import java.sql.Array
import java.sql.Date
import java.sql.Timestamp

object JdbcTypes {
   private val types = mapOf<String,Type>(
      ByteArray::class.java.canonicalName to PrimitiveType.ANY,
      Timestamp::class.java.canonicalName to PrimitiveType.INSTANT,
      Date::class.java.canonicalName to PrimitiveType.LOCAL_DATE,
      Array::class.java.canonicalName to ArrayType(PrimitiveType.STRING, CompilationUnit.unspecified())
   ) + mapOf(
      "java.lang.Object" to PrimitiveType.ANY
   )
   fun contains(type:Class<*>):Boolean = types.containsKey(type.canonicalName)
   fun get(type:Class<*>):Type = types.getValue(type.canonicalName)

   fun isArray(type:Class<*>) = type == Array::class.java
}
