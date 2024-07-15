package com.orbitalhq.connectors.jdbc.sql.ddl

import lang.taxi.types.ArrayType
import lang.taxi.types.EnumType
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.TypeAlias
import mu.KotlinLogging
import org.jooq.DataType
import org.jooq.impl.SQLDataType

object TaxiTypeToJooqType {
   private val logger = KotlinLogging.logger {}
   const val PkSuffix = "-pk"
   fun getSqlType(type: lang.taxi.types.Type, nullable: Boolean): DataType<out Any> {
      return getSqlType(type).nullable(nullable)
   }

   fun getSqlType(type: lang.taxi.types.Type): DataType<out Any> {
      return when {
         type is TypeAlias && type.inheritsFromPrimitive -> getSqlType(type.basePrimitive!!)
         type is PrimitiveType -> getSqlType(type)
         type is ArrayType -> SQLDataType.OTHER.arrayDataType
         type is EnumType -> SQLDataType.VARCHAR // TODO : Generate enum types
         type is ObjectType && type.fields.isNotEmpty() -> error("Only scalar types are supported.  ${type.qualifiedName} defines fields")
         type is ObjectType -> {
            require(type.basePrimitive != null) { "Type ${type.qualifiedName} is scalar, but does not have a primitive type.  This is unexpected" }
            getSqlType(type.basePrimitive!!)
         }

         else -> error("Add support for Taxi type ${type::class.simpleName}")
      }
   }

   private fun getSqlType(type: PrimitiveType): DataType<out Any> {
      return if (taxiToJooq.containsKey(type.basePrimitive)) {
         taxiToJooq[type.basePrimitive]!!
      } else {
         logger.warn { "No mapping defined between primitive type ${type.basePrimitive!!.name} and a sql type.  There should be - defaulting to VARCHAR" }
         SQLDataType.VARCHAR
      }
   }

   private val taxiToJooq = mapOf(
      PrimitiveType.ANY to SQLDataType.VARCHAR,
      PrimitiveType.BOOLEAN to SQLDataType.BOOLEAN,
      PrimitiveType.DATE_TIME to SQLDataType.OFFSETDATETIME,
      PrimitiveType.DECIMAL to SQLDataType.DECIMAL,
      PrimitiveType.DOUBLE to SQLDataType.DOUBLE,
      PrimitiveType.INTEGER to SQLDataType.INTEGER,
      PrimitiveType.INSTANT to SQLDataType.INSTANT,
      PrimitiveType.STRING to SQLDataType.VARCHAR,
      PrimitiveType.TIME to SQLDataType.TIME,
      PrimitiveType.LOCAL_DATE to SQLDataType.LOCALDATE,
      PrimitiveType.LONG to SQLDataType.BIGINT
   )

}
