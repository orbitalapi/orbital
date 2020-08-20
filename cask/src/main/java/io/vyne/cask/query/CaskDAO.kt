package io.vyne.cask.query

import com.zaxxer.hikari.HikariDataSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.ddl.views.CaskViewBuilder.Companion.ViewPrefix
import io.vyne.cask.ddl.*
import io.vyne.cask.timed
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import org.postgresql.PGConnection
import org.postgresql.largeobject.LargeObjectManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import java.io.InputStream
import java.nio.file.Path
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

fun String.toLocalDate(): LocalDate {
   return LocalDate.parse(this)
}

fun String.toLocalDateTime(): LocalDateTime {
   // Vyne is passing with the Zone information.
   return ZonedDateTime.parse(this).toLocalDateTime()
}

@Component
class CaskDAO(
   private val jdbcTemplate: JdbcTemplate,
   private val schemaProvider: SchemaProvider,
   dataSourceProps: DataSourceProperties
) {
   private val largeObjectDataSource: DataSource
   init {
      largeObjectDataSource = HikariDataSource()
      largeObjectDataSource.driverClassName = "org.postgresql.Driver"
      largeObjectDataSource.jdbcUrl = dataSourceProps.url
      largeObjectDataSource.username = dataSourceProps.username
      largeObjectDataSource.password = dataSourceProps.password
      largeObjectDataSource.isAutoCommit = false
      largeObjectDataSource.poolName = "LargeObject_CONNECTION_POOL"
   }
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
      val tableNames = findTableNamesForType(versionedType)
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
            try {
               jdbcTemplate.queryForList(findByQuery(tableName, columnName), findOneArg)
            } catch (exception:Exception) {
               log().error("Failed to execute query", exception)
               throw exception
            }

         }
         if (results.size > 1) {
            log().error("Call to findOne() returned ${results.size} results.  Will pick the first")
         }
         results.firstOrNull() ?: emptyMap()
      }
   }

   fun findMultiple(versionedType: VersionedType, columnName: String, arg: List<String>): List<Map<String, Any>> {
      // Ignore the compiler -- filterNotNull() required here because we can receive a null inbound
      // in the Json. Jackson doesn't filter it out, and so casting errors can occur.
      val inputValues = arg.filterNotNull()
      return timed("${versionedType.versionedName}.findMultiple${columnName}") {
         doForAllTablesOfType(versionedType) { tableName ->
            val originalTypeSchema = schemaProvider.schema()
            val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
            val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
            val findMultipleArg = jdbcQueryArgumentsType(fieldType, inputValues)

            val inPhrase = inputValues.joinToString(",") { "?" }
            val argTypes = inputValues.map { Types.VARCHAR }.toTypedArray().toIntArray()
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

   fun findTableNamesForType(qualifiedName:QualifiedName): List<String> {
      return jdbcTemplate.queryForList<String>(
         "SELECT tablename from cask_config where qualifiedtypename = ?",
         listOf(qualifiedName.toString()).toTypedArray(),
         String::class.java
      )
   }
   fun findTableNamesForType(versionedType: VersionedType): List<String> {
      return findTableNamesForType(versionedType.taxiType.toQualifiedName())
   }

   fun findAfter(versionedType: VersionedType, columnName: String, after: String): List<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.after") {
         val field = fieldForColumnName(versionedType, columnName)
         doForAllTablesOfType(versionedType) { tableName ->
            jdbcTemplate.queryForList(
               findAfterQuery(tableName, columnName),
               jdbcQueryArgumentType(field, after))
         }
      }
   }

   fun findBefore(versionedType: VersionedType, columnName: String, before: String): List<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.before") {
         val field = fieldForColumnName(versionedType, columnName)
         doForAllTablesOfType(versionedType) { tableName ->
            jdbcTemplate.queryForList(
               findBeforeQuery(tableName, columnName),
               jdbcQueryArgumentType(field, before))
         }
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

   fun createCaskMessage(versionedType: VersionedType, id: String, input: Flux<InputStream>): CaskMessage {
      val conn = largeObjectDataSource.connection
      try {
         return timed("CaskDao.createCaskMessage", true, TimeUnit.MILLISECONDS) {
            val type = schemaProvider.schema().toTaxiType(versionedType)
            val qualifiedTypeName = type.qualifiedName
            val insertedAt = Instant.now()

            // TODO Devrim cleanup
//         val conn = jdbcTemplate.getCustomConnection()
            //conn = largeObjectDataSource.connection
            requireNotNull(conn) { "Connection could not be obtained." }
            val pgConn = conn?.unwrap(PGConnection::class.java)
            requireNotNull(pgConn) { "Connection could not be obtained." }
            val lobj = pgConn.largeObjectAPI
            val oid = lobj.createLO(LargeObjectManager.READWRITE)
            val obj = lobj.open(oid, LargeObjectManager.WRITE)

            val chunkSize = 1024
            val buf = ByteArray(chunkSize)
            var stepSize = 0
            var totalSize = 0

            input.subscribe {
               stepSize = it.read(buf, 0, chunkSize)
               obj.write(buf, 0, stepSize)
               totalSize += stepSize
            }

            conn.prepareStatement(ADD_CASK_MESSAGE).apply {
               setString(1, id)
               setString(2, qualifiedTypeName)
               setLong(3, oid)
               setTimestamp(4, Timestamp.from(insertedAt))
               executeUpdate()
            }

            obj.close()
            conn.commit()

            CaskMessage(id, qualifiedTypeName, oid, insertedAt)
         }
      }
      finally {
         if(!conn.isClosed) {
            conn.close()
         }
      }
   }

   private val ADD_CASK_MESSAGE = """INSERT into CASK_MESSAGE (
         | id,
         | qualifiedTypeName,
         | messageId,
         | insertedAt) values ( ? , ?, ?, ? )""".trimMargin()

   data class CaskMessage(
      val id: String,
      val qualifiedTypeName: String,
      val messageId: Long,
      val insertedAt: Instant
   )

   val caskMessageRowMapper: (ResultSet, Int) -> CaskMessage = { rs: ResultSet, rowNum: Int ->
      CaskMessage(
         rs.getString("id"),
         rs.getString("qualifiedTypeName"),
         rs.getLong("messageId"),
         rs.getTimestamp("insertedAt").toInstant()
      )
   }

   // ############################
   // ### ADD Cask Config
   // ############################

   fun findAllCaskConfigs(): MutableList<CaskConfig> {
      return jdbcTemplate.query("Select * from CASK_CONFIG", caskConfigRowMapper)
   }

   fun createCaskConfig(versionedType: VersionedType, typeMigration: TypeMigration? = null, exposeType:Boolean = false, exposeService:Boolean = true) {
      // TODO : Migrate this to use CaskConfigRepository.
      timed("CaskDao.addCaskConfig", true, TimeUnit.MILLISECONDS) {
         val tableName = versionedType.caskRecordTable()
         val exists = exists(tableName)

         if (exists) {
            log().info("CaskConfig already exists for type=${versionedType.versionedName}, tableName=$tableName")
            return@timed
         }
         val config = CaskConfig.forType(
            type = versionedType,
            tableName = versionedType.caskRecordTable(),
            deltaAgainstTableName = typeMigration?.predecessorType?.let { it.caskRecordTable()  },
            exposesType = exposeType,
            exposesService = exposeService
         )

         log().info("Creating CaskConfig for type=${versionedType.versionedName}, tableName=$tableName")
         jdbcTemplate.update { connection ->
            connection.prepareStatement(ADD_CASK_CONFIG).apply {
               setString(1, config.tableName)
               setString(2, config.qualifiedTypeName)
               setString(3, config.versionHash)
               setArray(4, connection.createArrayOf("text", config.sourceSchemaIds.toTypedArray()))
               setArray(5, connection.createArrayOf("text", config.sources.toTypedArray()))
               setTimestamp(6, Timestamp.from(config.insertedAt))
               if (config.deltaAgainstTableName != null) setString(7, config.deltaAgainstTableName) else setNull(7, Types.VARCHAR)
               setBoolean(8, config.exposesService)
               setBoolean(9, config.exposesType)
            }
         }
      }
   }

   @Deprecated("Move to CaskConfigRepository")
   fun countCasks(tableName: String): Int {
      return jdbcTemplate.queryForObject("SELECT COUNT(*) from CASK_CONFIG WHERE tableName=?", arrayOf(tableName), Int::class.java)
   }

   @Deprecated("Move to CaskConfigRepository")
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

   @Transactional
   fun deleteCask(tableName: String) {
      log().info("Removing Cask with underlying table / view $tableName")
      jdbcTemplate.update("DELETE FROM CASK_CONFIG WHERE tableName=?", tableName)
      if (tableName.startsWith(ViewPrefix)) {
         jdbcTemplate.update("DROP VIEW $tableName")
      } else {
         jdbcTemplate.update("DROP TABLE $tableName")
      }
   }

   fun emptyCask(tableName: String) {
      jdbcTemplate.update("TRUNCATE ${tableName}")
   }

   private val ADD_CASK_CONFIG = """INSERT into CASK_CONFIG (
        | tableName,
        | qualifiedTypeName,
        | versionHash,
        | sourceSchemaIds,
        | sources,
        | insertedAt,
        | deltaAgainstTableName,
        | exposesService,
        | exposesType    )
        | values ( ? , ? , ?, ?, ?, ?, ?, ?, ?)""".trimMargin()


   private val caskConfigRowMapper: (ResultSet, Int) -> CaskConfig = { rs: ResultSet, rowNum: Int ->
      CaskConfig(
         rs.getString("tableName"),
         rs.getString("qualifiedTypeName"),
         rs.getString("versionHash"),
         listOf(*rs.getArray("sourceSchemaIds").array as Array<String>),
         listOf(*rs.getArray("sources").array as Array<String>),
         rs.getString("deltaAgainstTableName"),
         rs.getTimestamp("insertedAt").toInstant(),
         rs.getBoolean("exposesType"),
         rs.getBoolean("exposesService")
      )
   }


}
