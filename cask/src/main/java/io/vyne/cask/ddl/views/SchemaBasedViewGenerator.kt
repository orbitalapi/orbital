package io.vyne.cask.ddl.views

import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.cask.ddl.views.CaskViewBuilder.Companion.caskMessageIdColumn
import io.vyne.cask.ddl.views.CaskViewBuilder.Companion.dropViewStatement
import io.vyne.cask.services.DefaultCaskTypeProvider
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
import lang.taxi.kapt.quoted
import lang.taxi.types.AndExpression
import lang.taxi.types.Annotation
import lang.taxi.types.AssignmentExpression
import lang.taxi.types.ComparisonExpression
import lang.taxi.types.ComparisonOperand
import lang.taxi.types.ComparisonOperator
import lang.taxi.types.CompilationUnit
import lang.taxi.types.ConstantEntity
import lang.taxi.types.ElseMatchExpression
import lang.taxi.types.Field
import lang.taxi.types.InlineAssignmentExpression
import lang.taxi.types.LiteralAssignment
import lang.taxi.types.NullAssignment
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import lang.taxi.types.OrExpression
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import lang.taxi.types.View
import lang.taxi.types.ViewBodyDefinition
import lang.taxi.types.ViewBodyFieldDefinition
import lang.taxi.types.ViewFindFieldReferenceAssignment
import lang.taxi.types.ViewFindFieldReferenceEntity
import lang.taxi.types.WhenCaseBlock
import lang.taxi.types.WhenCaseMatchExpression
import lang.taxi.types.WhenFieldSetCondition
import lang.taxi.utils.quoted
import org.springframework.stereotype.Component

