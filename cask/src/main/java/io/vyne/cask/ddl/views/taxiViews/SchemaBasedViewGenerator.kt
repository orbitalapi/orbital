package io.vyne.cask.ddl.views.taxiViews

import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.cask.ddl.views.CaskViewBuilder
import io.vyne.cask.ddl.views.CaskViewBuilder.Companion.caskMessageIdColumn
import io.vyne.cask.ddl.views.CaskViewBuilder.Companion.dropViewStatement
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.VersionedType
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.schemas.toVyneQualifiedName
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
import lang.taxi.types.Annotation
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import lang.taxi.types.View
import lang.taxi.types.ViewBodyDefinition
import lang.taxi.utils.log
import lang.taxi.utils.quoted
import org.springframework.stereotype.Component

@Component
class SchemaBasedViewGenerator(private val caskConfigRepository: CaskConfigRepository,
                               private val schemaStore: SchemaStore) {

   private val taxiWriter = SchemaWriter()
   fun taxiViews() = schemaStore.schemaSet().schema.taxi.views

   fun generateDdl(taxiView: View): List<String> {
      try {
         val tableNamesForSourceTypes = fetchTableNamesForParticipatingTypes(taxiView)
         if (tableNamesForSourceTypes.isEmpty()) {
            return emptyList()
         }
         val sqlViewName = viewSqlName(taxiView)
         val viewBodyDefinitions = taxiView.viewBodyDefinitions!!
         val sqlViewDefinition = viewBodyDefinitions.joinToString(" union all \n") { viewBodyDefinition ->
            viewBodyDefinitionToSql(taxiView, viewBodyDefinition, tableNamesForSourceTypes)
         }
         val createSqlBuilder = StringBuilder()
         createSqlBuilder
            .appendln("create or replace view $sqlViewName as")
            .appendln(sqlViewDefinition)

         return listOf(dropViewStatement(sqlViewName), createSqlBuilder.toString())
      } catch (e: Exception) {
         log().error("Error in creating DDL for taxi view ${taxiView.qualifiedName}", e)
         return emptyList()
      }
   }

   fun generateCaskConfig(taxiView: View): CaskConfig {
      val viewType = generateViewType(taxiView)
      val config = CaskConfig.forType(
         viewType,
         viewSqlName(taxiView),
         // Views also expose a new type
         exposesType = true,
         exposesService = true,
         daysToRetain = 100000
      )
      return config.copy(versionHash = taxiView.definitionHash)
   }

   fun getDependencies(taxiView: View): List<QualifiedName> {
      return fetchTableNamesForParticipatingTypes(taxiView).keys.toList()
   }

   private fun fieldsSql(taxiView: View,
                         objectType: ObjectType,
                         tableNamesForSourceTypes: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>,
                         whenStatementGenerator: WhenStatementGenerator): List<String> {
      return objectType.fields.map { field ->
         selectStatement(taxiView, objectType, field, tableNamesForSourceTypes, whenStatementGenerator)
      }
   }

   private fun viewBodyDefinitionToSql(
      taxiView: View,
      viewBodyDefinition: ViewBodyDefinition,
      tableNamesForSourceTypes: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      val bodyTableName = tableNamesForSourceTypes[viewBodyDefinition.bodyType.toQualifiedName()]!!.second.tableName
      val viewBodyType = viewBodyDefinition.viewBodyType!! as ObjectType
      val whereStatementGenerator = WhereStatementGenerator(this, tableNamesForSourceTypes)
      val whenStatementGenerator = WhenStatementGenerator(
         taxiView,
         viewBodyType,
         tableNamesForSourceTypes,
         schemaStore.schemaSet().schema)

      val fieldList = fieldsSql(taxiView, viewBodyType, tableNamesForSourceTypes, whenStatementGenerator)
      val sqlStatementsForEachField = if (viewBodyDefinition.joinType == null) {
         fieldList.plus(caskMessageIdColumn(bodyTableName))
      } else {
         fieldList
            .plus(caskMessageIdColumn(bodyTableName, tableNamesForSourceTypes[viewBodyDefinition.joinType!!.toQualifiedName()]!!.second.tableName))
      }
      val sqlBuilder = StringBuilder()
      sqlBuilder.appendln("select")
      sqlBuilder.appendln(sqlStatementsForEachField.joinToString(", \n"))
      if (viewBodyDefinition.joinType == null) {
         sqlBuilder.appendln(" from $bodyTableName")
         sqlBuilder.append(whereStatementGenerator.whereStatement(viewBodyDefinition.bodyTypeFilter?.let { Pair(viewBodyDefinition.bodyType, it) }, null))
      } else {
         val mainTableName = tableNamesForSourceTypes[viewBodyDefinition.bodyType.toQualifiedName()]!!.second.tableName
         val joinTableName = tableNamesForSourceTypes[viewBodyDefinition.joinType!!.toQualifiedName()]!!.second.tableName
         val joinField1 = "$mainTableName.${PostgresDdlGenerator.toColumnName(viewBodyDefinition.joinInfo!!.mainField)}"
         val joinField2 = "$joinTableName.${PostgresDdlGenerator.toColumnName(viewBodyDefinition.joinInfo!!.joinField)}"
         sqlBuilder.appendln(" from $mainTableName LEFT JOIN $joinTableName ON $joinField1 = $joinField2")
         sqlBuilder.append(whereStatementGenerator.whereStatement(viewBodyDefinition.bodyTypeFilter?.let { Pair(viewBodyDefinition.bodyType, it) },
            viewBodyDefinition.joinTypeFilter?.let { Pair(viewBodyDefinition.joinType!!, it) }))
      }
      return sqlBuilder.toString()
   }

   private fun fetchTableNamesForParticipatingTypes(view: View): Map<QualifiedName, Pair<QualifiedName, CaskConfig>> {
      val sourceTypeNames = mutableListOf<QualifiedName>()
      view.viewBodyDefinitions?.map {
         sourceTypeNames.add((it.bodyType.toQualifiedName()))
         it.joinType?.let { joinType -> sourceTypeNames.add(joinType.toQualifiedName()) }
      }

      val retValue = CaskViewBuilder.caskConfigsForQualifiedNames(sourceTypeNames, this.caskConfigRepository).associateBy { keySelection ->
         keySelection.first
      }

      val sortedSourceTypeNames = sourceTypeNames.toSet().sortedBy { it.fullyQualifiedName }
      val sortedRetValues = retValue.keys.toSet().sortedBy { it.fullyQualifiedName }
      return if (sortedSourceTypeNames != sortedRetValues) {
         emptyMap()
      } else {
         retValue
      }
   }

   private fun selectStatement(
      taxiView: View,
      objectType: ObjectType,
      viewFieldDefinition: Field,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>,
      whenStatementGenerator: WhenStatementGenerator): String {
      return when (viewFieldDefinition.accessor) {
         null -> {
            if (viewFieldDefinition.memberSource == null) {
               //case for:
               // fieldName: FieldType case.
               PostgresDdlGenerator.selectNullAs(viewFieldDefinition.name, viewFieldDefinition.type)
            } else {
               val sourceField = columnName(
                  viewFieldDefinition.memberSource!!,
                  viewFieldDefinition.type, qualifiedNameToCaskConfig)
               PostgresDdlGenerator.selectAs(sourceField, viewFieldDefinition.name)
            }
         }
         else -> "${whenStatementGenerator.toWhenSql(viewFieldDefinition)} ${PostgresDdlGenerator.selectAs(viewFieldDefinition.name)}"
      }
   }

   private fun getField(sourceType: QualifiedName, fieldType: Type): Field {
      val objectType = this.schemaStore.schemaSet().schema.type(sourceType.fullyQualifiedName).taxiType as ObjectType
      return objectType.fields.first { field ->
         field.type == fieldType || (field.type.format != null && field.type.formattedInstanceOfType == fieldType)
      }
   }

   fun columnName(
      sourceType: QualifiedName,
      fieldType: Type,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      val sourceTableName = qualifiedNameToCaskConfig[sourceType]?.second?.tableName
      val columnName = PostgresDdlGenerator.toColumnName(getField(sourceType, fieldType))
      return "$sourceTableName.$columnName"
   }

   fun columnName(
      sourceType: QualifiedName,
      fieldType: QualifiedName,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      val fieldTaxiType = this.schemaStore.schemaSet().schema.type(fieldType.fullyQualifiedName).taxiType
      return columnName(sourceType, fieldTaxiType, qualifiedNameToCaskConfig)
   }

   private fun viewSqlName(taxiView: View) = "${CaskViewBuilder.VIEW_PREFIX}${taxiView.toQualifiedName().typeName}"

   private fun generateViewType(taxiView: View): VersionedType {
      val taxiDoc = generateTaxi(taxiView)
      val importSources = schemaStore.schemaSet().taxiSchemas
      val taxiSource = generateTaxiSource(taxiDoc)
      val schema = TaxiSchema.from(VersionedSource.sourceOnly(taxiSource), importSources)
      return schema.versionedType(taxiView.toQualifiedName().toVyneQualifiedName())
   }

   private fun generateTaxiSource(document: TaxiDocument): String {
      val schemas = taxiWriter.generateSchemas(listOf(document), importLocation = SchemaWriter.ImportLocation.CollectImports)
      return schemas.joinToString("\n")
   }

   private fun generateTaxi(taxiView: View): TaxiDocument {
      val typeDeclaration = this.typeFromView(taxiView)
      return TaxiDocument(types = setOf(typeDeclaration), services = emptySet())
   }

   fun typeFromView(taxiView: View): ObjectType {
      val firstDefinition = taxiView.viewBodyDefinitions!!.first()
      val taxiFields = (firstDefinition.viewBodyType!! as ObjectType).fields.map { field ->
         Field(name = field.name, type = field.type, compilationUnit = CompilationUnit.unspecified(), nullable = true)
      }

      return ObjectType(
         taxiView.qualifiedName,
         ObjectTypeDefinition(
            inheritsFrom = taxiView.inheritsFrom,
            fields = taxiFields.toSet(),
            compilationUnit = CompilationUnit.unspecified(),
            annotations = setOf(Annotation("Generated")),
            typeDoc = "Generated by Taxi View."
         )
      )
   }
}

fun Any?.mapSqlValue(): String {
   return when (this) {
      null -> "null"
      is String -> this.quoted("'")
      else -> this.toString()
   }
}
