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
import lang.taxi.types.Type
import schemacrawler.schema.Catalog
import schemacrawler.schema.Column
import schemacrawler.schema.ColumnDataType
import schemacrawler.schema.Table


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
               type = getType(tableMetadata, column, namespace = tableRequest.typeName?.typeName?.namespace ?: ""),
               nullable = column.isNullable,
               annotations = annotations,
               compilationUnit = CompilationUnit.unspecified()
            )
         }
         // ie: com.foo.actor.Actor
         // Update: Since we now pass an optional type name, we've removed the namespace
         // concept in the defualtModelName.
         val defaultModelName: String =
            "${tableMetadata.name.toTaxiConvention(firstLetterAsUppercase = false)}.${
               tableMetadata.name.toTaxiConvention(
                  firstLetterAsUppercase = true
               )
            }"
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
         val queryOperation = QueryOperation(
            name = model.toVyneQualifiedName().shortDisplayName.toTaxiConvention(firstLetterAsUppercase = false) + "Query",
            grammar = "vyneQl",
            returnType = ArrayType.of(model),
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
         Service(
            model.qualifiedName + "Service",
            members = listOf(queryOperation),
            annotations = listOf(
               JdbcConnectorTaxi.Annotations.databaseOperation(connectionName).asAnnotation(schema.taxi)
            ),
            compilationUnits = listOf(CompilationUnit.generatedFor(model))
         )
      }.toSet()

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
         fieldTypes.getOrPut(column) {
            ObjectType(
               "$namespacePrefix${tableMetadata.name.toTaxiConvention(firstLetterAsUppercase = false)}.${
                  column.name.toTaxiConvention(
                     firstLetterAsUppercase = true
                  )
               }",
               ObjectTypeDefinition(
                  inheritsFrom = setOf(getBasePrimitive(column.type)),
                  compilationUnit = CompilationUnit.unspecified()
               )
            )
         }
      }
   }

   private fun getBasePrimitive(type: ColumnDataType): Type {
      val defaultMappedClass = type.javaSqlType.defaultMappedClass
      return if (PrimitiveTypes.isClassTaxiPrimitive(defaultMappedClass)) {
         PrimitiveTypes.getTaxiPrimitive(defaultMappedClass)
      } else if (JdbcTypes.contains(defaultMappedClass)) {
         JdbcTypes.get(defaultMappedClass)
      } else {
         error("Type ${type.name} default maps to ${defaultMappedClass.canonicalName} which has no taxi equivalent")
      }
      TODO("Base Primitive not mapped for type ${type.name}")
   }
}

private fun String.toTaxiConvention(firstLetterAsUppercase: Boolean): String {
   val parts = this.split("_", "-", ".")
   return parts.mapIndexed { index, word ->
      if (index == 0 && !firstLetterAsUppercase) {
         word.toLowerCase()
      } else {
         word.toLowerCase().capitalize()
      }
   }.joinToString("")
}
