package io.vyne.cask.ddl.views

import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.schema
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.Type
import io.vyne.schemas.VersionedType
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
import lang.taxi.kapt.quoted
import lang.taxi.types.*
import lang.taxi.types.Annotation

private data class JoinTableSpec(val tableName: String, val fieldList: List<String>, val joinCriteria: String? = null) {
}

/**
 * Wraps logic for converting a CaskViewDefinition into both
 * DDL to generate against the schema, and the required taxi to expose as
 * a type.
 *
 * Turns out the logic to do those two things is fairly db-heavy and
 * intertwined, so have combined here.
 *
 * Also, have the viewSpec as a class attribute, to allow this to
 * cache expensive operations (compilation & db lookups)/
 */
class CaskViewBuilder(
   private val caskConfigRepository: CaskConfigRepository,
   private val schemaStore: SchemaStore,
   private val viewSpec: CaskViewDefinition
) {
   companion object {
      const val VIEW_PREFIX = "v_"
      fun dropViewStatement(viewTableName: String) = """drop view if exists $viewTableName;"""
      fun caskConfigsForQualifiedNames(qualifiedNames: List<QualifiedName>, caskConfigRepository: CaskConfigRepository): List<Pair<QualifiedName, CaskConfig>> {
         return qualifiedNames.map { qualifiedName ->
            qualifiedName to caskConfigRepository.findAllByQualifiedTypeName(qualifiedName.fullyQualifiedName)
         }.map { (qualifiedName, configs) ->
            val activeConfigs = configs.filter { it.status == CaskStatus.ACTIVE }
            if (configs.isNotEmpty() && activeConfigs.isEmpty()) {
               log().warn("Cask view for $qualifiedName cannot be created, as it has no active configs -- all are either replaced or migrating")
            }
            qualifiedName to activeConfigs
         }.mapNotNull { (qualifiedName, configs) ->
            if (configs.isEmpty()) {
               log().error("Type ${qualifiedName.parameterizedName} does not have any cask configs assigned.  This can happen if a view is defined before a cask is generated.  This will prevent cask views being generated. ")
               return@mapNotNull null
            }
            // MVP: Only support a single table (the most recently created)
            // per config)
            // In the future, as part of supporting upgrades/migrations,
            // we need to expand this to support all versions
            qualifiedName to configs.maxBy { it.insertedAt }!!
         }
      }

      // Cask now exposes functionality to query given cask by cask_message.insertedAt column.
      // This requires joining actual 'data' table to cask_message via  <data_table>.caskmessageid = cask_message.id.
      // Therefore we need to create caskmessageid column in view as well. Below we simply set the value of caskmessageid column of the
      // first table, but this is just an arbitraty decision for the moment as we can't come up with a better approach.
      fun caskMessageIdColumn(tableName: String) = """${tableName.quoted()}.${PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME} as ${PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME}"""
   }

   private val taxiWriter = SchemaWriter()
   private val tableConfigs: List<Pair<QualifiedName, CaskConfig>> by lazy { getCaskConfigs(viewSpec.join) }
   private val types: Map<QualifiedName, Type> by lazy { compileTypes(tableConfigs) }
   private val taxiTypes by lazy { types.mapValues { (_, vyneType) -> vyneType.taxiType as ObjectType } }
   private val viewTableName = "$VIEW_PREFIX${viewSpec.typeName.typeName}"

   fun generateCreateView(): List<String> {
      val join = viewSpec.join

      val tableNames: Map<QualifiedName, String> = tableConfigs.map { (qualifiedName, config) ->
         qualifiedName to config.tableName
      }.toMap()
      val preferredType = taxiTypes[join.types.first()]
      if (preferredType == null) {
         log().warn("No cask exists for type ${join.types.first()}.  This can happen if a view is defined before a cask is generated. Aborting view creation for view ${viewSpec.typeName}")
         return emptyList()
      }
      val filter = CaskViewFieldFilter(viewSpec.typeName, taxiTypes.values.toList(), preferredType)

      val tableList = join.types.mapIndexed { index, qualifiedName ->
         val thisType = types[qualifiedName]?.taxiType as ObjectType?
//            ?: error("$qualifiedName was not mapped to a type.  This shouldn't happen")
         if (thisType == null) {
            log().error("$qualifiedName was not mapped to a type. This suggests either the view has been incorrectly defined, or our schema is out of date.  Aborting generation of view.")
            return emptyList()
         }
         val thisTableName = tableNames[qualifiedName]
            ?: error("$qualifiedName was not mapped to a type.  This shouldn't happen")

         if (index == 0) {
            val fields = mapTableFields(thisType, filter, thisTableName)
            // Cask now exposes functionality to query given cask by cask_message.insertedAt column.
            // This requires joining actual 'data' table to cask_message via  <data_table>.caskmessageid = cask_message.id.
            // Therefore we need to create caskmessageid column in view as well. Below we simply set the value of caskmessageid column of the
            // first table, but this is just an arbitraty decision for the moment as we can't come up with a better approach.
            val caskMessageIdColumn = caskMessageIdColumn(thisTableName)
            JoinTableSpec(thisTableName, fields.plus(caskMessageIdColumn))
         } else {
            val previousTypeName = join.types[index - 1]
            val previousTableName = tableNames[previousTypeName] ?: error("No table defined for $previousTypeName")
            val previousType = types[previousTypeName]?.taxiType as ObjectType?
               ?: error("$previousTypeName was not mapped to a type.  This shouldn't happen")

            val fields = mapTableFields(thisType, filter, thisTableName)

            var errorInJoin = false
            val joinCriteria = join.joinOn.joinToString(separator = " and ") { joinExpression ->
               if (!previousType.hasField(joinExpression.leftField)) {
                  log().error("Cannot generate view ${viewSpec.typeName} because join references field ${joinExpression.leftField} which does not exist on type ${previousType.qualifiedName}")
                  errorInJoin = true
                  return@joinToString ""
               }
               if (!thisType.hasField(joinExpression.rightField)) {
                  log().error("Cannot generate view ${viewSpec.typeName} because join references field ${joinExpression.rightField} which does not exist on type ${thisType.qualifiedName}")
                  errorInJoin = true
                  return@joinToString ""
               }
               val leftFieldColumnName = PostgresDdlGenerator.toColumnName(previousType.field(joinExpression.leftField))
               val rightFieldColumnName = PostgresDdlGenerator.toColumnName(thisType.field(joinExpression.rightField))
               "${previousTableName.quoted()}.$leftFieldColumnName = ${thisTableName.quoted()}.$rightFieldColumnName"
            }

            if (errorInJoin) {
               return emptyList()
            }

            val joinStatement = "${join.kind.statement} ${thisTableName.quoted()} on $joinCriteria"
            JoinTableSpec(thisTableName, fields, joinStatement)
         }
      }

      val tableListStatement = tableList.joinToString("\n") { it.joinCriteria ?: it.tableName.quoted() }
      val whereClause = viewSpec.whereClause?.let { "WHERE ${convertWhereClause(it, taxiTypes.values.toList(), tableNames)}" }
         ?: ""
      // Note - for the view, we don't need to use the VersionedType,
      // since the view is a function of the underlying VersionedType's
      // Therefore, a QualifiedName for the reference types is sufficient.
      // (Hope I'm right about that).

      // LENS-343: Drop view first, to prevent errors about
      //
      val ddl = """create or replace view $viewTableName as
         |select ${if (viewSpec.distinct) "distinct" else ""}
         |${tableList.flatMap { it.fieldList }.joinToString(", \n")}
         |from
         |$tableListStatement
         |$whereClause;
      """.trimMargin()
         .trim()
      return listOf(dropViewStatement(viewTableName), ddl)
   }


   fun generateCaskConfig(): CaskConfig {
      val viewType = generateViewType()
      val config = CaskConfig.forType(
         viewType,
         viewTableName,
         // Views also expose a new type
         exposesType = true,
         exposesService = true
      )
      return config
   }

   private fun mapTableFields(thisType: ObjectType, filter: CaskViewFieldFilter, thisTableName: String): List<String> {
      val originalFields = filter.getOriginalFieldsForType(thisType)
      val renamedFields = filter.getRenamedFieldsForType(thisType)
      return originalFields.mapIndexed { index, originalField ->
         val renamedField = renamedFields[index]
         """${thisTableName.quoted()}.${PostgresDdlGenerator.toColumnName(originalField)} as ${PostgresDdlGenerator.toColumnName(renamedField)}"""
      }
   }

   internal fun convertWhereClause(clause: String, types: List<ObjectType>, tableNames: Map<QualifiedName, String>): String {
      // I hate regex.  Here's what this is doing:
      // trying to match patterns like test.Foo:fieldName or just Foo.fieldName
      // ((\w+)\.)* : Find word characters followed by a . (and permit 0-to-many groups of that)
      // followed by another grouping of word characters.
      // This is to match namespaced type names, and type names without namespaces
      // eg: test.Foo or Foo
      // Then look for ':'
      // Then look for another final group of word characters (the field name)
      val typeFieldNameRegex = """((\w+)\.)*(\w+):(\w+)""".toRegex()
      return typeFieldNameRegex.replace(clause) { matchResult ->
         val match = matchResult.value
         val (typeName, fieldName) = match.split(":")

         val type = types.firstOrNull { it.qualifiedName == typeName }
         if (type == null) {
            // We have to log here, rather than error, as some false matches are possible
            // against the regex.  For example 2020-03-05T22:30:00 will match
            log().warn("Criteria defined against type $typeName is not possible, because it's not present in the selected types")
            match
         } else {
            val tableName = tableNames[QualifiedName.from(typeName)]
               ?: error("Criteria defined against type $typeName is not possible, because it's not present in the selected types")
            if (!type.hasField(fieldName)) {
               log().warn("Criteria defined against type $typeName is not possible, becasue it does not expose a field called $fieldName")
               return@replace match
            }
            val columnName = PostgresDdlGenerator.toColumnName(type.field(fieldName))

            tableName.quoted() + "." + columnName
         }
      }
   }

   private fun generateViewType(): VersionedType {
      val taxiDoc = generateTaxi()
      val importSources = schemaStore.schemaSet().taxiSchemas
      val taxiSource = generateTaxiSource(taxiDoc)
      val schema = TaxiSchema.from(VersionedSource.sourceOnly(taxiSource), importSources)
      return schema.versionedType(viewSpec.typeName.toVyneQualifiedName())
   }

   fun generateTaxiSource(): String {
      val document = generateTaxi()
      return generateTaxiSource(document)
   }

   private fun generateTaxiSource(document: TaxiDocument): String {
      val schemas = taxiWriter.generateSchemas(listOf(document), importLocation = SchemaWriter.ImportLocation.CollectImports)
      return schemas.joinToString("\n")
   }

   private fun generateTaxi(): TaxiDocument {
      val schema = schemaStore.schemaSet().schema
      val inheritedTypes = viewSpec.inherits.map { inheritedTypeName ->
         schema.type(inheritedTypeName.fullyQualifiedName).taxiType
      }.toSet()

      val types = compileTypes(tableConfigs).values
         .map { it.taxiType as ObjectType }

      val filter = CaskViewFieldFilter(viewSpec.typeName, types, types[0])

      val typeDeclaration = ObjectType(
         viewSpec.typeName.fullyQualifiedName,
         ObjectTypeDefinition(
            inheritsFrom = inheritedTypes,
            fields = filter.getAllFieldsRenamed().toSet(),
            compilationUnit = CompilationUnit.unspecified(),
            annotations = setOf(Annotation("Generated")),
            typeDoc = "Generated by Cask.  Source types are ${types.joinToString(",") { it.qualifiedName }}"
         )
      )

      // TODO : Do we need to genreate imports here?
      return TaxiDocument(types = setOf(typeDeclaration), services = emptySet())

   }

   private fun compileTypes(tableConfigs: List<Pair<QualifiedName, CaskConfig>>): Map<QualifiedName, Type> {
      val types = tableConfigs.map { (qualifiedName, config) ->
         qualifiedName to config.schema().type(qualifiedName.fullyQualifiedName)
      }.toMap()
      return types
   }

   private fun getCaskConfigs(join: ViewJoin): List<Pair<QualifiedName, CaskConfig>> {
      return caskConfigsForQualifiedNames(join.types, caskConfigRepository)
   }
}
