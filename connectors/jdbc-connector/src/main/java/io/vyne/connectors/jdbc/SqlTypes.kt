package io.vyne.connectors.jdbc

import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.PrimitiveType
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

object SqlTypes {
   fun getSqlType(primitiveType:PrimitiveType):Class<Any> {
      return PrimitiveTypes.getJavaType(primitiveType).let { javaType ->
         // Some types have "better" sql representations...
         when (javaType) {
            Instant::class.java -> OffsetDateTime::class.java
            else -> javaType
         }
      } as Class<Any>
   }

   fun convertToSqlValue(value: Any): Any {
      return when(value) {
         is Instant -> OffsetDateTime.ofInstant(value, ZoneOffset.UTC)
         else -> value
      }
   }
}
