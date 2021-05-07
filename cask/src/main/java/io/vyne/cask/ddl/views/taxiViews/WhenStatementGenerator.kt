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
import net.sf.jsqlparser.expression.CaseExpression
import net.sf.jsqlparser.expression.Expression
import net.sf.jsqlparser.expression.NullValue
import net.sf.jsqlparser.expression.WhenClause
import net.sf.jsqlparser.expression.operators.relational.EqualsTo
import net.sf.jsqlparser.expression.operators.relational.GreaterThan
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals
import net.sf.jsqlparser.expression.operators.relational.MinorThan
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo
import net.sf.jsqlparser.parser.CCJSqlParserUtil.parseExpression
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.schema.Table

class WhenStatementGenerator(private val taxiView: View,
                             private val objectType: ObjectType,
                             private val qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>,
                             private val schema: Schema) {
   fun toWhenSql(viewFieldDefinition: Field): Expression {
      return when (val accessor = viewFieldDefinition.accessor) {
         is ConditionalAccessor -> {
            val whenFieldSetCondition = accessor.expression as WhenFieldSetCondition
            val expressions = whenFieldSetCondition
               .cases
               .map { caseBlock -> processWhenCaseMatchExpression(taxiView, objectType, caseBlock, qualifiedNameToCaskConfig) }
            CaseExpression()
               .addWhenClauses(expressions.filterIsInstance(WhenClause::class.java))
               .withElseExpression(expressions.minus(expressions.filterIsInstance(WhenClause::class.java)).first())
         }

         is FunctionAccessor -> {
            val aggregateStatement = WindowFunctionStatementGenerator.windowFunctionToSql(accessor) { memberSource, memberType ->
               columnName(taxiView, memberSource, memberType, qualifiedNameToCaskConfig).toString()
            }
            parseExpression(aggregateStatement, true)
         }

         else -> {
            val sqlNullStatement = PostgresDdlGenerator.toSqlNull(viewFieldDefinition.type)
            parseExpression(sqlNullStatement, true)

         }
      }
   }

   private fun processWhenCaseMatchExpression(
      taxiView: View,
      objectType: ObjectType,
      caseBlock: WhenCaseBlock,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): Expression {
      val caseExpression = caseBlock.matchExpression
      val assignments = caseBlock.assignments
      val assignmentSql = processAssignments(taxiView, objectType, assignments, qualifiedNameToCaskConfig)
      return when (caseExpression) {
         is ComparisonExpression -> {
            val comparisonExpression = processComparisonExpression(taxiView, caseExpression, qualifiedNameToCaskConfig)
            WhenClause().withWhenExpression(comparisonExpression).withThenExpression(assignmentSql)
            //   WhenSql(binaryExpression.toString(), assignmentSql)
         }
         is AndExpression -> {
            val andLhsComparisionExpression = processComparisonExpression(taxiView, caseExpression = caseExpression.left as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val andRhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = caseExpression.right as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val and = net.sf.jsqlparser.expression.operators.conditional.AndExpression(andLhsComparisionExpression, andRhsComparisonExpression)
            WhenClause().withWhenExpression(and).withThenExpression(assignmentSql)
         }

         is OrExpression -> {
            val orLhsComparisionExpression = processComparisonExpression(taxiView, caseExpression = caseExpression.left as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val orRhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = caseExpression.right as ComparisonExpression, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val or = net.sf.jsqlparser.expression.operators.conditional.OrExpression(orLhsComparisionExpression, orRhsComparisonExpression)
            WhenClause().withWhenExpression(or).withThenExpression(assignmentSql)

         }
         is ElseMatchExpression -> assignmentSql
         // this is also covered by compiler.
         else -> throw IllegalArgumentException("caseExpression should be a Logical Entity")
      }

   }

   private fun processAssignments(
      taxiView: View,
      objectType: ObjectType,
      assignments: List<AssignmentExpression>,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): Expression {
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
         is LiteralAssignment -> {
            val literalExpression = parseExpression(expression.value.mapSqlValue(), true)
            literalExpression
         }
         is NullAssignment -> {
            NullValue()
            //"null"
         }
         is ScalarAccessorValueAssignment -> {
            this.processScalarValueAssignment(expression)
         }
         else -> throw IllegalArgumentException("Unsupported assignment for a when case!")
      }
   }

   private fun processScalarValueAssignment(expression: ScalarAccessorValueAssignment): Expression {
      return when (val accessor = expression.accessor) {
         is ConditionalAccessor -> {
            val accessorExpression = accessor.expression
            if (accessorExpression is CalculatedModelAttributeFieldSetExpression) {
               val op1 = accessorExpression.operand1
               val op2 = accessorExpression.operand2
               if (isSourceTypeView(op1.memberSource, qualifiedNameToCaskConfig) || isSourceTypeView(op2.memberSource, qualifiedNameToCaskConfig)) {
                  // Case for
                  // (OrderSent::RequestedQuantity - OrderView::CumulativeQuantity)
                  if (isSourceTypeView(op1.memberSource, qualifiedNameToCaskConfig) && isSourceTypeView(op2.memberSource, qualifiedNameToCaskConfig)) {
                     // Case for
                     // (OrderView::RequestedQuantity - OrderView::CumulativeQuantity)
                     val op1ViewField = getViewField(op1.memberType)
                     val op2ViewField = getViewField(op2.memberType)
                     if (op1ViewField.accessor != null && op2ViewField.accessor != null) {
                        val case1Sql = this.toWhenSql(op1ViewField) as CaseExpression
                        val case2Sql = this.toWhenSql(op2ViewField) as CaseExpression
                        val whens = mutableListOf<WhenClause>()
                        case1Sql.whenClauses.map { when1Clause ->
                           case2Sql.whenClauses.forEach { when2Clause ->
                              val andExpresssion = net.sf.jsqlparser.expression.operators.conditional.AndExpression(when1Clause.whenExpression, when2Clause.whenExpression)
                              val thenExpressionString = "${when1Clause.thenExpression} ${accessorExpression.operator.symbol} ${when2Clause.thenExpression}"
                              val thenExpression = parseExpression(thenExpressionString, true)
                              whens.add(WhenClause().withWhenExpression(andExpresssion).withThenExpression(thenExpression))
                              val elseExpressionString = "${when2Clause.thenExpression} ${accessorExpression.operator.symbol} ${case1Sql.elseExpression}"
                              whens.add(WhenClause().withWhenExpression(when2Clause.whenExpression).withThenExpression(parseExpression(elseExpressionString, true)))
                           }
                           val elseExpressionString = "${when1Clause.thenExpression} ${accessorExpression.operator.symbol} ${case2Sql.elseExpression}"
                           whens.add(WhenClause().withWhenExpression(when1Clause.whenExpression).withThenExpression(parseExpression(elseExpressionString, true)))
                        }

                        return CaseExpression().withWhenClauses(whens).withElseExpression(
                           parseExpression("${case1Sql.elseExpression} ${accessorExpression.operator.symbol} ${case2Sql.elseExpression}", true)
                        )
                     }
                  }

                  val (viewField, otherOpAndItsOrder) = if (isSourceTypeView(op1.memberSource, qualifiedNameToCaskConfig)) {
                     getViewField(op1.memberType) to Pair(op2, BinaryOpOrder.Second)
                  } else {
                     getViewField(op2.memberType) to Pair(op1, BinaryOpOrder.First)
                  }
                  val fieldSql = this.toWhenSql(viewField)
                  val caseSql = fieldSql as? CaseExpression
                  val (otherOp, otherOpOrder) = otherOpAndItsOrder
                  val otherOpSql = columnName(taxiView, otherOp.memberSource, otherOp.memberType, qualifiedNameToCaskConfig)
                  return if (caseSql != null) {
                     val modifiedWhenSqls = when (otherOpOrder) {
                        BinaryOpOrder.First -> caseSql.whenClauses.map {
                           val thenSqlStr = "$otherOpSql ${accessorExpression.operator.symbol} ${it.thenExpression}"
                           WhenClause().withWhenExpression(it.whenExpression).withThenExpression(parseExpression(thenSqlStr, true))
                        }
                        BinaryOpOrder.Second -> caseSql.whenClauses.map {
                           val thenSqlStr = "${it.thenExpression} ${accessorExpression.operator.symbol} $otherOpSql"
                           WhenClause().withWhenExpression(it.whenExpression).withThenExpression(parseExpression(thenSqlStr, true))
                        }
                     }

                     val elseExpressionString = if (otherOpOrder == BinaryOpOrder.First) {
                        "$otherOpSql ${accessorExpression.operator.symbol} ${caseSql.elseExpression}"
                     } else {
                        "${caseSql.elseExpression} ${accessorExpression.operator.symbol} $otherOpSql"
                     }

                     CaseExpression().withWhenClauses(modifiedWhenSqls).withElseExpression(parseExpression(elseExpressionString, true))
                  } else {
                     val sqlString = when (otherOpOrder) {
                        BinaryOpOrder.First -> "$otherOpSql ${accessorExpression.operator.symbol} $fieldSql"
                        BinaryOpOrder.Second -> "$fieldSql ${accessorExpression.operator.symbol} $otherOpSql"
                     }
                     parseExpression(sqlString, true)
                  }
               } else {
                  val op1Sql = columnName(taxiView, op1.memberSource, op1.memberType, qualifiedNameToCaskConfig)
                  val op2Sql = columnName(taxiView, op2.memberSource, op2.memberType, qualifiedNameToCaskConfig)
                  val sqlString = "$op1Sql ${accessorExpression.operator.symbol} $op2Sql"
                  parseExpression(sqlString, true)
               }
            } else {
               throw IllegalArgumentException("Unsupported assignment for a when case!")
            }
         }
         is FunctionAccessor -> {
            val sqlString = WindowFunctionStatementGenerator.windowFunctionToSql(accessor) { memberSource, memberType ->
               columnName(taxiView, memberSource, memberType, qualifiedNameToCaskConfig).toString()
            }
            parseExpression(sqlString, true)
         }
         else -> throw IllegalArgumentException("Unsupported assignment for a when case!")
      }
   }

   private fun processComparisonExpression(
      taxiView: View,
      caseExpression: ComparisonExpression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): net.sf.jsqlparser.expression.operators.relational.ComparisonOperator {
      val lhs = processComparisonOperand(taxiView, caseExpression.left, qualifiedNameToCaskConfig)
      val rhs = processComparisonOperand(taxiView, caseExpression.right, qualifiedNameToCaskConfig)
      return when (caseExpression.operator) {
         ComparisonOperator.EQ -> EqualsTo(lhs, rhs)
         ComparisonOperator.NQ -> NotEqualsTo(lhs, rhs)
         ComparisonOperator.LE -> MinorThanEquals().withLeftExpression(lhs).withRightExpression(rhs)
         ComparisonOperator.GT -> GreaterThan().withLeftExpression(lhs).withRightExpression(rhs)
         ComparisonOperator.GE -> GreaterThanEquals().withLeftExpression(lhs).withRightExpression(rhs)
         ComparisonOperator.LT -> MinorThan().withLeftExpression(lhs).withRightExpression(rhs)
      }
   }

   private fun processComparisonOperand(
      taxiView: View,
      operand: ComparisonOperand,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): Expression {

      return when (operand) {
         // SourceType:FieldType
         is ModelAttributeFieldReferenceEntity -> {
            columnName(taxiView, operand.source, operand.fieldType, qualifiedNameToCaskConfig)
         }
         // "partial"
         is ConstantEntity -> {
            parseExpression(operand.value.mapSqlValue(), true)
         }
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
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): Expression {
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
         val columnName = PostgresDdlGenerator.toColumnName(getField(sourceType, fieldType))
         Column(Table(sourceTableName), columnName)
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

   private fun sqlForViewFieldWithoutAnAccessor(taxiView: View, viewFieldDefinition: Field): Expression {
      return if (viewFieldDefinition.memberSource == null) {
         //case for:
         // fieldName: FieldType case.
         val nullAsSql = PostgresDdlGenerator.selectNullAs(viewFieldDefinition.name, viewFieldDefinition.type)
         parseExpression(nullAsSql, true)
      } else {
         columnName(
            taxiView,
            viewFieldDefinition.memberSource!!,
            viewFieldDefinition.type, qualifiedNameToCaskConfig)
      }
   }
}

enum class BinaryOpOrder {
   First,
   Second
}
