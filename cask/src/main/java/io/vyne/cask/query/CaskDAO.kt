package io.vyne.cask.query

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.ddl.INSERTED_AT_COLUM_NAME
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.timed
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
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

   fun findAll(versionedType: VersionedType): List<Map<String, Any>> {
      val name = "${versionedType.versionedName}.findAll"
      return timed(name) {
         doForAllTablesOfType(versionedType) { tableName ->
            jdbcTemplate.queryForList(findAllQuery(tableName))
         }
      }
   }

   /**
    * Finds all the tables that are present for a specific versioned type, executes the code block
    * on each of them, and collates the responses.
    *
    * Note this is CLEARLY not the right way to do this long term.
    * Tried but discounted approaches:
    *  - UNION queries across the tables (requires tables to have the same set of columns)
    *  - Full outer join across the tables (columns with the same name result in only one of the columns being returned)
    *
    *  Solutions to the above are possible, but require EITHER a dbtrip per table to determine the
    *  column names, OR recompiling the schema for each versioned type, and attempting to recreate the
    *  column name.  However, I don't have the time to implement either of these approaches right now, and
    *  it's questionable how much more performant they'd be.
    */
   private fun doForAllTablesOfType(versionedType: VersionedType, function: (tableName: String) -> List<Map<String, Any>>): List<Map<String, Any>> {
      val tableNames = findTablesForType(versionedType)
      val results = tableNames.map { tableName -> function(tableName) }
      return mergeResultSets(results)
   }

   private fun mergeResultSets(results: List<List<Map<String, Any>>>): List<Map<String, Any>> {
      val allRecords = results.flatten()
      // Note: Originally the plan was to inject null values in the sets for fields that aren't
      // present in that record (because of the union of columns across multiple tables).
      // However, turns out, the absense of the key is probably sufficient.
      return allRecords
   }

   fun findBy(versionedType: VersionedType, columnName: String, arg: String): List<Map<String, Any>> {
      val name = "${versionedType.versionedName}.findBy${columnName}"
      return timed(name) {
         doForAllTablesOfType(versionedType) { tableName ->
            val originalTypeSchema = schemaProvider.schema()
            val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
            val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
            val findByArg = jdbcQueryArgumentType(fieldType, arg)
            jdbcTemplate.queryForList(findByQuery(tableName, columnName), findByArg)
         }
      }
   }

   fun findOne(versionedType: VersionedType, columnName: String, arg: String): Map<String, Any>? {
      return timed("${versionedType.versionedName}.findOne${columnName}") {
         val results = doForAllTablesOfType(versionedType) { tableName ->
            val originalTypeSchema = schemaProvider.schema()
            val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
            val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
            val findOneArg = jdbcQueryArgumentType(fieldType, arg)
            jdbcTemplate.queryForList(findByQuery(tableName, columnName), findOneArg)
         }
         if (results.size > 1) {
            log().error("Call to findOne() returned ${results.size} results.  Will pick the first")
         }
         results.firstOrNull() ?: emptyMap()
      }
   }

   fun findMultiple(versionedType: VersionedType, columnName: String, arg: List<String>): List<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findMultiple${columnName}") {
         doForAllTablesOfType(versionedType) { tableName ->
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
   }

   fun findBetween(versionedType: VersionedType, columnName: String, start: String, end: String): List<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.between") {
         val field = fieldForColumnName(versionedType, columnName)
         doForAllTablesOfType(versionedType) { tableName ->
            jdbcTemplate.queryForList(
               findBetweenQuery(tableName, columnName),
               jdbcQueryArgumentType(field, start),
               jdbcQueryArgumentType(field, end))
         }
      }
   }

   private fun findTablesForType(versionedType: VersionedType): List<String> {
      return jdbcTemplate.queryForList<String>(
         "SELECT tablename from cask_config where qualifiedtypename = ?",
         listOf(versionedType.fullyQualifiedName).toTypedArray(),
         String::class.java
      )
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
      PrimitiveType.INTEGER -> arg.toBigDecimal()
      PrimitiveType.BOOLEAN -> arg.toBoolean()
      PrimitiveType.LOCAL_DATE -> arg.toLocalDate()
      // TODO TIME db column type
      //PrimitiveType.TIME -> arg.toTime()
      PrimitiveType.INSTANT -> arg.toLocalDateTime()

      else -> TODO("type ${field.name} not yet mapped")
   }

   companion object {
      // TODO change to prepared statement, unlikely but potential for SQL injection via columnName
      fun findAllQuery(tableName: String) = """SELECT * FROM $tableName"""
      fun findInQuery(tableName: String, columnName: String, inPhrase: String) = """SELECT * FROM $tableName WHERE "$columnName" IN ($inPhrase)"""
      fun findByQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" = ?"""
      fun findBetweenQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" >= ? AND "$columnName" < ?"""
      fun findAfterQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" > ?"""
      fun findBeforeQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" < ?"""

      internal fun selectTableList(tableNames: List<String>): String {
         val indexTables = tableNames.mapIndexed { index, name -> "$name t$index" }
         if (indexTables.size == 1) {
            return indexTables[0]
         }
         return indexTables[0] + " " +
            indexTables.drop(1).joinToString(separator = " ") { "full outer join $it on 0 = 1" }
      }
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

   data class CaskMessage(
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

         val exists = exists(tableName)

         if (exists) {
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
               setInt(7, 30)
               if (deltaAgainstTableName != null) setString(8, deltaAgainstTableName) else setNull(8, Types.VARCHAR)
            }
         }
      }
   }

   fun countCasks(tableName: String): Int {
      return jdbcTemplate.queryForObject("SELECT COUNT(*) from CASK_CONFIG WHERE tableName=?", arrayOf(tableName), Int::class.java)
   }

   fun exists(tableName: String) = countCasks(tableName) > 0

   // ############################
   // ### COUNT Cask
   // ############################
   fun countCaskRecords(tableName: String): Int {
      return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $tableName", Int::class.java)
   }

   // ############################
   // ### DELETE/EMPTY Cask
   // ############################

   fun deleteCask(tableName: String) {
      jdbcTemplate.update("DELETE FROM CASK_CONFIG WHERE tableName=?", tableName)
      jdbcTemplate.update("DROP TABLE ${tableName}")
   }

   fun emptyCask(tableName: String) {
      jdbcTemplate.update("TRUNCATE ${tableName}")
   }

   fun evict(tableName: String, writtenBefore: Instant) {
      log().info("Evicting records for table=$tableName, older than $writtenBefore")
      jdbcTemplate.update { connection ->
         connection.prepareStatement("DELETE from $tableName WHERE $INSERTED_AT_COLUM_NAME < ?").apply {
            setTimestamp(1, Timestamp.from(writtenBefore))
         }
      }
   }

   fun setEvictionSchedule(tableName: String, daysToRetain: Int) {
      jdbcTemplate.update { connection ->
         connection.prepareStatement("UPDATE CASK_CONFIG SET daysToRetain= ? WHERE tableName= $tableName ").apply {
            setInt(1, daysToRetain)
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
        | daysToRetain,
        | deltaAgainstTableName)
        | values ( ? , ? , ?, ?, ?, ?, ?, ?)""".trimMargin()


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
