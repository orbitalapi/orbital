package io.vyne.cask.ddl

import de.bytefish.pgbulkinsert.pgsql.constants.DataType
import de.bytefish.pgbulkinsert.row.SimpleRow
import io.vyne.VersionedSource
import io.vyne.cask.annotations.Annotations
import io.vyne.cask.ddl.PostgresDdlGenerator.Companion.MESSAGE_ID_COLUMN_DDL
import io.vyne.cask.ddl.PostgresDdlGenerator.Companion.MESSAGE_ID_COLUMN_NAME
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.timed
import io.vyne.cask.types.allFields
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.EnumType
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import lang.taxi.utils.quoted
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import java.math.BigDecimal
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class TableMetadata(
   val tableName: String,
   val qualifiedTypeName: String,
   val versionHash: String,
   val sourceSchemaIds: List<String>,
   val sources: List<String>,
   val timestamp: Instant = Instant.now()
) {

   private val versionedSources: List<VersionedSource> = sourceSchemaIds.mapIndexed { index, schemaId ->
      val source = sources[index]
      VersionedSource.forIdAndContent(schemaId, source)
   }

   // TODO This is slow for large schemas, generates lots of garbage objects
   // this object is not used here, only by DatasourceUpgrader and CaskDao
   val schema: TaxiSchema by lazy {
      timed("Initializing schema", true, TimeUnit.MILLISECONDS) {
         TaxiSchema.from(versionedSources)
      }
   }

   companion object {
      const val TABLE_NAME = "Table_Metadata"
      const val DROP_TABLE = """DROP TABLE IF EXISTS $TABLE_NAME"""

      // TODO refactor/cleanup this is no managed by Flyway
      val CREATE_TABLE = """CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            | tableName varchar(32) NOT NULL,
            | qualifiedTypeName varchar(255) NOT NULL,
            | versionHash varchar(32) NOT NULL,
            | sourceSchemaIds text[] NOT NULL,
            | sources text[] NOT NULL,
            | insertedAt timestamp NOT NULL
            | )
        """.trimMargin()

      fun deleteEntry(type: VersionedType, template: JdbcTemplate) {
         val deleteDml = """DELETE from $TABLE_NAME where qualifiedTypeName = ? and versionHash = ?;"""
         try {
            template.update { connection ->
               connection.prepareStatement(deleteDml).apply {
                  setString(1, type.fullyQualifiedName)
                  setString(2, type.versionHash)
               }
            }
         } catch (exception: Exception) {
            if (exception.cause?.message?.contains("""ERROR: relation "table_metadata" does not exist""") == true) {
               // ignore it, as we just haven't persisted anything yet
            } else {
               throw(exception)
            }
         }
      }
   }

   // TODO refactor/cleanup this is no managed by Flyway
   private val dml = """INSERT into $TABLE_NAME (
        | tableName,
        | qualifiedTypeName,
        | versionHash,
        | sourceSchemaIds,
        | sources,
        | insertedAt)
        | values ( ?, ?, ?, ?, ?, ? )
    """.trimMargin()

   fun executeInsert(template: JdbcTemplate) {
      template.update { connection ->
         connection.prepareStatement(dml).apply {
            setString(1, tableName)
            setString(2, qualifiedTypeName)
            setString(3, versionHash)
            setArray(4, connection.createArrayOf("text", sourceSchemaIds.toTypedArray()))
            setArray(5, connection.createArrayOf("text", sources.toTypedArray()))
            setTimestamp(6, Timestamp.from(timestamp))
         }
      }
   }
}

data class TableGenerationStatement(
   val ddlStatement: String,
   val versionedType: VersionedType,
   val generatedTableName: String,
   val columns: List<PostgresColumn>,
   val metadata: TableMetadata
)

fun VersionedType.caskRecordTable(): String {
   return PostgresDdlGenerator.tableName(this)
}

class PostgresDdlGenerator {

