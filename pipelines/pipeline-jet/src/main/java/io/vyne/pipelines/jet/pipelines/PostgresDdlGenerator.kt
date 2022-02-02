package io.vyne.pipelines.jet.pipelines

import de.bytefish.pgbulkinsert.pgsql.constants.DataType
import de.bytefish.pgbulkinsert.row.SimpleRow
import io.vyne.VersionedSource
import io.vyne.models.TypedInstance
import io.vyne.pipelines.jet.pipelines.PostgresDdlGenerator.Companion.MESSAGE_ID_COLUMN_DDL
import io.vyne.pipelines.jet.pipelines.PostgresDdlGenerator.Companion.MESSAGE_ID_COLUMN_NAME
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import lang.taxi.types.EnumType
import lang.taxi.types.Field
import lang.taxi.types.FieldReferenceSelector
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import lang.taxi.utils.quoted
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class InstanceAttributeSet(
   val type: VersionedType,
   val attributes: Map<String, TypedInstance>,
   val messageId:String
)

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
}

data class TableGenerationStatement(
   val ddlStatement: String,
   val versionedType: VersionedType,
   val generatedTableName: String,
   val columns: List<PostgresColumn>,
   val metadata: TableMetadata
)


class PostgresDdlGenerator {
   private val _primaryKey = "PrimaryKey"
   private val _indexed = "Indexed"

   companion object {
      const val MESSAGE_ID_COLUMN_NAME = "vyneid"
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
      fun toColumnName(field: Field) = toColumnName(field.name)
      fun toColumnName(fieldName: String) = fieldName.quoted()
      fun toSqlNull(fieldType: Type): String {
         val ddlGenerator = PostgresDdlGenerator()
         val postgresColumnType = ddlGenerator.postgresColumnType(ddlGenerator.getPrimitiveType(fieldType))
         return "null::$postgresColumnType"
      }
      fun selectNullAs(fieldName: String, fieldType: Type): String {
         return "${toSqlNull(fieldType)} as ${toColumnName(fieldName)}"
      }
      fun selectAs(sourceField: Field, targetFieldName: String) = "${toColumnName(sourceField)} as ${toColumnName(targetFieldName)}"
      fun selectAs(sourceField: String, targetFieldName: String) = "$sourceField as ${toColumnName(targetFieldName)}"
      fun selectAs(fieldName: String) = "as ${toColumnName(fieldName)}"
      fun toWindowFunctionExpression(windowFunction: String,
                                     windowFunctionFields:  List<FieldReferenceSelector>): String {
         val orderByExpression =  if (windowFunctionFields.size == 3)  " ORDER BY ${toColumnName(windowFunctionFields[2].fieldName)}" else ""
         val partitionBy = "PARTITION BY ${toColumnName(windowFunctionFields[1].fieldName)}"
         return "$windowFunction(${toColumnName(windowFunctionFields.first().fieldName)}) OVER ($partitionBy  $orderByExpression)"
      }
   }

   fun generateDrop(versionedType: VersionedType): String {
      return "DROP TABLE IF EXISTS ${tableName(versionedType)};"
   }

   fun fetchPrimaryFields(versionedType: VersionedType): List<Field> {
      return (versionedType.taxiType as ObjectType).definition!!.fields
         .filter { it.annotations.any { a -> a.name == _primaryKey } }
   }

