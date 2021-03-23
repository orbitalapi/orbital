package io.vyne.cask.ddl.views

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.Schema
import io.vyne.utils.log
import lang.taxi.types.AndExpression
import lang.taxi.types.ComparisonExpression
import lang.taxi.types.ComparisonOperand
import lang.taxi.types.ComparisonOperator
import lang.taxi.types.ConstantEntity
import lang.taxi.types.ElseMatchExpression
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.OrExpression
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import lang.taxi.types.View
import lang.taxi.types.ViewBodyFieldDefinition
import lang.taxi.types.ViewFindFieldReferenceEntity
import lang.taxi.types.WhenCaseMatchExpression
import lang.taxi.types.WhenFieldSetCondition
import lang.taxi.utils.quoted
import org.springframework.stereotype.Component

@Component
class SchemaBasedViewGenerator(private val caskConfigRepository: CaskConfigRepository,
                               private val schemaStore: SchemaStore) {

   fun taxiViews() = schemaStore.schemaSet().schema.taxi.views

   fun generateDdl(taxiView: View): List<String>  {
      val tableNamesForSourceTypes = fetchTableNamesForParticipatingTypes(taxiView)
      taxiView.viewBodyDefinitions?.map { viewBodyDefinition ->
         if (viewBodyDefinition.joinType == null) {
            val selectStatements = viewBodyDefinition
               .viewBodyTypeDefinition
               ?.fields
               ?.map { field -> selectStatement(field)
            }
         }
      }
      return listOf()
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

   private fun selectStatement(viewFieldDefinition: ViewBodyFieldDefinition): String {
      if (viewFieldDefinition.accessor != null) {
         val expression = viewFieldDefinition.accessor!!.expression as WhenFieldSetCondition
         expression.cases.map { case ->
            val caseSqlStatement = processWhenCaseMatchExpression(case.matchExpression)
         }


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
      return objectType.fields.first { field -> field.type == fieldType}
   }

   private fun processWhenCaseMatchExpression(caseExpression: WhenCaseMatchExpression): String {
      return when(caseExpression) {
         is ComparisonExpression -> processComparisonExpression(caseExpression)
         is AndExpression -> "${processComparisonExpression(caseExpression = caseExpression.left as ComparisonExpression)} AND ${processComparisonExpression(caseExpression = caseExpression.right as ComparisonExpression)}"
         is OrExpression -> "${processComparisonExpression(caseExpression = caseExpression.left as ComparisonExpression)} OR ${processComparisonExpression(caseExpression = caseExpression.right as ComparisonExpression)}"
         else -> throw IllegalArgumentException("caseExpression should be a Logical Entity")
      }
   }

   private fun processComparisonExpression(caseExpression: ComparisonExpression): String {
      val lhs = processComparisonOperand(caseExpression.left)
      val rhs = processComparisonOperand(caseExpression.right)
      return when (caseExpression.operator) {
         ComparisonOperator.EQ ->  "$lhs = $rhs"
         ComparisonOperator.NQ -> "$lhs <> $rhs"
         ComparisonOperator.LE -> "$lhs <= $rhs"
         ComparisonOperator.GT -> "$lhs > $rhs"
         ComparisonOperator.GE -> "$lhs >= $rhs"
         ComparisonOperator.LT -> "$lhs < $rhs"
      }


   }

   private fun processComparisonOperand(operand: ComparisonOperand): String {
      return when (operand) {
         is ViewFindFieldReferenceEntity -> PostgresDdlGenerator.toColumnName(PostgresDdlGenerator.toColumnName(getField(operand.sourceType, operand.fieldType)))
         is ConstantEntity -> when(operand.value) {
            null -> "null"
            is String -> (operand.value!! as String).quoted("'")
            else -> operand.value!!.toString()
         }
         else -> throw IllegalArgumentException("operand should be a ViewFindFieldReferenceEntity")

      }
   }


   fun generateCaskConfig(taxiView: View): CaskConfig {
      TODO()
   }
}
