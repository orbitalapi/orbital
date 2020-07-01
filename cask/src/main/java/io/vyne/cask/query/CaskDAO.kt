package io.vyne.cask.query

import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.timed
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
import io.vyne.utils.orElse
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

fun String.toLocalDate(): LocalDate {
   return LocalDate.parse(this)
}

fun String.toLocalDateTime(): LocalDateTime {
   // Vyne is passing with the Zone information.
   return ZonedDateTime.parse(this).toLocalDateTime()
}

// TODO TIME db type
//fun String.toTime(): LocalDateTime {
//   // Postgres api does not accept LocalTime so have to map to LocalDateTime
//   return LocalDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.parse(this, DateTimeFormatter.ISO_TIME))
//}

@Component
class CaskDAO(private val jdbcTemplate: JdbcTemplate, private val schemaProvider: SchemaProvider) {
   val postgresDdlGenerator = PostgresDdlGenerator()

   fun findBy(versionedType: VersionedType, columnName: String, arg: String): List<Map<String, Any>> {
      val name = "${versionedType.versionedName}.findBy${columnName}"
      return timed(name) {
         val tableName = versionedType.caskRecordTable()
         val originalTypeSchema = schemaProvider.schema()
         val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
         val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
         val findByArg = jdbcQueryArgumentType(fieldType, arg)
         jdbcTemplate.queryForList(findByQuery(tableName, columnName), findByArg)
      }
   }

   fun findOne(versionedType: VersionedType, columnName: String, arg: String): Map<String, Any>? {
      return timed("${versionedType.versionedName}.findOne${columnName}") {
         val tableName = versionedType.caskRecordTable()
         val originalTypeSchema = schemaProvider.schema()
         val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
         val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
         val findOneArg = jdbcQueryArgumentType(fieldType, arg)
         jdbcTemplate.queryForList(findByQuery(tableName, columnName), findOneArg).firstOrNull().orElse(emptyMap())
      }
   }