   companion object {
      const val MESSAGE_ID_COLUMN_NAME = "caskmessageid"
      const val MESSAGE_ID_COLUMN_DDL = "\"$MESSAGE_ID_COLUMN_NAME\" varchar(64)"

      private const val POSTGRES_MAX_NAME_LENGTH = 31
      fun tableName(versionedType: VersionedType): String {
         val typeName = versionedType.type.name.name
         val tableName = if (versionedType.versionedNameHash.startsWith(typeName)) {
            versionedType.versionedNameHash
         } else {
            "${typeName}_${versionedType.versionedNameHash}"
         }.takeLast(POSTGRES_MAX_NAME_LENGTH)
         require(tableName.length <= POSTGRES_MAX_NAME_LENGTH) { "Generated tableName $tableName exceeds Postgres max of 31 characters" }
         return tableName.toLowerCase()
      }

      fun toColumnName(field: Field) = field.name.quoted()
   }

   fun generateDrop(versionedType: VersionedType): String {
      return "DROP TABLE IF EXISTS ${tableName(versionedType)};"
   }

   data class UpsertStatement(
      val sqlStatement: String,
      private val parameterSourceProvider: (InstanceAttributeSet) -> SqlParameterSource
   ) {
      fun toParameterSource(attributeSet: InstanceAttributeSet): SqlParameterSource =
         parameterSourceProvider(attributeSet)
   }

   private data class FieldWrapper(val name: String, val isPrimaryKey: Boolean) {

      val quotedFieldName = name.quoted()
      val placeholderName = ":$name"
      val updateStatementWithPlaceholder = "$quotedFieldName = $placeholderName"

      companion object {
         fun forField(field: Field): FieldWrapper =
            FieldWrapper(field.name, field.annotations.any { it.name == Annotations.PRIMARY_KEY })
      }
   }

   fun generateUpsertWithPlaceholders(versionedType: VersionedType): UpsertStatement {
      val tableName = tableName(versionedType)
      val fields = versionedType.allFields().sortedBy { it.name }
         .map { FieldWrapper.forField(it) }
         .plus(FieldWrapper(MESSAGE_ID_COLUMN_NAME, isPrimaryKey = false))
      // TODO : Why are we using this logic if there is no pk?
      if (fields.none { it.isPrimaryKey }) {
         error("Bomb triggered - why are we using upsert logic without a pk?")
      }

      val fieldNameList = fields.joinToString { it.quotedFieldName }
      val placeholderNameList = fields.joinToString { it.placeholderName }
      val primaryKeyNameList = fields.filter { it.isPrimaryKey }.joinToString { it.quotedFieldName }
      val nonPkUpdateClauseList = fields.filter { !it.isPrimaryKey }.joinToString { it.updateStatementWithPlaceholder }

      val statement = """
         INSERT INTO $tableName ( $fieldNameList )
         VALUES ( $placeholderNameList )
         ON CONFLICT ( $primaryKeyNameList )
         DO UPDATE SET $nonPkUpdateClauseList
      """.trimIndent()

      val parameterSourceProvider = { instanceAttributeSet: InstanceAttributeSet ->
         val parameterSource = MapSqlParameterSource()
         versionedType.allFields().map { field -> field to generateValueForField(field, instanceAttributeSet) }
            .forEach { (field, value) ->
               val primitiveType = getPrimitiveType(field, field.type)
               val dbType = getDbTypeForField(primitiveType)
               parameterSource.addValue(field.name, value, dbType.sqlType, dbType.postgresTypeName)
            }
         parameterSource.addValue(MESSAGE_ID_COLUMN_NAME, instanceAttributeSet.messageId, Types.VARCHAR)
         parameterSource
      }
      return UpsertStatement(statement, parameterSourceProvider)
   }

