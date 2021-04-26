package io.vyne.cask.ddl.views.taxiViews

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.schemas.Schema
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.AndExpression
import lang.taxi.types.AssignmentExpression
import lang.taxi.types.CalculatedModelAttributeFieldSetExpression
import lang.taxi.types.ComparisonExpression
import lang.taxi.types.ComparisonOperand
import lang.taxi.types.ComparisonOperator
import lang.taxi.types.ConditionalAccessor
import lang.taxi.types.ConstantEntity
import lang.taxi.types.ElseMatchExpression
import lang.taxi.types.Field
import lang.taxi.types.InlineAssignmentExpression
import lang.taxi.types.LiteralAssignment
import lang.taxi.types.ModelAttributeFieldReferenceEntity
import lang.taxi.types.ModelAttributeTypeReferenceAssignment
import lang.taxi.types.NullAssignment
import lang.taxi.types.ObjectType
import lang.taxi.types.OrExpression
import lang.taxi.types.QualifiedName
import lang.taxi.types.ScalarAccessorValueAssignment
import lang.taxi.types.Type
import lang.taxi.types.View
import lang.taxi.types.WhenCaseBlock
import lang.taxi.types.WhenFieldSetCondition
import java.io.StringReader

class WhenStatementGenerator(private val taxiView: View,
                             private val objectType: ObjectType,
                             private val qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>,
                             private val schema: Schema) {
   fun toWhenSql(viewFieldDefinition: Field): String {
      return when (val accessor = viewFieldDefinition.accessor) {
         is ConditionalAccessor -> {
            val whenFieldSetCondition = accessor.expression as WhenFieldSetCondition
            val caseBody = whenFieldSetCondition
               .cases
               .map { caseBlock -> processWhenCaseMatchExpression(taxiView, objectType, caseBlock, qualifiedNameToCaskConfig) }
            CaseSql(caseBody).toString()
         }

         is FunctionAccessor ->  WindowFunctionStatementGenerator.windowFunctionToSql(accessor) { memberSource, memberType ->
            columnName(taxiView, memberSource, memberType, qualifiedNameToCaskConfig)
         }

         else -> PostgresDdlGenerator.toSqlNull(viewFieldDefinition.type)
      }

   }

   private fun processWhenCaseMatchExpression(
      taxiView: View,
      objectType: ObjectType,
      caseBlock: WhenCaseBlock,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): WhenSql {
      val caseExpression = caseBlock.matchExpression
      val assignments = caseBlock.assignments
      val assignmentSql = processAssignments(taxiView, objectType, assignments, qualifiedNameToCaskConfig)
      return when (caseExpression) {
         is ComparisonExpression -> WhenSql(processComparisonExpression(taxiView, caseExpression, qualifiedNameToCaskConfig), assignmentSql)
         is AndExpression -> WhenSql("${processComparisonExpression(taxiView, caseExpression = caseExpression.left as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)} AND " +
            "${processComparisonExpression(taxiView, caseExpression = caseExpression.right as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)}", assignmentSql)
         is OrExpression -> WhenSql("${processComparisonExpression(taxiView, caseExpression = caseExpression.left as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)} OR " +
            "${processComparisonExpression(taxiView, caseExpression = caseExpression.right as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)}", assignmentSql)
         is ElseMatchExpression -> WhenSql(null, assignmentSql)
         // this is also covered by compiler.
         else -> throw IllegalArgumentException("caseExpression should be a Logical Entity")
      }

   }

   private fun processAssignments(
      taxiView: View,
      objectType: ObjectType,
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
         is ModelAttributeTypeReferenceAssignment -> {
            columnName(taxiView, expression.source, expression.type, qualifiedNameToCaskConfig)
         }
         is LiteralAssignment -> expression.value.mapSqlValue()
         is NullAssignment -> "null"
         is ScalarAccessorValueAssignment -> {
            when (val accessor = expression.accessor) {
               is ConditionalAccessor -> {
                  val expression = accessor.expression
                  if (expression is CalculatedModelAttributeFieldSetExpression) {
                     val op1 = expression.operand1
                     val op2 = expression.operand2
                     if (isSourceTypeView(op1.memberSource, qualifiedNameToCaskConfig) || isSourceTypeView(op2.memberSource, qualifiedNameToCaskConfig)) {
                        // Case for
                        // (OrderSent::RequestedQuantity - OrderView::CumulativeQuantity)
                        if (isSourceTypeView(op1.memberSource, qualifiedNameToCaskConfig) && isSourceTypeView(op2.memberSource, qualifiedNameToCaskConfig)) {
                           // Case for
                           // (OrderView::RequestedQuantity - OrderView::CumulativeQuantity)
                           val op1ViewField = getViewField(op1.memberType)
                           val op2ViewField = getViewField(op2.memberType)
                           if (op1ViewField.accessor != null && op2ViewField.accessor != null) {
                              throw IllegalArgumentException("Unsupported assignment for a when case! - Both ${op1.memberSource.typeName}::${op1.memberType.toQualifiedName().typeName} and Both ${op2.memberSource.typeName}::${op2
                                 .memberType.toQualifiedName().typeName} has accessors")
                           }
                        }

                        val (viewField, otherOpAndItsOrder) = if (isSourceTypeView(op1.memberSource, qualifiedNameToCaskConfig)) {
                           getViewField(op1.memberType) to Pair(op2, BinaryOpOrder.Second)
                        } else {
                            getViewField(op2.memberType) to Pair(op1, BinaryOpOrder.First)
                        }
                        val fieldSql =  this.toWhenSql(viewField)
                        val caseSql = CaseSql.fromString(fieldSql)
                        val (otherOp, otherOpOrder) = otherOpAndItsOrder
                        val otherOpSql = columnName(taxiView, otherOp.memberSource, otherOp.memberType, qualifiedNameToCaskConfig)
                        return if (caseSql != null) {

                          val modifiedWhenSqls =  when(otherOpOrder) {
                              BinaryOpOrder.First -> caseSql.whenSqls.map {
                                 WhenSql(it.antecedent, "$otherOpSql ${expression.operator.symbol} ${it.consequent}")
                              }
                              BinaryOpOrder.Second -> caseSql.whenSqls.map {
                                 WhenSql(it.antecedent, "${it.consequent} ${expression.operator.symbol} $otherOpSql")
                              }
                           }
                           CaseSql(modifiedWhenSqls).toString()
                        } else {
                           when(otherOpOrder) {
                              BinaryOpOrder.First ->  "$otherOpSql ${expression.operator.symbol} $fieldSql"
                              BinaryOpOrder.Second -> "$fieldSql ${expression.operator.symbol} $otherOpSql"
                           }
                        }
                     } else {
                        val op1Sql = columnName(taxiView, op1.memberSource, op1.memberType, qualifiedNameToCaskConfig)
                        val op2Sql = columnName(taxiView, op2.memberSource, op2.memberType, qualifiedNameToCaskConfig)
                        "$op1Sql ${expression.operator.symbol} $op2Sql"
                     }
                  } else {
                     throw IllegalArgumentException("Unsupported assignment for a when case!")
                  }
               }
               is FunctionAccessor -> {
                  WindowFunctionStatementGenerator.windowFunctionToSql(accessor) { memberSource, memberType ->
                     columnName(taxiView, memberSource, memberType, qualifiedNameToCaskConfig)
                  }
               }
               else -> throw IllegalArgumentException("Unsupported assignment for a when case!")
            }
         }
         else -> throw IllegalArgumentException("Unsupported assignment for a when case!")
      }
   }

   private fun processComparisonExpression(
      taxiView: View,
      caseExpression: ComparisonExpression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      val lhs = processComparisonOperand(taxiView, caseExpression.left, qualifiedNameToCaskConfig)
      val rhs = processComparisonOperand(taxiView, caseExpression.right, qualifiedNameToCaskConfig)
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
      taxiView: View,
      operand: ComparisonOperand,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      return when (operand) {
         // SourceType:FieldType
         is ModelAttributeFieldReferenceEntity -> columnName(taxiView, operand.source, operand.fieldType, qualifiedNameToCaskConfig)
         // "partial"
         is ConstantEntity -> operand.value.mapSqlValue()
         else -> throw IllegalArgumentException("operand should be a ViewFindFieldReferenceEntity")
      }
   }

   private fun isSourceTypeView(sourceType: QualifiedName, qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): Boolean {
      return qualifiedNameToCaskConfig[sourceType]?.second?.tableName == null
   }

   private fun columnName(
      taxiView: View,
      sourceType: QualifiedName,
      fieldType: Type,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): String {
      val sourceTableName = qualifiedNameToCaskConfig[sourceType]?.second?.tableName
      return if (sourceTableName == null) {
         // View::Type case.
         val viewField = getViewField(fieldType)
         if (viewField.accessor == null) {
            sqlForViewFieldWithoutAnAccessor(taxiView, viewField)
         } else {
            this.toWhenSql(viewField)

         }
      } else {
         "$sourceTableName.${PostgresDdlGenerator.toColumnName(getField(sourceType, fieldType))}"
      }
   }

   // finds the field with the given type in View definition.
   private fun getViewField(targetFieldType: Type): Field {
      return objectType.fields.first { fieldType ->
         (fieldType.type.formattedInstanceOfType ?: fieldType.type) == targetFieldType
      }

   }

   private fun getField(sourceType: QualifiedName, fieldType: Type): Field {
      val objectType = schema.type(sourceType.fullyQualifiedName).taxiType as ObjectType
      return objectType.fields.first { field ->
         field.type == fieldType || (field.type.format != null && field.type.formattedInstanceOfType == fieldType)
      }
   }

   fun sqlForViewFieldWithoutAnAccessor(taxiView: View, viewFieldDefinition: Field): String {
      return if (viewFieldDefinition.memberSource == null) {
         //case for:
         // fieldName: FieldType case.
         PostgresDdlGenerator.selectNullAs(viewFieldDefinition.name, viewFieldDefinition.type)
      } else {
          columnName(
            taxiView,
            viewFieldDefinition.memberSource!!,
            viewFieldDefinition.type, qualifiedNameToCaskConfig)
      }
   }
}


