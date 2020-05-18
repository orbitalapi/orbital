package io.vyne.cask.query

import io.vyne.cask.ingest.QueryView
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun String.toLocalDate(): LocalDate {
   return LocalDate.parse(this)
}

fun String.toLocalDateTime(): LocalDateTime {
   return LocalDateTime.parse(this)
}

// TODO TIME db type
//fun String.toTime(): LocalDateTime {
//   // Postgres api does not accept LocalTime so have to map to LocalDateTime
//   return LocalDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.parse(this, DateTimeFormatter.ISO_TIME))
//}

@Component
class CaskDAO(private val jdbcTemplate: JdbcTemplate) {
   fun findBy(versionedType: VersionedType, columnName: String, arg: String): List<Map<String, Any>> {
      val existingMetadaList = QueryView.tableMetadataForVersionedType(versionedType, jdbcTemplate)
      val exactVersionMatch = existingMetadaList.firstOrNull { it.versionHash == versionedType.versionHash }
         ?: existingMetadaList.maxBy { it.timestamp } ?: throw IllegalArgumentException(versionedType.fullyQualifiedName)
      val tableName = exactVersionMatch.tableName
      val originalTypeSchema = exactVersionMatch.schema
      val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
      val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
      val findByArg = jdbcQueryArgumentType(fieldType, arg)
      return jdbcTemplate.queryForList(findByQuery(tableName, columnName), findByArg)
   }

   private fun jdbcQueryArgumentType(field: Field, arg: String) = when (field.type.basePrimitive) {
      PrimitiveType.STRING -> arg
      PrimitiveType.ANY -> arg
      PrimitiveType.DECIMAL -> arg.toBigDecimal()
      PrimitiveType.DOUBLE -> arg.toBigDecimal()
      PrimitiveType.INTEGER ->arg.toBigDecimal()
      PrimitiveType.BOOLEAN -> arg.toBoolean()
      PrimitiveType.LOCAL_DATE -> arg.toLocalDate()
      // TODO TIME db column type
      //PrimitiveType.TIME -> arg.toTime()
      PrimitiveType.INSTANT -> arg.toLocalDateTime()

      else -> TODO("type ${field.name} not yet mapped")
   }

   companion object {
      fun findByQuery(tableName: String, columnName: String) = "SELECT * FROM $tableName WHERE $columnName = ?"
   }
}