   fun generateUpsertDml(versionedType: VersionedType, instance: InstanceAttributeSet, fetchOldValues: Boolean): UpsertMetadata {
      val tableName = tableName(versionedType)
      val fields = versionedType.allFields().sortedBy { it.name }
      val primaryKeyFields = this.fetchPrimaryFields(versionedType)
      val fieldsExcludingPk = fields.minus(primaryKeyFields)
      val values: Map<String, Any> = fields.mapNotNull {
         val generateValueForField = generateValueForField(it, instance)
         if (generateValueForField != null) {
            it.name to generateValueForField
         } else {
            null
         }
      }.toMap()

      val fieldNameList = fields.joinToString(", ") { "\"${it.name}\"" } +  ", ${MESSAGE_ID_COLUMN_NAME.quoted()}"

      val fieldValueLIst = fields.joinToString(", ") { values[it.name].toString() } + ", '${instance.messageId}'"
      val primaryKeyValues = primaryKeyFields.map {
         CaskIdColumnValue(
         toColumnName(it), values[it.name]!!
      )  }
      val primaryKeyFieldsList = primaryKeyFields.joinToString(", ") { "\"${it.name}\"" }

      val hasPrimaryKey = primaryKeyFields.isNotEmpty()
      val fetchReturnValuesStatement = if (fetchOldValues) generateReturningStatement(tableName, fields, primaryKeyFields) else ""

      val upsertConflictStatement = if (hasPrimaryKey) {
         val nonPkFieldsAndValues = fieldsExcludingPk.joinToString(", ") { "\"${it.name}\" = ${values[it.name]}" } +
            ", ${MESSAGE_ID_COLUMN_NAME.quoted()} = '${instance.messageId}'"
         """ON CONFLICT ( $primaryKeyFieldsList )
            |DO UPDATE SET $nonPkFieldsAndValues""".trimMargin()
      } else ""
      return UpsertMetadata( """INSERT INTO $tableName ( $fieldNameList )
         | VALUES ( $fieldValueLIst )
         | $upsertConflictStatement
         | $fetchReturnValuesStatement
      """.trimMargin(), primaryKeyValues)
   }

   private fun generateReturningStatement(tableName: String, fields: List<Field>, primaryKeyFields: List<Field>): String {
      val pkFieldFilters = primaryKeyFields.map { pkfield -> """ t2.${pkfield.name.quoted()} = $tableName.${pkfield.name.quoted()} """ }
      val pkFieldWhereStatement = pkFieldFilters.joinToString(" AND ")
      val fieldSelects = fields.map { field ->
         """ (select t2.${field.name.quoted()} from  $tableName t2 where $pkFieldWhereStatement) as ${field.name.quoted()} """
      }

      return """ RETURNING ${fieldSelects.joinToString(",")} """
   }
   fun generateDdl(versionedType: VersionedType, schema: Schema): TableGenerationStatement {
      // Design choice - I'm generating against the Taxi type, not the vyne
      // one, as we're migrating back to Taxi types
      val type = schema.toTaxiType(versionedType)
      // if we're not migrating types, store all fields on the type
      // TODO :  Should we exclude fields with accessors / expressions here?
      val fields = versionedType.allFields()

      return generateDdl(type, versionedType, fields)
   }

   fun generateDdlRedshift(versionedType: VersionedType, schema: Schema): TableGenerationStatement {
      // Design choice - I'm generating against the Taxi type, not the vyne
      // one, as we're migrating back to Taxi types
      val type = schema.toTaxiType(versionedType)
      // if we're not migrating types, store all fields on the type
      // TODO :  Should we exclude fields with accessors / expressions here?
      val fields = versionedType.allFields()

      return generateDdlRedshift(type, versionedType, fields)
   }

   fun generateDdlSnowflake(versionedType: VersionedType, schema: Schema): TableGenerationStatement {
      // Design choice - I'm generating against the Taxi type, not the vyne
      // one, as we're migrating back to Taxi types
      val type = schema.toTaxiType(versionedType)
      // if we're not migrating types, store all fields on the type
      // TODO :  Should we exclude fields with accessors / expressions here?
      val fields = versionedType.allFields()

      return generateDdlSnowflake(type, versionedType, fields)
   }

   private fun generateDdl(type: Type, versionedType: VersionedType, fields: List<Field>): TableGenerationStatement {
      return when (type) {
         is ObjectType -> generateObjectDdl(type, versionedType, fields)
         else -> TODO("Type ${type::class.simpleName} not yet supported")
      }
   }

   private fun generateDdlRedshift(type: Type, versionedType: VersionedType, fields: List<Field>): TableGenerationStatement {
      return when (type) {
         is ObjectType -> generateObjectDdlRedshift(type, versionedType, fields)
         else -> TODO("Type ${type::class.simpleName} not yet supported")
      }
   }

   private fun generateDdlSnowflake(type: Type, versionedType: VersionedType, fields: List<Field>): TableGenerationStatement {
      return when (type) {
         is ObjectType -> generateObjectDdlSnowflake(type, versionedType, fields)
         else -> TODO("Type ${type::class.simpleName} not yet supported")
      }
   }

