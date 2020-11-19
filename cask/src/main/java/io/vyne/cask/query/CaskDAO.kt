package io.vyne.cask.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.api.ContentType
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.CaskQueryOptions
import io.vyne.cask.config.FindOneMatchesManyBehaviour
import io.vyne.cask.config.QueryMatchesNoneBehaviour
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.ddl.views.CaskViewBuilder.Companion.VIEW_PREFIX
import io.vyne.cask.ingest.CaskMessage
import io.vyne.cask.ingest.CaskMessageRepository
import io.vyne.cask.query.generators.BetweenVariant
import io.vyne.cask.query.generators.FindBetweenInsertedAtOperationGenerator
import io.vyne.cask.timed
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import org.apache.commons.io.IOUtils
import org.postgresql.PGConnection
import org.postgresql.largeobject.LargeObjectManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import java.io.InputStream
import java.sql.Connection
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
   private val largeObjectDataSource: DataSource,
   private val caskMessageRepository: CaskMessageRepository,
   private val caskConfigRepository: CaskConfigRepository,
   private val objectMapper: ObjectMapper = jacksonObjectMapper(),
   private val queryOptions:CaskQueryOptions = CaskQueryOptions()
) {
   val postgresDdlGenerator = PostgresDdlGenerator()
   init {
      log().info("Cask running with query options: \n$queryOptions")
   }

   fun findAll(versionedType: VersionedType): List<Map<String, Any>> {
      val name = "${versionedType.versionedName}.findAll"
      return timed(name) {
         doForAllTablesOfType(versionedType) { tableName ->
            jdbcTemplate.queryForList(findAllQuery(tableName))
         }
      }
   }

   fun findAll(tableName: String): List<Map<String, Any>> {
      return jdbcTemplate.queryForList(findAllQuery(tableName))
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
            } catch (exception: Exception) {
               log().error("Failed to execute query", exception)
               throw exception
            }

         }
         when {
            results.isEmpty() -> {
               when (queryOptions.queryMatchesNoneBehaviour) {
                  QueryMatchesNoneBehaviour.RETURN_EMPTY -> emptyMap()
                  QueryMatchesNoneBehaviour.THROW_404 -> throw CaskQueryEmptyResultsException("Call to findOne on ${versionedType.fullyQualifiedName} by $columnName returned ${results.size} results.  Throwing a 404 exception.  You can configure this behaviour by setting cask.query-options.queryMatchesNoneBehaviour")
               }
            }
            results.size == 1 -> {
               results.first()
            }
            else -> { // results.size > 1
               when (queryOptions.findOneMatchesManyBehaviour) {
                  FindOneMatchesManyBehaviour.RETURN_FIRST -> {
                     log().warn("Call to findOne on ${versionedType.fullyQualifiedName} by $columnName returned ${results.size} results. pickFirstFromFindOne is configured to return first, so selecting the first record")
                     results.first()
                  }
                  FindOneMatchesManyBehaviour.THROW_ERROR -> {
                     throw CaskBadRequestException("Call to findOne on ${versionedType.fullyQualifiedName} by $columnName returned ${results.size} results.  This is not permitted.  You can configure this behaviour by setting cask.query-options.findOneMatchesManyBehaviour")
                  }
               }
            }
         }
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

   fun findBetween(versionedType: VersionedType,
                   columnName: String,
                   start: String,
                   end: String,
                   variant: BetweenVariant? = null): List<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.between") {
         if (FindBetweenInsertedAtOperationGenerator.fieldName == columnName) {
            doForAllTablesOfType(versionedType) { tableName ->
               jdbcTemplate.queryForList(
                  betweenQueryForCaskInsertedAt(tableName, variant),
                  jdbcQueryArgumentType(PrimitiveType.INSTANT, start),
                  jdbcQueryArgumentType(PrimitiveType.INSTANT, end))
            }
         } else {
            val field = fieldForColumnName(versionedType, columnName)
            doForAllTablesOfType(versionedType) { tableName ->
               jdbcTemplate.queryForList(
                  betweenQueryForField(tableName, columnName, variant),
                  jdbcQueryArgumentType(field, start),
                  jdbcQueryArgumentType(field, end))
            }
         }
      }
   }

   private fun betweenQueryForField(tableName: String, columnName: String, variant: BetweenVariant? = null) = when(variant) {
      BetweenVariant.GtLt -> findBetweenQueryGtLt(tableName, columnName)
      BetweenVariant.GtLte -> findBetweenQueryGtLte(tableName, columnName)
      BetweenVariant.GteLte -> findBetweenQueryGteLte(tableName, columnName)
      else -> findBetweenQuery(tableName, columnName)
   }

   private fun betweenQueryForCaskInsertedAt(tableName: String, variant: BetweenVariant? = null) = when(variant) {
      BetweenVariant.GtLt -> findBetweenGtLtCaskInsertedAtQuery(tableName)
      BetweenVariant.GtLte -> findBetweenGtLteCaskInsertedAtQuery(tableName)
      BetweenVariant.GteLte -> findBetweenGteLteCaskInsertedAtQuery(tableName)
      else -> findBetweenCaskInsertedAtQuery(tableName)
   }

   fun findTableNamesForType(qualifiedName: QualifiedName): List<String> {
      return caskConfigRepository.findAllByQualifiedTypeNameAndStatus(qualifiedName.fullyQualifiedName, CaskStatus.ACTIVE)
         .map { it.tableName }
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

   private fun jdbcQueryArgumentType(field: Field, arg: String) = jdbcQueryArgumentType (field.type.basePrimitive, arg)

   private fun jdbcQueryArgumentType(primitiveType: PrimitiveType?, arg: String) = when (primitiveType) {
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

      else -> TODO("type ${primitiveType?.name} not yet mapped")
   }

   companion object {
      // TODO change to prepared statement, unlikely but potential for SQL injection via columnName
      fun findAllQuery(tableName: String) = """SELECT * FROM $tableName"""
      fun findInQuery(tableName: String, columnName: String, inPhrase: String) = """SELECT * FROM $tableName WHERE "$columnName" IN ($inPhrase)"""
      fun findByQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" = ?"""
      fun findBetweenQuery(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" >= ? AND "$columnName" < ?"""
      fun findBetweenQueryGtLt(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" > ? AND "$columnName" < ?"""
      fun findBetweenQueryGtLte(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" > ? AND "$columnName" <= ?"""
      fun findBetweenQueryGteLte(tableName: String, columnName: String) = """SELECT * FROM $tableName WHERE "$columnName" >= ? AND "$columnName" <= ?"""

      fun findBetweenCaskInsertedAtQuery(tableName: String) = """SELECT caskTable.*, message.${CaskMessage.INSERTED_AT_COLUMN} as "caskInsertedAt" FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} >= ? AND message.${CaskMessage.INSERTED_AT_COLUMN} < ?"""
      fun findBetweenGtLtCaskInsertedAtQuery(tableName: String) = """SELECT caskTable.*, message.${CaskMessage.INSERTED_AT_COLUMN} as "caskInsertedAt" FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} > ? AND message.${CaskMessage.INSERTED_AT_COLUMN} < ?"""
      fun findBetweenGtLteCaskInsertedAtQuery(tableName: String) = """SELECT caskTable.*, message.${CaskMessage.INSERTED_AT_COLUMN} as "caskInsertedAt" FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} > ? AND message.${CaskMessage.INSERTED_AT_COLUMN} <= ?"""
      fun findBetweenGteLteCaskInsertedAtQuery(tableName: String) = """SELECT caskTable.*, message.${CaskMessage.INSERTED_AT_COLUMN} as "caskInsertedAt" FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} >= ? AND message.${CaskMessage.INSERTED_AT_COLUMN} <= ?"""

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
         val caskTable = postgresDdlGenerator.generateDdl(versionedType, schemaProvider.schema())
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

   fun getCaskMessages(messageIds: List<String>): List<CaskMessage> {
      return caskMessageRepository.findAllById(messageIds)
   }

   fun createCaskMessage(versionedType: VersionedType, id: String, input: Flux<InputStream>, contentType: ContentType, parameters: Any): CaskMessage {
      largeObjectDataSource.connection.use { connection ->
         connection.autoCommit = false
         return timed("CaskDao.createCaskMessage", true, TimeUnit.MILLISECONDS) {
            val type = schemaProvider.schema().toTaxiType(versionedType)
            val qualifiedTypeName = type.qualifiedName
            val insertedAt = Instant.now()

            val messageObjectId = persistMessageAsLargeObject(connection, input)

            val parametersJson = objectMapper.writeValueAsString(parameters)

            val caskMessage = caskMessageRepository.save(CaskMessage(id, qualifiedTypeName, messageObjectId, insertedAt, contentType, parametersJson))
            connection.commit()
            caskMessage
         }
      }
   }

   fun fetchRawCaskMessage(caskMessageId: String): Pair<ByteArray, ContentType?>? {
     return caskMessageRepository.findByIdOrNull(caskMessageId)?.let { caskMessage ->
        caskMessage.messageContentId?.let { largeObjectId ->
           largeObjectDataSource.connection.use { connection ->
              connection.autoCommit = false
              val pgConn = connection.unwrap(PGConnection::class.java)
              val largeObjectManager = pgConn.largeObjectAPI
              val largeObject = largeObjectManager.open(largeObjectId, LargeObjectManager.READ)
              IOUtils.toByteArray(largeObject.inputStream) to caskMessage.messageContentType
           }
        }
      }
   }


   fun getMessageContent(largeObjectId: Long): Flux<InputStream> {
      return Flux.create<InputStream> { emitter ->
         largeObjectDataSource.connection.use { connection ->
            connection.autoCommit = false
            val pgConn = connection.unwrap(PGConnection::class.java)
            val largeObjectManager = pgConn.largeObjectAPI
            val largeObject = largeObjectManager.open(largeObjectId)
            emitter.next(largeObject.inputStream)
            emitter.complete()
         }
      }
   }

   private fun persistMessageAsLargeObject(conn: Connection, input: Flux<InputStream>): Long? {
      val pgConn = conn.unwrap(PGConnection::class.java)
      val largeObjectManager = pgConn.largeObjectAPI
      val objectId = largeObjectManager.createLO(LargeObjectManager.READWRITE)
      Flux.from(input).subscribe { inputStream ->
         try {
            val largeObject = largeObjectManager.open(objectId, LargeObjectManager.WRITE)
            log().info("Streaming message contents to LargeObject API")
            IOUtils.copy(inputStream, largeObject.outputStream)
            log().info("Streaming message contents to LargeObject API completed")
            largeObject.close()
            conn.commit()
         } catch (exception: Exception) {
            log().error("Exception thrown whilst streaming message content to db", exception)
         }
      }
      return objectId

   }

   // ############################
   // ### ADD Cask Config
   // ############################

   fun findMessageIdsToReplay(sourceTableName: String, targetTableName: String): List<String> {
      return jdbcTemplate.queryForList("""select distinct ${PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME}
         |from $sourceTableName where ${PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME} not in (
         |  select distinct ${PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME} from $targetTableName
         |)""".trimMargin(), String::class.java)
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
   fun deleteCask(tableName: String, shouldCascade: Boolean = false) {
      log().info("Removing Cask with underlying table / view $tableName")
      jdbcTemplate.update("DELETE FROM CASK_CONFIG WHERE tableName=?", tableName)
      if (tableName.startsWith(VIEW_PREFIX)) {
         jdbcTemplate.update("DROP VIEW $tableName")
      } else {
         val dropStatement = if (shouldCascade) {
            "DROP TABLE $tableName CASCADE"
         } else {
            "DROP TABLE $tableName"
         }
         jdbcTemplate.update(dropStatement)
      }
   }

   fun emptyCask(tableName: String) {
      jdbcTemplate.update("TRUNCATE ${tableName}")
   }


}
