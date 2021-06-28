package io.vyne.cask.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.api.ContentType
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.CaskQueryOptions
import io.vyne.cask.config.FindOneMatchesManyBehaviour
import io.vyne.cask.config.JdbcStreamingTemplate
import io.vyne.cask.config.QueryMatchesNoneBehaviour
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.cask.ddl.PostgresDdlGenerator.Companion.MESSAGE_ID_COLUMN_NAME
import io.vyne.cask.ddl.caskRecordTable
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
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import javax.sql.DataSource


fun String.toLocalDate(): LocalDate {
   return LocalDate.parse(this)
}

fun String.toLocalDateTime(): LocalDateTime {
   return ZonedDateTime.parse(this, CaskDAO.DATE_TIME_FORMATTER)
      .withZoneSameInstant(ZoneOffset.UTC)
      .toLocalDateTime()
}


@Component
class CaskDAO(
   private val jdbcTemplate: JdbcTemplate,
   private val jdbcStreamingTemplate: JdbcStreamingTemplate,
   private val schemaProvider: SchemaProvider,
   private val largeObjectDataSource: DataSource,
   private val caskMessageRepository: CaskMessageRepository,
   private val caskConfigRepository: CaskConfigRepository,
   private val objectMapper: ObjectMapper = jacksonObjectMapper(),
   private val queryOptions: CaskQueryOptions = CaskQueryOptions()
) {
   val postgresDdlGenerator = PostgresDdlGenerator()

   init {
      log().info("Cask running with query options: \n$queryOptions")
   }

   fun findAll(versionedType: VersionedType): Stream<Map<String, Any>> {
      val name = "${versionedType.versionedName}.findAll"
      return timed(name) {
         doForAllTablesOfType(versionedType) { tableName ->
            jdbcStreamingTemplate.queryForStream(findAllQuery(tableName))
         }
      }
   }

   fun findAll(tableName: String): Stream<Map<String, Any>> {
      return jdbcStreamingTemplate.queryForStream(findAllQuery(tableName))
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
   private fun doForAllTablesOfType(versionedType: VersionedType, function: (tableName: String) -> Stream<Map<String, Any>>): Stream<Map<String, Any>> {
      val tableNames = findTableNamesForType(versionedType)
      val results = tableNames.map { tableName -> function(tableName) }
      return mergeResultSets(results)
   }

   private fun doForAllTablesOfTypeSingle(versionedType: VersionedType, function: (tableName: String) -> List<Map<String, Any>>): List<Map<String, Any>> {
      val tableNames = findTableNamesForType(versionedType)
      val results = tableNames.map { tableName -> function(tableName) }
      return results.flatten()
   }

   private fun mergeResultSets(results: List<Stream<Map<String, Any>>>): Stream<Map<String, Any>> {

      return results[0]

         val allRecords = Stream.of(results[0])
         .reduce { stream1: Stream<out Map<String, Any>>, stream2: Stream<out Map<String, Any>> ->
            Stream.concat(
               stream1,
               stream2
            )
         }
         .orElseGet { Stream.empty() }

      return allRecords
   }

   fun findBy(versionedType: VersionedType, columnName: String, arg: String): Stream<Map<String, Any>> {
      val name = "${versionedType.versionedName}.findBy${columnName}"
      return timed(name) {
         doForAllTablesOfType(versionedType) { tableName ->

            val originalTypeSchema = schemaProvider.schema()
            val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
            val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
            val findByArg = castArgumentToJdbcType(fieldType, arg)
            jdbcStreamingTemplate.queryForStream(findByQuery(tableName, columnName), findByArg)
         }
      }
   }

   fun findOne(versionedType: VersionedType, columnName: String, arg: String): Map<String, Any>? {
      return timed("${versionedType.versionedName}.findOne${columnName}") {
         val results = doForAllTablesOfTypeSingle(versionedType) { tableName ->
            val originalTypeSchema = schemaProvider.schema()
            val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
            val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
            val findOneArg = castArgumentToJdbcType(fieldType, arg)
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

   fun findMultiple(versionedType: VersionedType, columnName: String, arg: List<String>): Stream<Map<String, Any>> {
      // Ignore the compiler -- filterNotNull() required here because we can receive a null inbound
      // in the Json. Jackson doesn't filter it out, and so casting errors can occur.
      val inputValues = arg.filterNotNull()
      return timed("${versionedType.versionedName}.findMultiple${columnName}") {
         doForAllTablesOfType(versionedType) { tableName ->
            val originalTypeSchema = schemaProvider.schema()
            val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
            val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
            val findMultipleArg = castArgumentsToJdbcType(fieldType, inputValues)

            val inPhrase = inputValues.joinToString(",") { "?" }
            val argTypes = inputValues.map { Types.VARCHAR }.toTypedArray().toIntArray()
            val argValues = findMultipleArg.toTypedArray()
            val retVal = jdbcStreamingTemplate.queryForStream(findInQuery(tableName, columnName, inPhrase), argValues, argTypes)
            retVal
         }
      }
   }

   fun findBetween(versionedType: VersionedType,
                   columnName: String,
                   start: String,
                   end: String,
                   variant: BetweenVariant? = null): Stream<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.between") {
         if (FindBetweenInsertedAtOperationGenerator.fieldName == columnName) {
            doForAllTablesOfType(versionedType) { tableName ->
               val query = betweenQueryForCaskInsertedAt(tableName, variant)
               val start = castArgumentToJdbcType(PrimitiveType.INSTANT, start)
               val end = castArgumentToJdbcType(PrimitiveType.INSTANT, end)
               log().info("issuing query => $query with start => $start and end => $end")
               jdbcStreamingTemplate.queryForStream(
                  query,
                  start,
                  end)
            }
         } else {
            val field = fieldForColumnName(versionedType, columnName)
            doForAllTablesOfType(versionedType) { tableName ->
               val query = betweenQueryForField(tableName, columnName, variant)
               val start = castArgumentToJdbcType(field, start)
               val end = castArgumentToJdbcType(field, end)
               log().info("issuing query => $query with start => $start and end => $end")
               jdbcStreamingTemplate.queryForStream(
                  query,
                  start,
                  end)
            }
         }
      }
   }

   private fun betweenQueryForField(tableName: String, columnName: String, variant: BetweenVariant? = null) = when (variant) {
      BetweenVariant.GtLt -> findBetweenQueryGtLt(tableName, columnName)
      BetweenVariant.GtLte -> findBetweenQueryGtLte(tableName, columnName)
      BetweenVariant.GteLte -> findBetweenQueryGteLte(tableName, columnName)
      else -> findBetweenQuery(tableName, columnName)
   }

   private fun betweenQueryForCaskInsertedAt(tableName: String, variant: BetweenVariant? = null) = when (variant) {
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

   fun findAfter(versionedType: VersionedType, columnName: String, after: String): Stream<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.after") {
         val field = fieldForColumnName(versionedType, columnName)
         doForAllTablesOfType(versionedType) { tableName ->
            jdbcStreamingTemplate.queryForStream(
               findAfterQuery(tableName, columnName),
               castArgumentToJdbcType(field, after))
         }
      }
   }

   fun findBefore(versionedType: VersionedType, columnName: String, before: String): Stream<Map<String, Any>> {
      return timed("${versionedType.versionedName}.findBy${columnName}.before") {
         val field = fieldForColumnName(versionedType, columnName)
         doForAllTablesOfType(versionedType) { tableName ->
            jdbcStreamingTemplate.queryForStream(
               findBeforeQuery(tableName, columnName),
               castArgumentToJdbcType(field, before))
         }
      }
   }

   private fun fieldForColumnName(versionedType: VersionedType, columnName: String): Field {
      val originalTypeSchema = schemaProvider.schema()
      val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
      return (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
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

      fun findBetweenCaskInsertedAtQuery(tableName: String) = """SELECT caskTable.${MESSAGE_ID_COLUMN_NAME} as "caskMessageId", caskTable.*, message.${CaskMessage.INSERTED_AT_COLUMN} as "caskInsertedAt" FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} >= ? AND message.${CaskMessage.INSERTED_AT_COLUMN} < ?"""
      fun findBetweenGtLtCaskInsertedAtQuery(tableName: String) = """SELECT caskTable.${MESSAGE_ID_COLUMN_NAME} as "caskRawMessageId", caskTable.*, message.${CaskMessage.INSERTED_AT_COLUMN} as "caskInsertedAt" FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} > ? AND message.${CaskMessage.INSERTED_AT_COLUMN} < ?"""
      fun findBetweenGtLteCaskInsertedAtQuery(tableName: String) = """SELECT caskTable.${MESSAGE_ID_COLUMN_NAME} as "caskRawMessageId", caskTable.*, message.${CaskMessage.INSERTED_AT_COLUMN} as "caskInsertedAt" FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} > ? AND message.${CaskMessage.INSERTED_AT_COLUMN} <= ?"""
      fun findBetweenGteLteCaskInsertedAtQuery(tableName: String) = """SELECT caskTable.${MESSAGE_ID_COLUMN_NAME} as "caskRawMessageId", caskTable.*, message.${CaskMessage.INSERTED_AT_COLUMN} as "caskInsertedAt" FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} >= ? AND message.${CaskMessage.INSERTED_AT_COLUMN} <= ?"""

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

      fun castArgumentsToJdbcType(field: Field, args: List<String>) = args.map { castArgumentToJdbcType(field, it) }

      fun castArgumentToJdbcType(field: Field, arg: String): Any {
         return field.type.basePrimitive?.let {
            castArgumentToJdbcType(it, arg)
         }
            ?: error("Field ${field.name} has a non-primitive type ${field.type.qualifiedName}.  Non-primitive types are not currently supported")

      }

      fun castArgumentToJdbcType(primitiveType: PrimitiveType, arg: String): Any = when (primitiveType) {
         PrimitiveType.STRING -> arg
         PrimitiveType.ANY -> arg
         PrimitiveType.DECIMAL -> arg.toBigDecimal()
         PrimitiveType.DOUBLE -> arg.toBigDecimal()
         PrimitiveType.INTEGER -> arg.toInt()
         PrimitiveType.BOOLEAN -> arg.toBoolean()
         PrimitiveType.LOCAL_DATE -> arg.toLocalDate()
         // TODO TIME db column type
         //PrimitiveType.TIME -> arg.toTime()
         PrimitiveType.INSTANT -> arg.toLocalDateTime()

         else -> TODO("type ${primitiveType.name} not yet mapped")
      }

      val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
         .parseLenient()
         .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
         .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 4, true)
         .appendPattern("[XXX]")
         .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0).parseDefaulting(ChronoField.OFFSET_SECONDS, ZoneOffset.UTC.totalSeconds.toLong()).toFormatter()
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
            // Don't use spring repository here as:
            // 1. it opens up a new connection
            // 2. large object insertion and corresponding cask_message table insertion must be transactional.
            ////val caskMessage = caskMessageRepository.save(CaskMessage(id, qualifiedTypeName, messageObjectId, insertedAt, contentType, parametersJson))
            val caskMessageInsertInto = connection.prepareStatement(CaskMessage.insertInto)
            caskMessageInsertInto.setString(1, id)
            caskMessageInsertInto.setString(2, qualifiedTypeName)
            caskMessageInsertInto.setLong(3, messageObjectId)
            caskMessageInsertInto.setTimestamp(4, Timestamp.from(insertedAt))
            caskMessageInsertInto.setString(5, contentType.name)
            caskMessageInsertInto.setString(6, parametersJson)
            caskMessageInsertInto.executeUpdate()
            try {
               connection.commit()
            } catch (e: Exception) {
               log().error("Error in creating cask message", e)
               connection.rollback()
            }
            CaskMessage(id, qualifiedTypeName, messageObjectId, insertedAt, contentType, parametersJson)
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

   private fun persistMessageAsLargeObject(conn: Connection, input: Flux<InputStream>): Long {
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
      return jdbcTemplate.queryForList("""select distinct $MESSAGE_ID_COLUMN_NAME
         |from $sourceTableName where $MESSAGE_ID_COLUMN_NAME not in (
         |  select distinct $MESSAGE_ID_COLUMN_NAME from $targetTableName
         |)""".trimMargin(), String::class.java)
   }


   @Deprecated("Move to CaskConfigRepository")
   fun countCasks(tableName: String): Int {
      return jdbcTemplate.queryForObject("SELECT COUNT(*) from CASK_CONFIG WHERE tableName=?", arrayOf(tableName), Int::class.java)
   }

   // ############################
   // ### COUNT Cask
   // ############################
   fun countCaskRecords(tableName: String): Int {
      return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $tableName", Int::class.java)
   }


   /**
    * Deletes the the given cask(s) and returns QualifiedNames for deleted casks underlying types.
    * @param Cask config for the cask to be deleted.
    * @param shouldCascade When true, all other casks specified in dependencies will also be deleted.
    * @param dependencies List of other casks to be deleted when shouldCascade is set to true.
    * @return List of fully qualified type names corresponding to deleted casks.
    */
   @Transactional
   fun deleteCask(caskConfig: CaskConfig, shouldCascade: Boolean = false, dependencies: List<CaskConfig> = emptyList()): List<QualifiedName> {
      val typesForDeletedCasks = mutableListOf<QualifiedName>()
      val tableName = caskConfig.tableName
      log().info("Removing Cask with configuration: $caskConfig")
      typesForDeletedCasks.add(QualifiedName.from(caskConfig.qualifiedTypeName))
      jdbcTemplate.update("DELETE FROM CASK_CONFIG WHERE tableName=?", tableName)
      if (shouldCascade) {
         dependencies.forEach { dependentCaskConfig ->
            log().info("Removing Cask with configuration: $dependentCaskConfig")
            jdbcTemplate.update("DELETE FROM CASK_CONFIG WHERE tableName=?", dependentCaskConfig.tableName)
            typesForDeletedCasks.add(QualifiedName.from(dependentCaskConfig.qualifiedTypeName))
         }
      }
      if (caskConfig.exposesType) {
         jdbcTemplate.update("DROP VIEW $tableName")
      } else {
         val dropStatement = if (shouldCascade) {
            "DROP TABLE $tableName CASCADE"
         } else {
            "DROP TABLE $tableName"
         }
         log().info("Drop statement ${dropStatement}")
         jdbcTemplate.update(dropStatement + " CASCADE")
         log().info("Drop done")
      }

      log().info("Returning after deletion")

      return typesForDeletedCasks.toList()
   }

   fun emptyCask(tableName: String) {
      jdbcTemplate.update("TRUNCATE ${tableName}")
   }
}