   fun findMultiple(versionedType: VersionedType, columnName: String, arg: List<String>): List<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findMultiple${columnName}") {
         val tableName = versionedType.caskRecordTable()
         val originalTypeSchema = schemaProvider.schema()
         val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
         val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
         val findMultipleArg = jdbcQueryArgumentsType(fieldType, arg)
         val inPhrase = arg.joinToString(",") { "?" }
         val argTypes = arg.map { Types.VARCHAR }.toTypedArray().toIntArray()
         val argValues = findMultipleArg.toTypedArray()
         val retVal = jdbcTemplate.queryForList(findInQuery(tableName, columnName, inPhrase), argValues, argTypes)
         retVal
      }
   }

   fun findBetween(versionedType: VersionedType, columnName: String, start: String, end: String): List<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.between") {
         val field = fieldForColumnName(versionedType, columnName)
         jdbcTemplate.queryForList(
            findBetweenQuery(versionedType.caskRecordTable(), columnName),
            jdbcQueryArgumentType(field, start),
            jdbcQueryArgumentType(field, end))
      }
   }

   fun findAfter(versionedType: VersionedType, columnName: String, after: String): List<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.after") {
         val field = fieldForColumnName(versionedType, columnName)
         jdbcTemplate.queryForList(
            findAfterQuery(versionedType.caskRecordTable(), columnName),
            jdbcQueryArgumentType(field, after))
      }
   }

   fun findBefore(versionedType: VersionedType, columnName: String, before: String): List<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.before") {
         val field = fieldForColumnName(versionedType, columnName)
         jdbcTemplate.queryForList(
            findBeforeQuery(versionedType.caskRecordTable(), columnName),
            jdbcQueryArgumentType(field, before))
      }
   }

   private fun fieldForColumnName(versionedType: VersionedType, columnName: String): Field {
      val originalTypeSchema = schemaProvider.schema()
      val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
      return (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
   }

   private fun jdbcQueryArgumentsType(field: Field, args: List<String>) = args.map { jdbcQueryArgumentType(field, it) }

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
      // TODO change to prepared statement, unlikely but potential for SQL injection via columnName
      fun findInQuery(tableName: String, columnName: String, inPhrase: String) = """SELECT * FROM $tableName WHERE "$columnName" IN ($inPhrase)"""
      fun findByQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" = ?"""
      fun findBetweenQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" >= ? AND "$columnName" < ?"""
      fun findAfterQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" > ?"""
      fun findBeforeQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" < ?"""
   }

   // ############################
   // ### ADD Add Cask Table
   // ############################

   fun createCaskRecordTable(versionedType: VersionedType): String {
      return timed("CaskDao.createCaskRecordTable", true, TimeUnit.MICROSECONDS) {
         val caskTable = postgresDdlGenerator.generateDdl(versionedType, schemaProvider.schema(), null, null)
         jdbcTemplate.execute(caskTable.ddlStatement)
         log().info("CaskRecord table=${caskTable.generatedTableName} created")
         caskTable.generatedTableName
      }
   }

   fun dropCaskRecordTable(versionedType: VersionedType) {
      timed("CaskDao.dropCaskRecordTable", true, TimeUnit.MICROSECONDS) {
         val tableName = versionedType.caskRecordTable()
         val dropCaskTableDdl = postgresDdlGenerator.generateDrop(versionedType)
         jdbcTemplate.execute(dropCaskTableDdl)
         log().info("CaskRecord table=$tableName dropped")
      }
   }

   // ############################
   // ### ADD Cask Message
   // ############################

   fun findAllCaskMessages(): MutableList<CaskMessage> {
      return jdbcTemplate.query("Select * from CASK_MESSAGE", caskMessageRowMapper)
   }

   fun createCaskMessage(versionedType: VersionedType, readCachePath: Path, id: String): CaskMessage {
      return timed("CaskDao.createCaskMessage", true, TimeUnit.MILLISECONDS) {
         val type = schemaProvider.schema().toTaxiType(versionedType)
         val qualifiedTypeName = type.qualifiedName
         val insertedAt = Instant.now()
         jdbcTemplate.update { connection ->
            connection.prepareStatement(ADD_CASK_MESSAGE).apply {
               setString(1, id)
               setString(2, qualifiedTypeName)
               setString(3, readCachePath.toString())
               setTimestamp(4, Timestamp.from(insertedAt))
            }
         }
         CaskMessage(id, qualifiedTypeName, readCachePath.toString(), insertedAt)
      }
   }

   private val ADD_CASK_MESSAGE = """INSERT into CASK_MESSAGE (
         | id,
         | qualifiedTypeName,
         | readCachePath,
         | insertedAt) values ( ? , ?, ?, ? )""".trimMargin()

   data class CaskMessage (
      val id: String,
      val qualifiedTypeName: String,
      val readCachePath: String,
      val insertedAt: Instant
   )

   val caskMessageRowMapper: (ResultSet, Int) -> CaskMessage = { rs: ResultSet, rowNum: Int ->
      CaskMessage(
         rs.getString("id"),
         rs.getString("readCachePath"),
         rs.getString("qualifiedTypeName"),
         rs.getTimestamp("insertedAt").toInstant()
      )
   }

   // ############################
   // ### ADD Cask Config
   // ############################

   fun findAllCaskConfigs(): MutableList<CaskConfig> {
      return jdbcTemplate.query("Select * from CASK_CONFIG", caskConfigRowMapper)
   }

   fun createCaskConfig(versionedType: VersionedType, typeMigration: TypeMigration? = null) {
      timed("CaskDao.addCaskConfig", true, TimeUnit.MILLISECONDS) {
         val type = schemaProvider.schema().toTaxiType(versionedType)
         val deltaAgainstTableName = typeMigration?.predecessorType?.let { it.caskRecordTable() }
         val tableName = versionedType.caskRecordTable()
         val qualifiedTypeName = type.qualifiedName
         val versionHash = versionedType.versionHash
         val sourceSchemaIds = versionedType.sources.map { it.id }
         val sources = versionedType.sources.map { it.content }
         val timestamp = Instant.now()

         val count = jdbcTemplate.queryForObject("SELECT COUNT(*) from CASK_CONFIG WHERE tableName=?", arrayOf(tableName), Int::class.java)

         if (count > 0) {
            log().info("CaskConfig already exists for type=${versionedType.versionedName}, tableName=$tableName")
            return@timed
         }

         log().info("Creating CaskConfig for type=${versionedType.versionedName}, tableName=$tableName")
         jdbcTemplate.update { connection ->
            connection.prepareStatement(ADD_CASK_CONFIG).apply {
               setString(1, tableName)
               setString(2, qualifiedTypeName)
               setString(3, versionHash)
               setArray(4, connection.createArrayOf("text", sourceSchemaIds.toTypedArray()))
               setArray(5, connection.createArrayOf("text", sources.toTypedArray()))
               setTimestamp(6, Timestamp.from(timestamp))
               if (deltaAgainstTableName != null) setString(7, deltaAgainstTableName) else setNull(7, Types.VARCHAR)
            }
         }
      }
   }

   private val ADD_CASK_CONFIG = """INSERT into CASK_CONFIG (
        | tableName,
        | qualifiedTypeName,
        | versionHash,
        | sourceSchemaIds,
        | sources,
        | insertedAt,
        | deltaAgainstTableName)
        | values ( ? , ? , ?, ?, ?, ?, ?)""".trimMargin()

   data class CaskConfig(
      val tableName: String,
      val qualifiedTypeName: String,
      val versionHash: String,
      val sourceSchemaIds: List<String>,
      val sources: List<String>,
      val deltaAgainstTableName: String?,
      val insertedAt: Instant
   )

   private val caskConfigRowMapper: (ResultSet, Int) -> CaskConfig = { rs: ResultSet, rowNum: Int ->
      CaskConfig(
         rs.getString("tableName"),
         rs.getString("qualifiedTypeName"),
         rs.getString("versionHash"),
         listOf(*rs.getArray("sourceSchemaIds").array as Array<String>),
         listOf(*rs.getArray("sources").array as Array<String>),
         rs.getString("deltaAgainstTableName"),
         rs.getTimestamp("insertedAt").toInstant()
      )
   }


}