data class CaseSql(val whenSqls: List<WhenSql>) {
   /*
     Generates:
    case
            when OrderFill_tb."tradeNo" = null then OrderFill_tb."executedQuantity"
            else sumOver(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."fillOrderId"   ORDER BY OrderFill_tb."tradeNo")
    end
    */
   override fun toString(): String {
      val builder = StringBuilder()
      builder.appendln("case")
      builder.append(whenSqls.joinToString("\n") { it.toString()})
      builder.appendln("")
      builder.append("end")
      return builder.toString()
   }
   companion object {
      fun fromString(caseSql: String): CaseSql? {
         val lines = StringReader(caseSql).readLines()
         val whereLines = lines.drop(1).dropLast(1)
         val whenSqls = whereLines.map { WhenSql.fromString(it) }
         if (whenSqls.any { it == null }) {
            return null
         }

         return CaseSql(whereLines.map { WhenSql.fromString(it)!! })
      }
   }
}

/**
 * Represents, either: (antecedent != null case)
 *  when OrderFill_tb."tradeNo" = null then OrderFill_tb."executedQuantity"
 * or: (antecedent == null case)
 *  else sumOver(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."fillOrderId"   ORDER BY OrderFill_tb."tradeNo")
 */
data class WhenSql(val antecedent: String?, val consequent: String) {
   override fun toString(): String {
      return if (antecedent == null) {
         "else $consequent"
      } else {
         "when $antecedent then $consequent"
      }
   }
   companion object {
      fun fromString(whenSql: String): WhenSql? {
         if (whenSql.contains("else") && whenSql.contains("where") && !whenSql.contains("then")) {
            return null
         }

         return if (whenSql.contains("else")) {
            WhenSql(null, whenSql.replace("else ", ""))
         } else {
            val parts = whenSql.replace("when ", "").split("then ")
            WhenSql(parts[0], parts[1])
         }
      }
   }
}

enum class BinaryOpOrder {
   First,
   Second
}
