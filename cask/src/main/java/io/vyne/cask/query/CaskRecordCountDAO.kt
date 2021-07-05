package io.vyne.cask.query

import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.JdbcStreamingTemplate
import io.vyne.cask.ddl.PostgresDdlGenerator.Companion.MESSAGE_ID_COLUMN_NAME
import io.vyne.cask.ingest.CaskMessage
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
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.sql.Types
private val logger = KotlinLogging.logger {}

@Component
class CaskRecordCountDAO(
   private val jdbcStreamingTemplate: JdbcStreamingTemplate,
   private val schemaProvider: SchemaProvider,
   private val caskConfigRepository: CaskConfigRepository,
) {


   fun findCountAll(versionedType: VersionedType): Int {
      val name = "${versionedType.versionedName}.findCountAll"
      val count = timed(name) {
         countForAllTablesOfType(versionedType) { tableName ->
            jdbcStreamingTemplate.queryForObject(findCountAllQuery(tableName), Int::class.java)
         }
      }
      logger.debug {"Record count for findCountAll : $count"}
      return count

   }

   fun findCountAll(tableName: String): Int {
      val count = jdbcStreamingTemplate.queryForObject(findCountAllQuery(tableName), Int::class.java)
      logger.debug {"Record count for findCountAll(${tableName}) : $count"}
      return count

   }

   private fun countForAllTablesOfType(versionedType: VersionedType, function: (tableName: String) -> Int ): Int {
      val tableNames = findTableNamesForType(versionedType)
      val results = tableNames.map { tableName -> function(tableName) }
      return countResultSets(results)
   }


   private fun countResultSets(results: List<Int>): Int {
      return results.sum()
   }

   fun findCountBy(versionedType: VersionedType, columnName: String, arg: String): Int {
      val name = "${versionedType.versionedName}.findCountBy${columnName}"
      return timed(name) {
         countForAllTablesOfType(versionedType) { tableName ->

            val originalTypeSchema = schemaProvider.schema()
            val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
            val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
            val findByArg = castArgumentToJdbcType(fieldType, arg)

            val count = jdbcStreamingTemplate.queryForObject(findCountByQuery(tableName, columnName), Int::class.java, findByArg)
            logger.debug {"Record count for findCountBy : $count"}
            count

         }
      }
   }

   fun findCountMultiple(versionedType: VersionedType, columnName: String, arg: List<String>): Int {
      // Ignore the compiler -- filterNotNull() required here because we can receive a null inbound
      // in the Json. Jackson doesn't filter it out, and so casting errors can occur.
      val inputValues = arg.filterNotNull()
      val count = timed("${versionedType.versionedName}.findCountMultiple${columnName}") {
         countForAllTablesOfType(versionedType) { tableName ->
            val originalTypeSchema = schemaProvider.schema()
            val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
            val fieldType = (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
            val findMultipleArg = castArgumentsToJdbcType(fieldType, inputValues)

            val inPhrase = inputValues.joinToString(",") { "?" }
            val argTypes = inputValues.map { Types.VARCHAR }.toTypedArray().toIntArray()
            val argValues = findMultipleArg.toTypedArray()

            jdbcStreamingTemplate.queryForObject(findCountInQuery(tableName, columnName, inPhrase), Int::class.java, argValues, argTypes)

         }
      }
      logger.debug {"Record count for findCountMultiple : $count"}
      return count
   }

   fun findCountBetween(versionedType: VersionedType,
                   columnName: String,
                   start: String,
                   end: String,
                   variant: BetweenVariant? = null): Int {
      val count = timed("${versionedType.versionedName}.findCountBetween${columnName}.between") {
         if (FindBetweenInsertedAtOperationGenerator.fieldName == columnName) {
            countForAllTablesOfType(versionedType) { tableName ->
               val query = betweenQueryForCaskInsertedAt(tableName, variant)
               val start = castArgumentToJdbcType(PrimitiveType.INSTANT, start)
               val end = castArgumentToJdbcType(PrimitiveType.INSTANT, end)
               log().info("issuing findCountBetween query => $query with start => $start and end => $end")
               jdbcStreamingTemplate.queryForObject(query, Int::class.java, start, end)
            }
         } else {
            val field = fieldForColumnName(versionedType, columnName)
            countForAllTablesOfType(versionedType) { tableName ->
               val query = betweenQueryForField(tableName, columnName, variant)
               val start = castArgumentToJdbcType(field, start)
               val end = castArgumentToJdbcType(field, end)
               log().info("issuing findCountBetween query => $query with start => $start and end => $end")

               jdbcStreamingTemplate.queryForObject(query, Int::class.java, start, end)

            }
         }
      }

      logger.debug {"Record count for findCountBetween : $count"}
      return count

   }

   private fun betweenQueryForField(tableName: String, columnName: String, variant: BetweenVariant? = null) = when (variant) {
      BetweenVariant.GtLt -> findCountBetweenQueryGtLt(tableName, columnName)
      BetweenVariant.GtLte -> findCountBetweenQueryGtLte(tableName, columnName)
      BetweenVariant.GteLte -> findCountBetweenQueryGteLte(tableName, columnName)
      else -> findCountBetweenQuery(tableName, columnName)
   }

   private fun betweenQueryForCaskInsertedAt(tableName: String, variant: BetweenVariant? = null) = when (variant) {
      BetweenVariant.GtLt -> findCountBetweenGtLtCaskInsertedAtQuery(tableName)
      BetweenVariant.GtLte -> findCountBetweenGtLteCaskInsertedAtQuery(tableName)
      BetweenVariant.GteLte -> findCountBetweenGteLteCaskInsertedAtQuery(tableName)
      else -> findCountBetweenCaskInsertedAtQuery(tableName)
   }

   fun findTableNamesForType(qualifiedName: QualifiedName): List<String> {
      return caskConfigRepository.findAllByQualifiedTypeNameAndStatus(qualifiedName.fullyQualifiedName, CaskStatus.ACTIVE)
         .map { it.tableName }
   }

   fun findTableNamesForType(versionedType: VersionedType): List<String> {
      return findTableNamesForType(versionedType.taxiType.toQualifiedName())
   }

   fun findCountAfter(versionedType: VersionedType, columnName: String, after: String): Int {
      val count = timed("${versionedType.versionedName}.findCountBy${columnName}.after") {
         val field = fieldForColumnName(versionedType, columnName)
         countForAllTablesOfType(versionedType) { tableName ->
            jdbcStreamingTemplate.queryForObject(findCountAfterQuery(tableName, columnName), Int::class.java, castArgumentToJdbcType(field, after))
         }
      }

      logger.debug {"Record count for findCountAfter : $count"}
      return count

   }

   fun findCountBefore(versionedType: VersionedType, columnName: String, before: String): Int {
      val count = timed("${versionedType.versionedName}.findCountBefore${columnName}.before") {
         val field = fieldForColumnName(versionedType, columnName)
         countForAllTablesOfType(versionedType) { tableName ->
            jdbcStreamingTemplate.queryForObject(findCountBeforeQuery(tableName, columnName), Int::class.java, castArgumentToJdbcType(field, before))
         }
      }

      logger.debug {"Record count for findCountBefore : $count"}
      return count
   }

   private fun fieldForColumnName(versionedType: VersionedType, columnName: String): Field {
      val originalTypeSchema = schemaProvider.schema()
      val originalType = originalTypeSchema.versionedType(versionedType.fullyQualifiedName.fqn())
      return (originalType.taxiType as ObjectType).allFields.first { it.name == columnName }
   }

   companion object {
      // TODO change to prepared statement, unlikely but potential for SQL injection via columnName
      /*
      Find Count All queries - following pattern of findX
       */

      fun findCountAllQuery(tableName: String) = """SELECT count(*) FROM $tableName"""
      fun findCountInQuery(tableName: String, columnName: String, inPhrase: String) =
         """SELECT count(*) FROM $tableName WHERE "$columnName" IN ($inPhrase)"""

      fun findCountByQuery(tableName: String, columnName: String) =
         """SELECT count(*) FROM $tableName WHERE "$columnName" = ?"""

      fun findCountBetweenQuery(tableName: String, columnName: String) =
         """SELECT count(*) FROM $tableName WHERE "$columnName" >= ? AND "$columnName" < ?"""

      fun findCountBetweenQueryGtLt(tableName: String, columnName: String) =
         """SELECT count(*) FROM $tableName WHERE "$columnName" > ? AND "$columnName" < ?"""

      fun findCountBetweenQueryGtLte(tableName: String, columnName: String) =
         """SELECT count(*) FROM $tableName WHERE "$columnName" > ? AND "$columnName" <= ?"""

      fun findCountBetweenQueryGteLte(tableName: String, columnName: String) =
         """SELECT count(*) FROM $tableName WHERE "$columnName" >= ? AND "$columnName" <= ?"""

      fun findCountBetweenCaskInsertedAtQuery(tableName: String) =
         """SELECT count(*) FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} >= ? AND message.${CaskMessage.INSERTED_AT_COLUMN} < ?"""

      fun findCountBetweenGtLtCaskInsertedAtQuery(tableName: String) =
         """SELECT count(*) FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} > ? AND message.${CaskMessage.INSERTED_AT_COLUMN} < ?"""

      fun findCountBetweenGtLteCaskInsertedAtQuery(tableName: String) =
         """SELECT count(*) FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} > ? AND message.${CaskMessage.INSERTED_AT_COLUMN} <= ?"""

      fun findCountBetweenGteLteCaskInsertedAtQuery(tableName: String) =
         """SELECT count(*) FROM $tableName caskTable INNER JOIN cask_message message ON caskTable.${MESSAGE_ID_COLUMN_NAME} = message.${CaskMessage.ID_COLUMN} WHERE message.${CaskMessage.INSERTED_AT_COLUMN} >= ? AND message.${CaskMessage.INSERTED_AT_COLUMN} <= ?"""

      fun findCountAfterQuery(tableName: String, columnName: String) =
         """SELECT count(*) FROM $tableName WHERE "$columnName" > ?"""

      fun findCountBeforeQuery(tableName: String, columnName: String) =
         """SELECT count(*) FROM $tableName WHERE "$columnName" < ?"""


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


   }


}