   fun generateUpsertDml(versionedType: VersionedType, instance: InstanceAttributeSet): String {
      val tableName = tableName(versionedType)
      val fields = versionedType.allFields().sortedBy { it.name }
      val primaryKeyFields = (versionedType.taxiType as ObjectType).definition!!.fields
         .filter { it.annotations.any { a -> a.name == Annotations.PRIMARY_KEY } }
      val fieldsExcludingPk = fields.minus(primaryKeyFields)
      val values: Map<String, Any> = fields.mapNotNull {
         val generateValueForField = generateValueForField(it, instance)
         if (generateValueForField != null) {
            it.name to generateValueForField
         } else {
            null
         }
      }.toMap()

      val fieldNameList = fields.joinToString(", ") { "\"${it.name}\"" } + ", ${MESSAGE_ID_COLUMN_NAME.quoted()}"

      val fieldValueLIst = fields.joinToString(", ") { values[it.name].toString() } + ", '${instance.messageId}'"
      val primaryKeyFieldsList = primaryKeyFields.joinToString(", ") { "\"${it.name}\"" }

      val hasPrimaryKey = primaryKeyFields.isNotEmpty()

      val upsertConflictStatement = if (hasPrimaryKey) {
         val nonPkFieldsAndValues = fieldsExcludingPk.joinToString(", ") { "\"${it.name}\" = ${values[it.name]}" } +
            ", ${MESSAGE_ID_COLUMN_NAME.quoted()} = '${instance.messageId}'"
         """ON CONFLICT ( $primaryKeyFieldsList )
            |DO UPDATE SET $nonPkFieldsAndValues""".trimMargin()
      } else ""
      return """INSERT INTO $tableName ( $fieldNameList )
         | VALUES ( $fieldValueLIst )
         | $upsertConflictStatement
      """.trimMargin()
   }

   fun generateDdl(versionedType: VersionedType, schema: Schema): TableGenerationStatement {
      // Design choice - I'm generating against the Taxi type, not the vyne
      // one, as we're migrating back to Taxi types
      val type = schema.toTaxiType(versionedType)
      // if we're not migrating types, store all fields on the type
      val fields = versionedType.allFields().filter { it.formula == null }

      return generateDdl(type, versionedType, fields)
   }

   private fun generateDdl(type: Type, versionedType: VersionedType, fields: List<Field>): TableGenerationStatement {
      return when (type) {
         is ObjectType -> generateObjectDdl(type, versionedType, fields)
         else -> TODO("Type ${type::class.simpleName} not yet supported")
      }
   }

   // Note - could probably collapse this with the caller method at the moment, since
   // we're not supporting anything other than ObjectTypes.
   // However, that'll change shortly, and don't wanna refactor this again.
   private fun generateObjectDdl(
      type: ObjectType,
      versionedType: VersionedType,
      fields: List<Field>
   ): TableGenerationStatement {
      val columns = fields.map { generateColumnForField(it) } + MessageIdColumn
      val tableName = tableName(versionedType)
      val ddl = """${generateCaskTableDdl(versionedType, fields)}
         |${generateTableIndexesDdl(tableName, fields)}
      """.trimMargin()
      val metadata = TableMetadata(
         tableName,
         type.qualifiedName,
         versionedType.versionHash,
         emptyList(),
         emptyList(),
//         versionedType.sources.map { it.id },
         // Note:  We're persisting the entire schema.  This is obviously way too much,
         // and will cause a big perf hit once we get real schemas here.
         // We need to build the ability to create a subset of a schema, based on the data needed
         // to compile a single type - pulling in type references where required.
//         versionedType.sources.map { it.content },
         Instant.now()
      )
      return TableGenerationStatement(ddl, versionedType, tableName, columns, metadata)
   }

   private fun generateTableIndexesDdl(tableName: String, fields: List<Field>): String {
      val result = StringBuilder()
      // TODO We can not have a field declared as both a PK and a unique constraint. Perhaps we should handle that on taxi side too.
      val indexedColumns =
         fields.filter { col -> col.annotations.any { it.name == Annotations.INDEXED } && !col.annotations.any { it.name == Annotations.PRIMARY_KEY } }
      indexedColumns.forEach {
         result.appendln("""CREATE INDEX IF NOT EXISTS idx_${tableName}_${it.name} ON ${tableName}("${it.name}");""")
      }
      // Also index our internal fields
      result.appendln("""CREATE INDEX IF NOT EXISTS idx_${tableName}_${MESSAGE_ID_COLUMN_NAME} on ${tableName}("$MESSAGE_ID_COLUMN_NAME");""")
      return result.toString()
   }

