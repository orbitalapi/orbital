package io.vyne.cask.ddl

import de.bytefish.pgbulkinsert.row.SimpleRow
import io.vyne.VersionedSource
import io.vyne.cask.timed
import io.vyne.cask.types.allFields
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.*
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.nio.file.Path
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class TableMetadata(
   val tableName: String,
   val qualifiedTypeName: String,
   val versionHash: String,
   val sourceSchemaIds: List<String>,
   val sources: List<String>,
   val timestamp: Instant = Instant.now(),
   val readCachePath: Path?,
   val deltaAgainstTableName: String?
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
            | insertedAt timestamp NOT NULL,
            | readCachePath varchar(255),
            | deltaAgainst varchar(32)
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
        | insertedAt,
        | deltaAgainst,
        | readCachePath)
        | values ( ? , ? , ?, ?, ?, ?, ?, ? )
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
            if (deltaAgainstTableName != null) setString(7, deltaAgainstTableName) else setNull(7, Types.VARCHAR)
            if (readCachePath != null) setString(8, readCachePath.toString()) else setNull(8, Types.VARCHAR)
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
      private const val POSTGRES_MAX_NAME_LENGTH = 31
      fun tableName(versionedType: VersionedType): String {
         val typeName = versionedType.type.name.name
         val tableName = if (versionedType.versionedNameHash.startsWith(typeName)) {
            versionedType.versionedNameHash
         } else {
            "${typeName}_${versionedType.versionedNameHash}"
         }.takeLast(Companion.POSTGRES_MAX_NAME_LENGTH)
         require(tableName.length <= POSTGRES_MAX_NAME_LENGTH) { "Generated tableName $tableName exceeds Postgres max of 31 characters" }
         return tableName
      }
      fun toColumnName(field: Field) = """"${field.name}""""
   }

   fun generateDrop(versionedType: VersionedType): String {
      return "DROP TABLE IF EXISTS ${tableName(versionedType)};"
   }

   fun generateDdl(versionedType: VersionedType, schema: Schema, cachePath: Path?, typeMigration: TypeMigration?): TableGenerationStatement {
      // Design choice - I'm generating against the Taxi type, not the vyne
      // one, as we're migrating back to Taxi types
      val type = schema.toTaxiType(versionedType)
      val deltaAgainstTableName = typeMigration?.predecessorType?.let { tableName(it) }

      // if we're not migrating types, store all fields on the type
      val fields = typeMigration?.fields ?: versionedType.allFields()

      return generateDdl(type, schema, versionedType, fields, cachePath, deltaAgainstTableName)
   }

   private fun generateDdl(type: Type, schema: Schema, versionedType: VersionedType, fields: List<Field>, cachePath: Path?, deltaAgainstTableName: String?): TableGenerationStatement {
      return when (type) {
         is ObjectType -> generateObjectDdl(type, schema, versionedType, fields, cachePath, deltaAgainstTableName)
         else -> TODO("Type ${type::class.simpleName} not yet supported")
      }
   }

   // Note - could probably collapse this with the caller method at the moment, since
   // we're not supporting anything other than ObjectTypes.
   // However, that'll change shortly, and don't wanna refactor this again.
   private fun generateObjectDdl(type: ObjectType, schema: Schema, versionedType: VersionedType, fields: List<Field>, cachePath: Path?, deltaAgainstTableName: String?): TableGenerationStatement {
      val columns = fields.map { generateColumnForField(it) }
      val tableName = tableName(versionedType)
      val ddl = generateCaskTableDdl(versionedType, fields)
      val metadata = TableMetadata(
         tableName,
         type.qualifiedName,
         versionedType.versionHash,
         versionedType.sources.map { it.id },
         // Note:  We're persisting the entire schema.  This is obviously way too much,
         // and will cause a big perf hit once we get real schemas here.
         // We need to build the ability to create a subset of a schema, based on the data needed
         // to compile a single type - pulling in type references where required.
         versionedType.sources.map { it.content },
         Instant.now(),
         cachePath,
         deltaAgainstTableName
      )
      return TableGenerationStatement(ddl, versionedType, tableName, columns, metadata)
   }

   fun generateCaskTableDdl(versionedType: VersionedType, fields: List<Field>): String {
      val tableName = tableName(versionedType)
      val columns = fields.map { generateColumnForField(it) }
      val fieldDef = columns.joinToString(",\n") { it.sql }
      return """CREATE TABLE IF NOT EXISTS $tableName (
$fieldDef)""".trim()
   }

   fun generateColumnForField(field: Field): PostgresColumn {
      val primitiveType = getPrimitiveType(field, field.type)
      return generateColumnForField(field, primitiveType)
   }

   internal fun getPrimitiveType(field: Field, type: Type): PrimitiveType {
      if (PrimitiveType.isAssignableToPrimitiveType(type)) {
         return PrimitiveType.getUnderlyingPrimitive(type)
      } else if (type is EnumType) {
         return PrimitiveType.STRING
      } else if (type.inheritsFrom.size == 1) {
         return getPrimitiveType(field, type.inheritsFrom.first())
      } else {
         TODO("Unable to generate column for field=${field}, type=${type}") //To change body of created functions use File | Settings | File Templates.
      }
   }


   private fun generateColumnForField(field: Field, primitiveType: PrimitiveType): PostgresColumn {
      val columnName = toColumnName(field)
      val p: Pair<String, RowWriter> = when (primitiveType) {
         PrimitiveType.STRING -> ScalarTypes.varchar() to { row, v -> row.setText(columnName, v.toString()) }
         PrimitiveType.ANY -> ScalarTypes.varchar() to { row, v -> row.setText(columnName, v.toString()) }
         PrimitiveType.DECIMAL -> ScalarTypes.numeric() to { row, v -> row.setNumeric(columnName, v as BigDecimal) }
         PrimitiveType.DOUBLE -> ScalarTypes.numeric() to { row, v -> row.setNumeric(columnName, v as BigDecimal) }
         PrimitiveType.INTEGER -> ScalarTypes.integer() to { row, v -> row.setNumeric(columnName, v as BigDecimal) }
         PrimitiveType.BOOLEAN -> ScalarTypes.boolean() to { row, v -> row.setBoolean(columnName, v as Boolean) }
         PrimitiveType.LOCAL_DATE -> ScalarTypes.date() to { row, v -> row.setDate(columnName, v as LocalDate) }
         PrimitiveType.DATE_TIME -> ScalarTypes.timestamp() to { row, v -> row.setTimeStamp(columnName, v as LocalDateTime) }
         PrimitiveType.INSTANT -> ScalarTypes.timestamp() to { row, v -> row.setTimeStamp(columnName, LocalDateTime.ofInstant((v as Instant), ZoneId.of("UTC")))}
         // TODO TIME db column type
//         PrimitiveType.TIME -> ScalarTypes.timestamp() to { row, v ->
//            run {
//               val time = (v as LocalTime)
//               val date = LocalDate.of(1970, 1, 1)
//               // Postgres api does not accept LocalTime so have to map to LocalDateTime
//               row.setTimeStamp(fieldName, LocalDateTime.of(date, time))
//            }
//         }
         else -> TODO("Primitive type ${primitiveType.name} not yet mapped")
      }
      val (postgresType, writer) = p
      val nullable = "" //if (field.nullable) "" else " NOT NULL"

      return PostgresColumn(columnName, field, "$columnName $postgresType$nullable", primitiveType, writer)
   }
}

private object ScalarTypes {
   fun varchar(size: Int = 255) = "VARCHAR($size)"
   fun numeric(precision: Int = 30, scale: Int = 15) = "NUMERIC($precision,$scale)"
   fun integer() = "INTEGER"
   fun boolean() = "BOOLEAN"
   fun timestamp() = "TIMESTAMP"
   fun date() = "DATE"
}

typealias RowWriter = (rowWriter: SimpleRow, value: Any) -> Unit

// Note: Storing the type explicity, rather than referencing from field, to avoid
// having to look up primitives via aliases and inheritence again
data class PostgresColumn(val name: String, val field: Field, val sql: String, val fieldType: Type, private val writer: RowWriter) {
   fun write(rowWriter: SimpleRow, value: Any) {
      writer(rowWriter, value)
   }
}