   // Note - could probably collapse this with the caller method at the moment, since
   // we're not supporting anything other than ObjectTypes.
   // However, that'll change shortly, and don't wanna refactor this again.
   private fun generateObjectDdl(
      type: ObjectType,
      versionedType: VersionedType,
      fields: List<Field>): TableGenerationStatement {
      val taxiPrimaryKeyFields = this.fetchPrimaryFields(versionedType)
      val columns = fields.map { generateColumnForField(it) } +
         MessageIdColumn
      val tableName = tableName(versionedType)
      val ddl = """${generateCaskTableDdl(versionedType, fields, columns)}
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

   private fun generateObjectDdlRedshift(
      type: ObjectType,
      versionedType: VersionedType,
      fields: List<Field>): TableGenerationStatement {
      val taxiPrimaryKeyFields = this.fetchPrimaryFields(versionedType)
      val columns = fields.map { generateColumnForField(it) } +
         MessageIdColumn
      val tableName = tableName(versionedType)
      val ddl = """${generateCaskTableDdl(versionedType, fields, columns)}
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

   private fun generateObjectDdlSnowflake(
      type: ObjectType,
      versionedType: VersionedType,
      fields: List<Field>): TableGenerationStatement {
      val taxiPrimaryKeyFields = this.fetchPrimaryFields(versionedType)
      val columns = fields.map { generateColumnForField(it) } +
         MessageIdColumn
      val tableName = tableName(versionedType)
      val ddl = """${generateCaskTableDdl(versionedType, fields, columns)}
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
      val indexedColumns = fields.filter { col -> col.annotations.any { it.name == _indexed } && !col.annotations.any { it.name == _primaryKey} }
      indexedColumns.forEach {
         result.appendln("""CREATE INDEX IF NOT EXISTS idx_${tableName}_${it.name} ON ${tableName}("${it.name}");""")
      }
      // Also index our internal fields
      result.appendln("""CREATE INDEX IF NOT EXISTS idx_${tableName}_${MESSAGE_ID_COLUMN_NAME} on ${tableName}("$MESSAGE_ID_COLUMN_NAME");""")
      return result.toString()
   }

   private fun generateCaskTableDdl(versionedType: VersionedType, fields: List<Field>, columns: List<PostgresColumn>): String {
      val tableName = tableName(versionedType)
      val fieldDef = columns.joinToString(",\n") { it.sql }

      return """CREATE TABLE IF NOT EXISTS $tableName (
         |$fieldDef
         |${generatePrimaryKey(fields, tableName)});""".trimMargin()
   }

   fun generateColumnForField(field: Field): PostgresColumn {
      val primitiveType = getPrimitiveType(field.type)
      return generateColumnForField(field, primitiveType)
   }

   fun getPrimitiveType(type: Type): PrimitiveType {
      return when {
          PrimitiveType.isAssignableToPrimitiveType(type) -> {
             PrimitiveType.getUnderlyingPrimitive(type)
          }
          type is EnumType -> {
             PrimitiveType.STRING
          }
          type.inheritsFrom.size == 1 -> {
             getPrimitiveType(type.inheritsFrom.first())
          }
          else -> {
             TODO("Unable to generate column for type=${type}") //To change body of created functions use File | Settings | File Templates.
          }
      }
   }

   private fun generatePrimaryKey(fields: List<Field>, tableName: String): String {
      val pks = fields.filter { it.annotations.any { a -> a.name == _primaryKey } }

      return if (pks.isNotEmpty()) {
         """,
          |CONSTRAINT ${tableName}_pkey PRIMARY KEY ( ${pks.joinToString(", ") { """"${it.name}"""" }} )""".trimMargin()
      } else {
         """"""
        // """,
        //  |CONSTRAINT ${tableName}_pkey PRIMARY KEY ( ${CASK_ROW_ID_COLUMN_NAME.quoted()} )""".trimMargin()
      }
   }

   fun postgresColumnType(primitiveType: PrimitiveType): String {
      return when (primitiveType) {
         PrimitiveType.STRING -> ScalarTypes.varchar()
         PrimitiveType.ANY -> ScalarTypes.varchar()
         PrimitiveType.DECIMAL -> ScalarTypes.numeric()
         PrimitiveType.DOUBLE -> ScalarTypes.numeric()
         PrimitiveType.INTEGER -> ScalarTypes.integer()
         PrimitiveType.BOOLEAN -> ScalarTypes.boolean()
         PrimitiveType.LOCAL_DATE -> ScalarTypes.date()
         PrimitiveType.DATE_TIME -> ScalarTypes.timestamp()
         PrimitiveType.INSTANT -> ScalarTypes.timestamp()
         PrimitiveType.TIME -> ScalarTypes.time()
         else -> TODO("Primitive type ${primitiveType.name} not yet mapped")
      }
   }

   private fun generateColumnForField(field: Field, primitiveType: PrimitiveType): PostgresColumn {
      val columnName = toColumnName(field)
      val p: Pair<String, RowWriter> = when (primitiveType) {
         PrimitiveType.STRING -> ScalarTypes.varchar() to { row, v -> row.setText(columnName, v.toString()) }
         PrimitiveType.ANY -> ScalarTypes.varchar() to { row, v -> row.setText(columnName, v.toString()) }
         PrimitiveType.DECIMAL -> ScalarTypes.numeric() to { row, v -> row.setNumeric(columnName, positiveScaledBigDecimal(v as BigDecimal)) }
         PrimitiveType.DOUBLE -> ScalarTypes.numeric() to { row, v -> row.setNumeric(columnName, positiveScaledBigDecimal(v as BigDecimal)) }
         PrimitiveType.INTEGER -> ScalarTypes.integer() to { row, v -> row.setInteger(columnName, v as Int) }
         PrimitiveType.BOOLEAN -> ScalarTypes.boolean() to { row, v -> row.setBoolean(columnName, v as Boolean) }
         PrimitiveType.LOCAL_DATE -> ScalarTypes.date() to { row, v -> row.setDate(columnName, v as LocalDate) }
         PrimitiveType.DATE_TIME -> ScalarTypes.timestamp() to { row, v -> row.setTimeStamp(columnName, v as LocalDateTime) }
         PrimitiveType.INSTANT -> ScalarTypes.timestamp() to { row, v -> row.setTimeStamp(columnName, LocalDateTime.ofInstant((v as Instant), ZoneId.of("UTC")))}
         PrimitiveType.TIME -> ScalarTypes.time() to { row, v -> row.setValue(columnName, DataType.Time, (v as LocalTime))
         }
         else -> TODO("Primitive type ${primitiveType.name} not yet mapped")
      }
      val (postgresType, writer) = p
      val nullable = "" //if (field.nullable) "" else " NOT NULL"

      return FieldBasedColumn(columnName, field, "$columnName $postgresType$nullable", writer)
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
      return when (val primitiveType = getPrimitiveType(field.type)) {
         PrimitiveType.STRING,
         PrimitiveType.BOOLEAN,
         PrimitiveType.LOCAL_DATE,
         PrimitiveType.DATE_TIME,
         PrimitiveType.INSTANT,
         PrimitiveType.TIME,
         PrimitiveType.ANY -> {
            val value = instance.attributes.getValue(field.name).value
            if (value == null) {
               value
            } else {
               "'${instance.attributes.getValue(field.name).value}'"
            }
         }
         PrimitiveType.DECIMAL,
         PrimitiveType.DOUBLE,
         PrimitiveType.INTEGER -> instance.attributes.getValue(field.name).value
         else -> TODO("Primitive type ${primitiveType.name} not yet mapped")
      }
   }
}

private object ScalarTypes {
   fun varchar(size: Int = 255) = "VARCHAR($size)"
   fun numeric() = "NUMERIC"
   fun integer() = "INTEGER"
   fun boolean() = "BOOLEAN"
   fun timestamp() = "TIMESTAMP"
   fun time() = "TIME"
   fun date() = "DATE"
}

typealias RowWriter = (rowWriter: SimpleRow, value: Any) -> Unit

interface PostgresColumn {
   val name:String
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

   override fun readValue(attributeSet: InstanceAttributeSet): Any? {
      return attributeSet.messageId
   }

}


data class FieldBasedColumn(override val name: String, private val field: Field, override val sql: String, private val writer: RowWriter):PostgresColumn {
   override fun write(rowWriter: SimpleRow, value: Any) {
      writer(rowWriter, value)
   }

   override fun readValue(attributeSet: InstanceAttributeSet): Any? {
      return attributeSet.attributes.getValue(field.name).value
   }
}

data class UpsertMetadata(val upsertSqlStatement: String, val idColumnValues: List<CaskIdColumnValue>)


fun VersionedType.allFields(): List<Field> {
   return when (taxiType) {
      is ObjectType -> (taxiType as ObjectType).allFields
      else -> TODO("Support for non-object types coming")
   }
}

data class CaskIdColumnValue(val columnName: String, val value: Any)