   private fun generateCaskTableDdl(versionedType: VersionedType, fields: List<Field>): String {
      val tableName = tableName(versionedType)
      val columns = fields.map { generateColumnForField(it) } + MessageIdColumn
      val fieldDef = columns.joinToString(",\n") { it.sql }

      return """CREATE TABLE IF NOT EXISTS $tableName (
         |$fieldDef
         |${generatePrimaryKey(fields, tableName)});""".trimMargin()
   }

   fun generateColumnForField(field: Field): PostgresColumn {
      val primitiveType = getPrimitiveType(field, field.type)
      return generateColumnForField(field, primitiveType)
   }

   private fun getPrimitiveType(field: Field, type: Type): PrimitiveType {
      return when {
         PrimitiveType.isAssignableToPrimitiveType(type) -> {
            PrimitiveType.getUnderlyingPrimitive(type)
         }
         type is EnumType -> {
            PrimitiveType.STRING
         }
         type.inheritsFrom.size == 1 -> {
            getPrimitiveType(field, type.inheritsFrom.first())
         }
         else -> {
            TODO("Unable to generate column for field=${field}, type=${type}") //To change body of created functions use File | Settings | File Templates.
         }
      }
   }

   private fun generatePrimaryKey(fields: List<Field>, tableName: String): String {
      val pks = fields.filter { it.annotations.any { a -> a.name == Annotations.PRIMARY_KEY } }

      if (pks.isNotEmpty()) {
         return """,
            |CONSTRAINT ${tableName}_pkey PRIMARY KEY ( ${pks.joinToString(", ") { """"${it.name}"""" }} )""".trimMargin()
      }
      return ""
   }

   private fun getDbTypeForField(primitiveType: PrimitiveType): DbTypeName {
      return when (primitiveType) {
         PrimitiveType.STRING -> ScalarTypes.varchar()
         PrimitiveType.ANY -> ScalarTypes.varchar()
         PrimitiveType.DECIMAL -> ScalarTypes.numeric()
         PrimitiveType.DOUBLE -> ScalarTypes.numeric()
         PrimitiveType.INTEGER -> ScalarTypes.INTEGER
         PrimitiveType.BOOLEAN -> ScalarTypes.BOOLEAN
         PrimitiveType.LOCAL_DATE -> ScalarTypes.DATE
         PrimitiveType.DATE_TIME -> ScalarTypes.TIMESTAMP
         PrimitiveType.INSTANT -> ScalarTypes.TIMESTAMP
         PrimitiveType.TIME -> ScalarTypes.TIME
         else -> TODO("Primitive type ${primitiveType.name} not yet mapped")
      }
   }

   private fun getWriterForField(field: Field, primitiveType: PrimitiveType): RowWriter {
      val columnName = toColumnName(field)
      return when (primitiveType) {
         PrimitiveType.STRING -> { row, v -> row.setText(columnName, v.toString()) }
         PrimitiveType.ANY -> { row, v -> row.setText(columnName, v.toString()) }
         PrimitiveType.DECIMAL -> { row, v ->
            row.setNumeric(
               columnName,
               positiveScaledBigDecimal(v as BigDecimal)
            )
         }
         PrimitiveType.DOUBLE -> { row, v ->
            row.setNumeric(
               columnName,
               positiveScaledBigDecimal(v as BigDecimal)
            )
         }
         PrimitiveType.INTEGER -> { row, v -> row.setInteger(columnName, v as Int) }
         PrimitiveType.BOOLEAN -> { row, v -> row.setBoolean(columnName, v as Boolean) }
         PrimitiveType.LOCAL_DATE -> { row, v -> row.setDate(columnName, v as LocalDate) }
         PrimitiveType.DATE_TIME -> { row, v ->
            row.setTimeStamp(
               columnName,
               v as LocalDateTime
            )
         }
         PrimitiveType.INSTANT -> { row, v ->
            row.setTimeStamp(
               columnName,
               LocalDateTime.ofInstant((v as Instant), ZoneId.of("UTC"))
            )
         }
         PrimitiveType.TIME -> { row, v ->
            row.setValue(columnName, DataType.Time, (v as LocalTime))
         }
         else -> TODO("Primitive type ${primitiveType.name} not yet mapped")
      }
   }

