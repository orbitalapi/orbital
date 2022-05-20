package io.vyne.connectors.jdbc.schema

import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.jdbc.TableTaxiGenerationRequest
import io.vyne.query.VyneQlGrammar
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.toVyneQualifiedName
import lang.taxi.TaxiDocument
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.Level
import lang.taxi.generators.Message
import lang.taxi.generators.SchemaWriter
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.services.Parameter
import lang.taxi.services.QueryOperation
import lang.taxi.services.QueryOperationCapability
import lang.taxi.services.Service
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import schemacrawler.schema.Catalog
import schemacrawler.schema.Column
import schemacrawler.schema.ColumnDataType
import schemacrawler.schema.Table
import java.util.*

/**
 * Builds taxi types from a SQL schema.
 * (ie., SQL -> Taxi.  NOT Taxi -> SQL)
 */
class JdbcTaxiSchemaGenerator(
   val catalog: Catalog,
   val schemaWriter: SchemaWriter = SchemaWriter()
) {
   private val fieldTypes = mutableMapOf<Column, Type>()
   private val models = mutableMapOf<Table, ObjectType>()

   /**
    * builds the schemas for the provided tables.
    * If serviceGenerationParams is passed, then services are also generated
    */
   fun buildSchema(
      tables: List<TableTaxiGenerationRequest>,
      schema: Schema,
      connectionName: String
   ): GeneratedTaxiCode {
      val messages = mutableListOf<Message>()
      val createdModels = tables.mapNotNull { tableRequest ->
         val tableMetadata = catalog.tables.singleOrNull { tableMetadata ->
            tableMetadata.name.equals(
               tableRequest.table.tableName,
               ignoreCase = true
            )
         }
         if (tableMetadata == null) {
            messages.add(
               Message(
                  Level.ERROR,
                  "Can't generate a schema for table $tableRequest as it wasn't found in the database"
               )
            )
            return@mapNotNull null
         }

         val fields = tableMetadata.columns.map { column ->
            val annotations = if (column.isPartOfPrimaryKey) {
               listOf(Annotation("Id"))
            } else {
               emptyList()
            }
            Field(
               name = column.name,
               type = getType(
                  tableMetadata,
                  column,
                  namespace = tableRequest.typeName?.typeName?.namespace ?: tableRequest.defaultNamespace ?: ""
               ),
               nullable = column.isNullable,
               annotations = annotations,
               compilationUnit = CompilationUnit.unspecified()
            )
         }

         // Create a default name using the table as the namespace if one wasn't provided
         val defaultModelNamespace =
            tableRequest.defaultNamespace ?: tableMetadata.name.toTaxiConvention(firstLetterAsUppercase = false)
         val defaultModelTypeName = tableMetadata.name.toTaxiConvention(
            firstLetterAsUppercase = true
         )
         val defaultModelName: String = "${defaultModelNamespace}.${defaultModelTypeName}"
         // TODO  :Need to handle if the model already exists
         val modelName = tableRequest.typeName?.typeName?.fullyQualifiedName ?: defaultModelName
         tableMetadata to ObjectType(
            modelName,
            ObjectTypeDefinition(
               fields.toSet(),
               annotations = setOf(
                  JdbcConnectorTaxi.Annotations.table(
                     tableMetadata.schema.name,
                     tableMetadata.name,
                     connectionName,
                  ).asAnnotation(schema.taxi)
               ),
               compilationUnit = CompilationUnit.unspecified()
            )
         )
      }
      models.putAll(createdModels)
      val services: Set<Service> = generateServices(createdModels, schema, connectionName)
      val doc = TaxiDocument(types = (fieldTypes.values + models.values).toSet(), services = services)
      return GeneratedTaxiCode(schemaWriter.generateSchemas(listOf(doc)), messages)
   }

   private fun generateServices(
      createdModels: List<Pair<Table, ObjectType>>,
      schema: Schema,
      connectionName: String,
   ): Set<Service> {
      return createdModels.map { (table, model) ->
         val modelNameForOperations =
            model.toVyneQualifiedName().shortDisplayName.toTaxiConvention(firstLetterAsUppercase = true)
         val findManyOperation = queryOperation(
            "findMany$modelNameForOperations",
            returnType = ArrayType.of(model),
            schema, model
         )
         val findOneOperation = queryOperation(
            "findOne$modelNameForOperations",
            returnType = model,
            schema, model
         )
         Service(
            model.qualifiedName + "Service",
            members = listOf(findManyOperation, findOneOperation),
            annotations = listOf(
               JdbcConnectorTaxi.Annotations.databaseOperation(connectionName).asAnnotation(schema.taxi)
            ),
            compilationUnits = listOf(CompilationUnit.generatedFor(model))
         )
      }.toSet()
   }

   private fun queryOperation(name: String, returnType: Type, schema: Schema, model: Type): QueryOperation {
      return QueryOperation(
         name = name,
         grammar = "vyneQl",
         returnType = returnType,
         capabilities = QueryOperationCapability.ALL,
         parameters = listOf(
            Parameter(
               annotations = emptyList(),
               type = schema.taxiType(VyneQlGrammar.QUERY_TYPE_NAME.fqn()),
               constraints = emptyList(),
               isVarArg = false,
               name = "querySpec"
            )
         ),
         annotations = emptyList(),
         compilationUnits = listOf(CompilationUnit.generatedFor(model))
      )
   }

   private fun getType(
      tableMetadata: Table,
      column: Column,
      namespace: String,
      followForeignKey: Boolean = true
   ): Type {

      // Make the namespace concatenable
      val namespacePrefix = if (namespace.isEmpty()) "" else "$namespace."
      return if (!column.isPartOfPrimaryKey && column.isPartOfForeignKey && followForeignKey) {
         return getType(
            column.referencedColumn.parent,
            column.referencedColumn,
            namespace,
            // If the column is part of another foreign key on the other side, it
            // doesn't matter, that's the type we want.
            followForeignKey = false
         )
      } else {
         val type = fieldTypes.getOrPut(column) {
            getFieldType(namespacePrefix, tableMetadata, column)
         }
         if (isArrayType(column)) {
            ArrayType(type, CompilationUnit.unspecified())
         } else {
            type
         }
      }
   }

   private fun isArrayType(column: Column): Boolean {
      return JdbcTypes.isArray(column.type.javaSqlType.defaultMappedClass)
   }

   private fun getFieldType(
      namespacePrefix: String,
      tableMetadata: Table,
      column: Column
   ): Type {
      val columnTypeName = namespacePrefix +
         tableMetadata.name.toTaxiConvention(firstLetterAsUppercase = false) +
         // Put column types in a dedicated namespace.
         // This is to avoid conflicts where a table and a column have the same
         // name, but need to be separate types.
         // eg:
         // Table City
         //    -   column : city (String) // The city name
         ".types." +
         column.name.toTaxiConvention(
            firstLetterAsUppercase = true
         )
      return ObjectType(
         columnTypeName,
         ObjectTypeDefinition(
            inheritsFrom = setOf(getBasePrimitive(column.type)),
            compilationUnit = CompilationUnit.unspecified()
         )
      )
   }

   private fun getBasePrimitive(type: ColumnDataType): Type {
      val defaultMappedClass = type.javaSqlType.defaultMappedClass
      return when {
         PrimitiveTypes.isClassTaxiPrimitive(defaultMappedClass) -> PrimitiveTypes.getTaxiPrimitive(defaultMappedClass)
         JdbcTypes.isArray(defaultMappedClass) -> {
            // TODO : How do we determine the inner array type?  Assume String for now
            PrimitiveType.STRING
         }
         JdbcTypes.contains(defaultMappedClass) -> JdbcTypes.get(defaultMappedClass)
         else -> error("Type ${type.name} default maps to ${defaultMappedClass.canonicalName} which has no taxi equivalent")
      }
   }
}

private fun String.toTaxiConvention(firstLetterAsUppercase: Boolean): String {
   val parts = this.split("_", "-", ".")
   return parts.mapIndexed { index, word ->
      if (index == 0 && !firstLetterAsUppercase) {
         word.lowercase(Locale.getDefault())
      } else {
         word.lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
      }
   }.joinToString("")
}