@Component
class SchemaBasedViewGenerator(private val caskConfigRepository: CaskConfigRepository,
                               private val schemaStore: SchemaStore) {

   private val taxiWriter = SchemaWriter()
   fun taxiViews() = schemaStore.schemaSet().schema.taxi.views

   fun generateDdl(taxiView: View): List<String> {
      val tableNamesForSourceTypes = fetchTableNamesForParticipatingTypes(taxiView)
      if (tableNamesForSourceTypes.isEmpty()) {
         return emptyList()
      }
      val sqlViewName =viewSqlName(taxiView)
      val viewBodyDefinitions = taxiView.viewBodyDefinitions!!
      val sqlViewDefinition = viewBodyDefinitions.joinToString(" union all \n") { viewBodyDefinition ->
         viewBodyDefinitionToSql(viewBodyDefinition, tableNamesForSourceTypes)
      }
      val createSqlBuilder = StringBuilder()
      createSqlBuilder
         .appendln("create or replace view $sqlViewName as")
         .appendln(sqlViewDefinition)

      return listOf(dropViewStatement(sqlViewName), createSqlBuilder.toString())
   }

   fun generateCaskConfig(taxiView: View): CaskConfig {
      val viewType = generateViewType(taxiView)
      val config = CaskConfig.forType(
         viewType,
         viewSqlName(taxiView),
         // Views also expose a new type
         exposesType = true,
         exposesService = true
      )
      return config.copy(versionHash = taxiView.definitionHash)
   }

   fun getDependencies(taxiView: View): List<QualifiedName> {
      return fetchTableNamesForParticipatingTypes(taxiView).keys.toList()
   }
   private fun viewBodyDefinitionToSql(
      viewBodyDefinition: ViewBodyDefinition,
      tableNamesForSourceTypes: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      val bodyTableName = tableNamesForSourceTypes[viewBodyDefinition.bodyType.toQualifiedName()]!!.second.tableName
      val sqlStatementsForEachField =viewBodyDefinition.viewBodyTypeDefinition!!.fields.map { field ->
         selectStatement(field, tableNamesForSourceTypes)
      }.plus(caskMessageIdColumn(bodyTableName))
      val sqlBuilder = StringBuilder()
      if (viewBodyDefinition.joinType == null) {
         sqlBuilder.appendln("select")
      } else {
         sqlBuilder.appendln("select distinct")
      }
      sqlBuilder.appendln(sqlStatementsForEachField.joinToString(", \n"))
      if (viewBodyDefinition.joinType == null) {
         sqlBuilder.appendln(" from $bodyTableName")
      } else {
         val mainTableName = tableNamesForSourceTypes[viewBodyDefinition.bodyType.toQualifiedName()]!!.second.tableName
         val joinTableName = tableNamesForSourceTypes[viewBodyDefinition.joinType!!.toQualifiedName()]!!.second.tableName
         val joinField1 = "$mainTableName.${PostgresDdlGenerator.toColumnName(viewBodyDefinition.joinInfo!!.mainField)}"
         val joinField2 = "$joinTableName.${PostgresDdlGenerator.toColumnName(viewBodyDefinition.joinInfo!!.joinField)}"
         sqlBuilder.appendln(" from $mainTableName LEFT JOIN $joinTableName ON $joinField1 = $joinField2")
      }
      return sqlBuilder.toString()
   }

   private fun fetchTableNamesForParticipatingTypes(view: View): Map<QualifiedName, Pair<QualifiedName, CaskConfig>> {
      val sourceTypeNames = mutableListOf<QualifiedName>()
      view.viewBodyDefinitions?.map {
         sourceTypeNames.add((it.bodyType.toQualifiedName()))
         it.joinType?.let { joinType -> sourceTypeNames.add(joinType.toQualifiedName()) }
      }

      val retValue =  CaskViewBuilder.caskConfigsForQualifiedNames(sourceTypeNames, this.caskConfigRepository).associateBy { keySelection ->
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
      viewFieldDefinition: ViewBodyFieldDefinition,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      if (viewFieldDefinition.accessor != null) {
         val expression = viewFieldDefinition.accessor!!.expression as WhenFieldSetCondition
         val caseBody = expression.cases.joinToString("\n") { caseBlock -> processWhenCaseMatchExpression(caseBlock, qualifiedNameToCaskConfig) }
         return StringBuilder()
            .appendln("case")
            .appendln(caseBody)
            .appendln("end as ${PostgresDdlGenerator.toColumnName(viewFieldDefinition.fieldName)}")
            .toString()
      }
      return if (viewFieldDefinition.sourceType == viewFieldDefinition.fieldType) {
         //case for:
         // fieldName: FieldType case.
         PostgresDdlGenerator.selectNullAs(viewFieldDefinition.fieldName, viewFieldDefinition.fieldType)
      } else {
         // orderId: OrderSent.SentOrderId
         val sourceField = columnName(viewFieldDefinition.sourceType, viewFieldDefinition.fieldType, qualifiedNameToCaskConfig)
         PostgresDdlGenerator.selectAs(sourceField, viewFieldDefinition.fieldName)
      }
   }

   private fun getField(sourceType: Type, fieldType: Type): Field {
      val objectType = sourceType as ObjectType
      return objectType.fields.first { field -> field.type == fieldType }
   }

   private fun processWhenCaseMatchExpression(
      caseBlock: WhenCaseBlock,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      val caseExpression = caseBlock.matchExpression
      val assignments = caseBlock.assignments
      val assignmentSql = processAssignments(assignments, qualifiedNameToCaskConfig)
      return when (caseExpression) {
         is ComparisonExpression -> "when ${processComparisonExpression(caseExpression, qualifiedNameToCaskConfig)} then $assignmentSql"
         is AndExpression -> "when ${processComparisonExpression(caseExpression = caseExpression.left as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)} AND " +
            "${processComparisonExpression(caseExpression = caseExpression.right as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)} then $assignmentSql"
         is OrExpression -> "when ${processComparisonExpression(caseExpression = caseExpression.left as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)} OR " +
            "${processComparisonExpression(caseExpression = caseExpression.right as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)} then $assignmentSql"
         is ElseMatchExpression -> "else $assignmentSql"
         // this is also covered by compiler.
         else -> throw IllegalArgumentException("caseExpression should be a Logical Entity")
      }

   }

   private fun columnName(
      sourceType: Type,
      fieldType: Type,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>> ): String {
      val sourceTableName = qualifiedNameToCaskConfig[sourceType.toQualifiedName()]!!.second.tableName
      val columnName = PostgresDdlGenerator.toColumnName(getField(sourceType, fieldType))
      return "$sourceTableName.$columnName"
   }

   private fun processAssignments(
      assignments: List<AssignmentExpression>,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      if (assignments.size != 1) {
         throw IllegalArgumentException("only 1 assignment is supported for a when case!")
      }

      val assignment = assignments.first()

      if (assignment !is InlineAssignmentExpression) {
         throw IllegalArgumentException("only inline assignment is supported for a when case!")
      }

      return when (val expression = assignment.assignment) {
         is ViewFindFieldReferenceAssignment -> columnName(expression.type, expression.fieldType, qualifiedNameToCaskConfig)
         is LiteralAssignment -> expression.value.mapSqlValue()
         is NullAssignment -> "null"
         // This is also covered by the compiler.
         else -> throw IllegalArgumentException("Unsupported assignment for a when case!")
      }
   }

   private fun processComparisonExpression(
      caseExpression: ComparisonExpression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      val lhs = processComparisonOperand(caseExpression.left, qualifiedNameToCaskConfig)
      val rhs = processComparisonOperand(caseExpression.right, qualifiedNameToCaskConfig)
      return when (caseExpression.operator) {
         ComparisonOperator.EQ -> "$lhs = $rhs"
         ComparisonOperator.NQ -> "$lhs <> $rhs"
         ComparisonOperator.LE -> "$lhs <= $rhs"
         ComparisonOperator.GT -> "$lhs > $rhs"
         ComparisonOperator.GE -> "$lhs >= $rhs"
         ComparisonOperator.LT -> "$lhs < $rhs"
      }


   }

   private fun processComparisonOperand(
      operand: ComparisonOperand,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      return when (operand) {
         is ViewFindFieldReferenceEntity -> columnName(operand.sourceType, operand.fieldType, qualifiedNameToCaskConfig)
         is ConstantEntity -> operand.value.mapSqlValue()
         else -> throw IllegalArgumentException("operand should be a ViewFindFieldReferenceEntity")

      }
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
      val taxiFields = firstDefinition.viewBodyTypeDefinition!!.fields.map { viewBodyFieldDefinition ->
         with(viewBodyFieldDefinition) {
            Field(name =  fieldName, type = this.fieldType, compilationUnit =  CompilationUnit.unspecified(), nullable = true)
         }
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
      is String -> (this!! as String).quoted("'")
      else -> this!!.toString()
   }
}
