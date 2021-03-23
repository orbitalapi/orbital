package io.vyne.cask.ddl.views

import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.PostgresDdlGenerator
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
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import lang.taxi.types.OrExpression
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import lang.taxi.types.View
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
      val sqlViewName =viewSqlName(taxiView)
      val firstViewBodyDefinition = taxiView.viewBodyDefinitions!!.first()
      val selectStatement = firstViewBodyDefinition.viewBodyTypeDefinition!!.fields.map {field ->
         selectStatement(field, tableNamesForSourceTypes)
      }.joinToString(", \n")
      val createSqlBuilder = StringBuilder()
      createSqlBuilder
         .appendln("create or replace view $sqlViewName as")
         .appendln("select")
         .appendln(selectStatement)
      createSqlBuilder.appendln(" from ${tableNamesForSourceTypes[firstViewBodyDefinition.bodyType.toQualifiedName()]!!.second.tableName}")
      return listOf(dropViewStatement(sqlViewName), createSqlBuilder.toString())
   }

   private fun fetchTableNamesForParticipatingTypes(view: View): Map<QualifiedName, Pair<QualifiedName, CaskConfig>> {
      val sourceTypeNames = mutableListOf<QualifiedName>()
      view.viewBodyDefinitions?.map {
         sourceTypeNames.add((it.bodyType.toQualifiedName()))
         it.joinType?.let { joinType -> sourceTypeNames.add(joinType.toQualifiedName()) }
      }

      return CaskViewBuilder.caskConfigsForQualifiedNames(sourceTypeNames, this.caskConfigRepository).associateBy { keySelection ->
         keySelection.first
      }
   }

   private fun selectStatement(
      viewFieldDefinition: ViewBodyFieldDefinition,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      if (viewFieldDefinition.accessor != null) {
         val expression = viewFieldDefinition.accessor!!.expression as WhenFieldSetCondition
         val caseBody = expression.cases.joinToString("\n") { caseBlock -> processWhenCaseMatchExpression(caseBlock) }
         return StringBuilder()
            .appendln("case")
            .appendln(caseBody)
            .appendln("end as ${PostgresDdlGenerator.toColumnName(viewFieldDefinition.fieldName)}")
            .toString()
      }
      return if (viewFieldDefinition.sourceType == viewFieldDefinition.fieldType) {
         //case for:
         // fieldName: FieldType case.
         PostgresDdlGenerator.selectNullAs(viewFieldDefinition.fieldName)
      } else {
         // orderId: OrderSent.SentOrderId
         val objectType = viewFieldDefinition.sourceType as ObjectType
         val sourceField = getField(viewFieldDefinition.sourceType, viewFieldDefinition.fieldType)
         PostgresDdlGenerator.selectAs(sourceField, viewFieldDefinition.fieldName)
      }
   }

   private fun getField(sourceType: Type, fieldType: Type): Field {
      val objectType = sourceType as ObjectType
      return objectType.fields.first { field -> field.type == fieldType }
   }

   private fun processWhenCaseMatchExpression(caseBlock: WhenCaseBlock): String {
      val caseExpression = caseBlock.matchExpression
      val assignments = caseBlock.assignments
      val assignmentSql = processAssignments(assignments)
      return when (caseExpression) {
         is ComparisonExpression -> "when ${processComparisonExpression(caseExpression)} then $assignmentSql"
         is AndExpression -> "when ${processComparisonExpression(caseExpression = caseExpression.left as ComparisonExpression)} AND " +
            "${processComparisonExpression(caseExpression = caseExpression.right as ComparisonExpression)} then $assignmentSql"
         is OrExpression -> "when ${processComparisonExpression(caseExpression = caseExpression.left as ComparisonExpression)} OR " +
            "${processComparisonExpression(caseExpression = caseExpression.right as ComparisonExpression)} then $assignmentSql"
         is ElseMatchExpression -> "else $assignmentSql"
         else -> throw IllegalArgumentException("caseExpression should be a Logical Entity")
      }

   }

   private fun processAssignments(assignments: List<AssignmentExpression>): String {
      if (assignments.size != 1) {
         throw IllegalArgumentException("only 1 assignment is supported for a when case!")
      }

      val assignment = assignments.first()

      if (assignment !is InlineAssignmentExpression) {
         throw IllegalArgumentException("only inline assignment is supported for a when case!")
      }

      return when (val expression = assignment.assignment) {
         is ViewFindFieldReferenceAssignment -> PostgresDdlGenerator.toColumnName(getField(expression.type, expression.fieldType))
         is LiteralAssignment -> expression.value.mapSqlValue()
         else -> throw IllegalArgumentException("Unsupported assignment for a when case!")

      }
   }

   private fun processComparisonExpression(caseExpression: ComparisonExpression): String {
      val lhs = processComparisonOperand(caseExpression.left)
      val rhs = processComparisonOperand(caseExpression.right)
      return when (caseExpression.operator) {
         ComparisonOperator.EQ -> "$lhs = $rhs"
         ComparisonOperator.NQ -> "$lhs <> $rhs"
         ComparisonOperator.LE -> "$lhs <= $rhs"
         ComparisonOperator.GT -> "$lhs > $rhs"
         ComparisonOperator.GE -> "$lhs >= $rhs"
         ComparisonOperator.LT -> "$lhs < $rhs"
      }


   }

   private fun processComparisonOperand(operand: ComparisonOperand): String {
      return when (operand) {
         is ViewFindFieldReferenceEntity -> PostgresDdlGenerator.toColumnName(getField(operand.sourceType, operand.fieldType))
         is ConstantEntity -> operand.value.mapSqlValue()
         else -> throw IllegalArgumentException("operand should be a ViewFindFieldReferenceEntity")

      }
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
      return config
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
      val schema = schemaStore.schemaSet().schema
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
            typeDoc = "Generated by Taxi View.}"
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