   private fun generateColumnForField(field: Field, primitiveType: PrimitiveType): PostgresColumn {
      val columnName = toColumnName(field)
      val postgresType = getDbTypeForField(primitiveType)
      val writer = getWriterForField(field, primitiveType)
      val nullable = "" //if (field.nullable) "" else " NOT NULL"

      return FieldBasedColumn(columnName, field, "$columnName ${postgresType.postgresTypeName}$nullable", writer)
   }

   // pgbulkinsert library does not handle BigDecimal's with negative scale.
   // so this is a workaround for the issue till the library fixes it in future releases.
   private fun positiveScaledBigDecimal(bigDecimal: BigDecimal): BigDecimal {
      return if (bigDecimal.scale() < 0) {
         BigDecimal(bigDecimal.toPlainString())
      } else {
         bigDecimal
      }
   }

   private fun generateValueForField(field: Field, instance: InstanceAttributeSet): Any? {
      return when (val primitiveType = getPrimitiveType(field, field.type)) {
         PrimitiveType.STRING,
         PrimitiveType.BOOLEAN,
         PrimitiveType.LOCAL_DATE,
         PrimitiveType.DATE_TIME,
         PrimitiveType.INSTANT,
         PrimitiveType.TIME,
         PrimitiveType.ANY -> {
            // These values used to be quoted.
            // However, in moving to NamedParameterJdbcTemplate it seems this isn't needed anymore.
            // If everything looks ok, we can remove this code later.
            instance.attributes.getValue(field.name).value
//            val value = instance.attributes.getValue(field.name).value
//            if (value == null) {
//               value
//            } else {
//               "'${instance.attributes.getValue(field.name).value}'"
//            }
         }
         PrimitiveType.DECIMAL,
         PrimitiveType.DOUBLE,
         PrimitiveType.INTEGER -> instance.attributes.getValue(field.name).value
         else -> TODO("Primitive type ${primitiveType.name} not yet mapped")
      }
   }
}

data class DbTypeName(val postgresTypeName: String, val sqlType: Int)
private object ScalarTypes {
   fun varchar(size: Int = 255) = DbTypeName("VARCHAR($size)", Types.VARCHAR)
   fun numeric(precision: Int = 30, scale: Int = 15) = DbTypeName("NUMERIC($precision,$scale)", Types.NUMERIC)
   val INTEGER = DbTypeName("INTEGER", Types.INTEGER)
   val BOOLEAN = DbTypeName("BOOLEAN", Types.BOOLEAN)
   val TIMESTAMP = DbTypeName("TIMESTAMP", Types.TIMESTAMP)
   val TIME = DbTypeName("TIME", Types.TIME)
   val DATE = DbTypeName("DATE", Types.DATE)
}

typealias RowWriter = (rowWriter: SimpleRow, value: Any) -> Unit

interface PostgresColumn {
   val name: String
   val sql: String
   fun write(rowWriter: SimpleRow, value: Any)
   fun readValue(attributeSet: InstanceAttributeSet): Any?
}

object MessageIdColumn : PostgresColumn {
   override val name: String = MESSAGE_ID_COLUMN_NAME.quoted()
   override val sql: String = MESSAGE_ID_COLUMN_DDL
   override fun write(rowWriter: SimpleRow, value: Any) {
      rowWriter.setVarChar(name, value.toString())
   }

   override fun readValue(attributeSet: InstanceAttributeSet): Any {
      return attributeSet.messageId
   }

}

data class FieldBasedColumn(
   override val name: String,
   private val field: Field,
   override val sql: String,
   private val writer: RowWriter
) : PostgresColumn {
   override fun write(rowWriter: SimpleRow, value: Any) {
      writer(rowWriter, value)
   }

   override fun readValue(attributeSet: InstanceAttributeSet): Any? {
      return attributeSet.attributes.getValue(field.name).value
   }
}

