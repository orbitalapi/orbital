package io.vyne.cask.ddl.views.taxiViews

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.schemas.Schema
import lang.taxi.accessors.ConditionalAccessor
import lang.taxi.expressions.Expression
import lang.taxi.expressions.FunctionExpression
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.vyne.aggregations.SumOver
import lang.taxi.types.AssignmentExpression
import lang.taxi.types.ElseMatchExpression
import lang.taxi.types.Field
import lang.taxi.types.FormulaOperator
import lang.taxi.types.InlineAssignmentExpression
import lang.taxi.types.ModelAttributeReferenceSelector
import lang.taxi.types.ObjectType
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import lang.taxi.types.View
import lang.taxi.types.WhenCaseBlock
import lang.taxi.types.WhenFieldSetCondition
import net.sf.jsqlparser.expression.AnalyticExpression
import net.sf.jsqlparser.expression.CaseExpression
import net.sf.jsqlparser.expression.NullValue
import net.sf.jsqlparser.expression.WhenClause
import net.sf.jsqlparser.expression.operators.relational.EqualsTo
import net.sf.jsqlparser.expression.operators.relational.GreaterThan
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression
import net.sf.jsqlparser.expression.operators.relational.MinorThan
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo
import net.sf.jsqlparser.parser.CCJSqlParserUtil.parseExpression
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.expression.Expression as SqlExpression

class WhenStatementGenerator(
   private val taxiView: View,
   private val objectType: ObjectType,
   private val qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>,
   private val schema: Schema
) {
   fun toWhenSql(viewFieldDefinition: Field): SqlExpression {
      val accessor = viewFieldDefinition.accessor
      return when {
         accessor is ConditionalAccessor -> {
            when (val fieldSetExpression = accessor.expression) {
               is WhenFieldSetCondition -> {
                  val expressions = fieldSetExpression
                     .cases
                     .map { caseBlock ->
                        processWhenCaseMatchExpression(
                           taxiView,
                           caseBlock,
                           qualifiedNameToCaskConfig
                        )
                     }
                  CaseExpression()
                     .addWhenClauses(expressions.filterIsInstance(WhenClause::class.java))
                     .withElseExpression(
                        expressions.minus(expressions.filterIsInstance(WhenClause::class.java)).first()
                     )
               }

               else -> throw IllegalArgumentException("${accessor.expression} is not supported in views")
            }
         }

         accessor is OperatorExpression && accessor.lhs is ModelAttributeReferenceSelector && accessor.rhs is ModelAttributeReferenceSelector ->
            processCalculatedModelAttributeFieldSetExpression(accessor)

         accessor is FunctionAccessor -> {
            processFunctionAccessor(accessor)
         }

         else -> {
            val sqlNullStatement = PostgresDdlGenerator.toSqlNull(viewFieldDefinition.type)
            parseExpression(sqlNullStatement, true)

         }
      }
   }

   /**
    * Produces SQL Statement for Logical Expressions
    */
   private fun processLogicalExpression(
      logicalOperatorExpression: OperatorExpression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>,
      sqlExpressionGenerator: (lhs: SqlExpression, rhs: SqlExpression) -> SqlExpression
   ): SqlExpression {
      val left = logicalOperatorExpression.lhs
      val right = logicalOperatorExpression.rhs
      val leftOperatorExpression = (left as? OperatorExpression)
      val rightOperatorExpression = (right as? OperatorExpression)
      return when {
         leftOperatorExpression != null && rightOperatorExpression != null && leftOperatorExpression.operator.isComparisonOperator() && rightOperatorExpression.operator.isComparisonOperator() -> {
            val andLhsComparisonExpression = processComparisonExpression(
               taxiView,
               caseExpression = left,
               qualifiedNameToCaskConfig = qualifiedNameToCaskConfig
            )
            val andRhsComparisonExpression = processComparisonExpression(
               taxiView,
               caseExpression = right,
               qualifiedNameToCaskConfig = qualifiedNameToCaskConfig
            )
            sqlExpressionGenerator(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         leftOperatorExpression != null && rightOperatorExpression != null && leftOperatorExpression.operator.isComparisonOperator() && rightOperatorExpression.operator == FormulaOperator.LogicalAnd -> {
            val andLhsComparisonExpression = processComparisonExpression(
               taxiView,
               caseExpression = left,
               qualifiedNameToCaskConfig = qualifiedNameToCaskConfig
            )
            val andRhsComparisonExpression = processAndExpression(right, qualifiedNameToCaskConfig)
            sqlExpressionGenerator(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         leftOperatorExpression != null && rightOperatorExpression != null && leftOperatorExpression.operator.isComparisonOperator() && rightOperatorExpression.operator == FormulaOperator.LogicalOr -> {
            val andLhsComparisonExpression = processComparisonExpression(
               taxiView,
               caseExpression = left,
               qualifiedNameToCaskConfig = qualifiedNameToCaskConfig
            )
            val andRhsComparisonExpression = processOrExpression(right, qualifiedNameToCaskConfig)
            sqlExpressionGenerator(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         rightOperatorExpression != null && rightOperatorExpression.operator.isComparisonOperator() && leftOperatorExpression != null && leftOperatorExpression.operator == FormulaOperator.LogicalAnd -> {
            val andRhsComparisonExpression = processComparisonExpression(
               taxiView,
               caseExpression = right,
               qualifiedNameToCaskConfig = qualifiedNameToCaskConfig
            )
            val andLhsComparisonExpression = processAndExpression(left, qualifiedNameToCaskConfig)
            sqlExpressionGenerator(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         rightOperatorExpression != null && rightOperatorExpression.operator.isComparisonOperator() && leftOperatorExpression != null && leftOperatorExpression.operator == FormulaOperator.LogicalOr -> {
            val andRhsComparisonExpression = processComparisonExpression(
               taxiView,
               caseExpression = right,
               qualifiedNameToCaskConfig = qualifiedNameToCaskConfig
            )
            val andLhsComparisonExpression = processOrExpression(left, qualifiedNameToCaskConfig)
            sqlExpressionGenerator(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         else -> throw IllegalArgumentException("unexpected expression whilst processing AndExpression => $left and $right")
      }

   }

   /**
    * Processes AndExpression for Lhs of a when expression:
    * Foo::Bar <Comparison_op> Baz::FieldType && Foo::Bar1 <Comparison_op> Baz::FieldType1
    */
   private fun processAndExpression(
      andOperatorExpression: OperatorExpression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>
   ): SqlExpression {
      return processLogicalExpression(andOperatorExpression, qualifiedNameToCaskConfig) { lhs, rhs ->
         net.sf.jsqlparser.expression.operators.conditional.AndExpression(lhs, rhs)
      }
   }

   /**
    * Processes OrExpression for Lhs of a when expression:
    * Foo::Bar <Comparison_op> Baz::FieldType || Foo::Bar1 <Comparison_op> Baz::FieldType1
    */
   private fun processOrExpression(
      caseExpression: OperatorExpression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>
   ): SqlExpression {
      return processLogicalExpression(caseExpression, qualifiedNameToCaskConfig) { lhs, rhs ->
         net.sf.jsqlparser.expression.operators.conditional.OrExpression(lhs, rhs)
      }
   }

   /**
    * Process a when case.
    * Allowed Cases:
    * Foo::Bar <Comparison_op> Baz::FieldType -> RHS // ComparisonExpression
    * Foo::Bar <Comparison_op> Baz::FieldType && Foo::Bar1 <Comparison_op> Baz::FieldType1 -> RHS // AndExpression
    * Foo::Bar <Comparison_op> Baz::FieldType || Foo::Bar1 <Comparison_op> Baz::FieldType1 -> RHS // OrExpression
    * else -> RHS // ElseMatchExpression
    */
   private fun processWhenCaseMatchExpression(
      taxiView: View,
      caseBlock: WhenCaseBlock,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>
   ): SqlExpression {
      val matchExpression = caseBlock.matchExpression
      val assignments = caseBlock.assignments
      val assignmentSql = processAssignments(taxiView, assignments, qualifiedNameToCaskConfig)
      val andOperatorExpression = (matchExpression as? OperatorExpression)?.operator == FormulaOperator.LogicalAnd
      val orOperatorExpression = (matchExpression as? OperatorExpression)?.operator == FormulaOperator.LogicalOr
      return when {
         andOperatorExpression -> {
            val and = processAndExpression(matchExpression as OperatorExpression, qualifiedNameToCaskConfig)
            WhenClause().withWhenExpression(and).withThenExpression(assignmentSql)
         }

         orOperatorExpression -> {
            val or = processOrExpression(matchExpression as OperatorExpression, qualifiedNameToCaskConfig)
            WhenClause().withWhenExpression(or).withThenExpression(assignmentSql)
         }

         matchExpression is OperatorExpression -> {
            val comparisonExpression = processComparisonExpression(taxiView, matchExpression, qualifiedNameToCaskConfig)
            WhenClause().withWhenExpression(comparisonExpression).withThenExpression(assignmentSql)
         }

         matchExpression is ElseMatchExpression -> assignmentSql
         // this is also covered by compiler.
         else -> throw IllegalArgumentException("caseExpression should be a Logical Entity")
      }

   }

   /**
    * Process the right hand side of ->
    * Allowed Expressions
    *   ->  Foo::Bar  // ModelAttributeTypeReferenceAssignment
    *   -> 'foo'      // LiteralAssignment
    *   -> enumType.EnumValue // EnumValueAssignment
    *   -> Foo::Bar operator Baz::FieldType // ScalarAccessorValueAssignment
    *   -> null // NullAssignment
    */
   private fun processAssignments(
      taxiView: View,
      assignments: List<AssignmentExpression>,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>
   ): SqlExpression {
      if (assignments.size != 1) {
         throw IllegalArgumentException("only 1 assignment is supported for a when case!")
      }

      val assignment = assignments.first()

      if (assignment !is InlineAssignmentExpression) {
         throw IllegalArgumentException("only inline assignment is supported for a when case!")
      }

      val expression = assignment.assignment
      return when {
         expression is ModelAttributeReferenceSelector -> {
            columnName(taxiView, expression.memberSource, expression.targetType, qualifiedNameToCaskConfig)
         }

         expression is LiteralExpression -> {
            val literalExpression = parseExpression(expression.value.mapSqlValue(), true)
            literalExpression
         }

         expression is OperatorExpression && expression.lhs is ModelAttributeReferenceSelector && expression.rhs is ModelAttributeReferenceSelector ->
            processCalculatedModelAttributeFieldSetExpression(expression)

         expression is FunctionExpression -> {
            val accessor = expression.function
            processFunctionAccessor(accessor)
         }

         else -> throw IllegalArgumentException("Unsupported assignment for a when case!")
      }
   }

   private fun processFunctionAccessor(accessor: FunctionAccessor): SqlExpression {
      return when (accessor.function.toQualifiedName()) {
         SumOver.name -> {
            val aggregateStatement =
               FunctionStatementGenerator.windowFunctionToSql(accessor) { memberSource, memberType ->
                  columnName(taxiView, memberSource, memberType, qualifiedNameToCaskConfig).toString()
               }
            parseExpression(aggregateStatement, true)
         }

         lang.taxi.functions.stdlib.Coalesce.name -> {
            val coalesceStatement =
               FunctionStatementGenerator.coalesceFunctionToSql(accessor) { memberSource, memberType ->
                  columnName(taxiView, memberSource, memberType, qualifiedNameToCaskConfig).toString()

               }
            parseExpression(coalesceStatement, true)
         }

         else -> throw IllegalArgumentException("Illegal Function Accessor only sumOver and coalesce are allowed.")
      }
   }

   /**
    * Processing for the 'THEN' part, i.e. right hand-side of '->' operator when rhs contains a calculation:
    *      Foo::Bar != null -> Foo::Bar - View::FieldType
    */
   private fun processCalculatedModelAttributeFieldSetExpression(accessorExpression: OperatorExpression): SqlExpression {
      // if the accessorExpression is => Foo::Bar - View::FieldType
      // op1 =>  Foo::Bar
      val op1 = accessorExpression.lhs as ModelAttributeReferenceSelector
      // op2 => View::FieldType
      val op2 = accessorExpression.rhs as ModelAttributeReferenceSelector
      // Check to see either op1 or op2 refers to a field in 'View' (e.g. View::FieldType)
      val isViewOp1 = isSourceTypeView(op1.memberSource, qualifiedNameToCaskConfig)
      val isViewOp2 = isSourceTypeView(op2.memberSource, qualifiedNameToCaskConfig)
      return when {
         /**
          * Case for:
          *  View::FieldType1 - View::FieldType2
          */
         isViewOp1 && isViewOp2 -> {
            processCalculatedModelAttributeFieldSetExpressionWithViewFieldsOnly(accessorExpression)
         }

         /**
          * Case for:
          *   View::FieldType1 - Model::FieldType
          */
         isViewOp1 || !isViewOp2 -> {
            val viewField = getViewField(op1.targetType)
            processCalculatedModelAttributeFieldSetExpressionWithOneViewFieldOperand(
               viewField,
               op2,
               BinaryOpOrder.Second,
               accessorExpression
            )
         }

         /**
          * Case for:
          * Model::FieldType - View::FieldType1
          */
         !isViewOp1 || isViewOp2 -> {
            val viewField = getViewField(op2.targetType)
            processCalculatedModelAttributeFieldSetExpressionWithOneViewFieldOperand(
               viewField,
               op1,
               BinaryOpOrder.First,
               accessorExpression
            )
         }

         //Neither operands refer to a view field, so they must refer to models in 'find' expressions.
         else -> {
            val op1Sql = columnName(taxiView, op1.memberSource, op1.targetType, qualifiedNameToCaskConfig)
            val op2Sql = columnName(taxiView, op2.memberSource, op2.targetType, qualifiedNameToCaskConfig)
            val sqlString = "$op1Sql ${accessorExpression.operator.symbol} $op2Sql"
            parseExpression(sqlString, true)
         }
      }
   }

   /**
    *   Processes right hand side of a when expression for:
    *   View::FieldType1 - View::FieldType2
    */
   private fun processCalculatedModelAttributeFieldSetExpressionWithViewFieldsOnly(accessorExpression: OperatorExpression): SqlExpression {
      // Case for
      // (OrderView::RequestedQuantity - OrderView::CumulativeQuantity)
      // if the accessorExpression is => Foo::Bar - View::FieldType
      val op1 = accessorExpression.lhs as ModelAttributeReferenceSelector
      val op2 = accessorExpression.rhs as ModelAttributeReferenceSelector
      val op1ViewField = getViewField(op1.targetType)
      val op2ViewField = getViewField(op2.targetType)
      if (op1ViewField.accessor != null && op2ViewField.accessor != null) {
         val expression1 = this.toWhenSql(op1ViewField)
         val expression2 = this.toWhenSql(op2ViewField)
         if (expression1 is AnalyticExpression && expression2 is AnalyticExpression) {
            val combinedExpressionSql =
               "$expression1 ${accessorExpression.operator.symbol}  ($expression2)"
            return parseExpression(combinedExpressionSql, true)
         }
         val case2Sql = this.toWhenSql(op2ViewField) as CaseExpression
         if (expression1 is AnalyticExpression) {
            val whens = mutableListOf<WhenClause>()
            case2Sql.whenClauses.map {
               val thenSqlStr = "$expression1 ${accessorExpression.operator.symbol} ${it.thenExpression}"
               WhenClause().withWhenExpression(it.whenExpression).withThenExpression(parseExpression(thenSqlStr, true))
            }
            val elseExpressionString = "$expression1 ${accessorExpression.operator.symbol} (${case2Sql.elseExpression})"
            return CaseExpression().withWhenClauses(whens)
               .withElseExpression(parseExpression(elseExpressionString, true))
         }
         val case1Sql = expression1 as CaseExpression

         val whens = mutableListOf<WhenClause>()
         case1Sql.whenClauses.map { when1Clause ->
            case2Sql.whenClauses.forEach { when2Clause ->
               val andExpression = net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                  when1Clause.whenExpression,
                  when2Clause.whenExpression
               )
               val thenExpressionString =
                  "${when1Clause.thenExpression} ${accessorExpression.operator.symbol} (${when2Clause.thenExpression})"
               val thenExpression = parseExpression(thenExpressionString, true)
               whens.add(WhenClause().withWhenExpression(andExpression).withThenExpression(thenExpression))
               val elseExpressionString =
                  "${case1Sql.elseExpression} ${accessorExpression.operator.symbol} (${when2Clause.thenExpression})"
               whens.add(
                  WhenClause().withWhenExpression(when2Clause.whenExpression)
                     .withThenExpression(parseExpression(elseExpressionString, true))
               )
            }
            val elseExpressionString =
               "${when1Clause.thenExpression} ${accessorExpression.operator.symbol} (${case2Sql.elseExpression})"
            whens.add(
               WhenClause().withWhenExpression(when1Clause.whenExpression)
                  .withThenExpression(parseExpression(elseExpressionString, true))
            )
         }

         return CaseExpression().withWhenClauses(whens).withElseExpression(
            parseExpression(
               "${case1Sql.elseExpression} ${accessorExpression.operator.symbol} (${case2Sql.elseExpression})",
               true
            )
         )
      } else {
         TODO()
      }
   }


   /**
    *   Processes right hand side of a when expression for:
    *
    *   Model::FieldType - View::FieldType
    *   or
    *   View::FieldType - Mode::FieldType
    */
   private fun processCalculatedModelAttributeFieldSetExpressionWithOneViewFieldOperand(
      viewField: Field,
      otherOp: ModelAttributeReferenceSelector,
      otherOpOrder: BinaryOpOrder,
      accessorExpression: OperatorExpression
   ): SqlExpression {
      val fieldSql = this.toWhenSql(viewField)
      val caseSql = fieldSql as? CaseExpression
      val otherOpSql = columnName(taxiView, otherOp.memberSource, otherOp.targetType, qualifiedNameToCaskConfig)
      return if (caseSql != null) {
         val modifiedWhenSqlStatements = when (otherOpOrder) {
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

         CaseExpression().withWhenClauses(modifiedWhenSqlStatements)
            .withElseExpression(parseExpression(elseExpressionString, true))
      } else {
         val sqlString = when (otherOpOrder) {
            BinaryOpOrder.First -> "$otherOpSql ${accessorExpression.operator.symbol} $fieldSql"
            BinaryOpOrder.Second -> "$fieldSql ${accessorExpression.operator.symbol} $otherOpSql"
         }
         parseExpression(sqlString, true)
      }
   }

   private fun processComparisonExpression(
      taxiView: View,
      caseExpression: OperatorExpression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>
   ): SqlExpression {
      val lhs = processComparisonOperand(taxiView, caseExpression.lhs, qualifiedNameToCaskConfig)
      val rhs = processComparisonOperand(taxiView, caseExpression.rhs, qualifiedNameToCaskConfig)
      return when (caseExpression.operator) {
         FormulaOperator.Equal -> {
            if (rhs is NullValue) {
               IsNullExpression().withLeftExpression(lhs)
            } else {
               EqualsTo(lhs, rhs)
            }
         }

         FormulaOperator.NotEqual -> {
            if (rhs is NullValue) {
               IsNullExpression().withLeftExpression(lhs).withNot(true)
            } else {
               NotEqualsTo(lhs, rhs)
            }
         }

         FormulaOperator.LessThanOrEqual -> MinorThanEquals().withLeftExpression(lhs).withRightExpression(rhs)
         FormulaOperator.GreaterThan -> GreaterThan().withLeftExpression(lhs).withRightExpression(rhs)
         FormulaOperator.GreaterThanOrEqual -> GreaterThanEquals().withLeftExpression(lhs).withRightExpression(rhs)
         FormulaOperator.LessThan -> MinorThan().withLeftExpression(lhs).withRightExpression(rhs)
         else -> throw IllegalArgumentException("unexpected Formula Operator ${caseExpression.operator}")
      }
   }

   private fun processComparisonOperand(
      taxiView: View,
      operand: Expression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>
   ): SqlExpression {

      return when (operand) {
         // SourceType:FieldType
         is ModelAttributeReferenceSelector -> {
            columnName(taxiView, operand.memberSource, operand.targetType, qualifiedNameToCaskConfig)
         }

         is LiteralExpression -> {
            parseExpression(operand.value.mapSqlValue(), true)
         }
         // "partial"
         /**
         is ConstantEntity -> {
         parseExpression(operand.value.mapSqlValue(), true)
         }
          */
         else -> throw IllegalArgumentException("operand should be a ViewFindFieldReferenceEntity")
      }
   }

   /**
    * given
    *
    * model Foo {
    *
    * }
    *
    * view FooView {
    *
    * }
    *
    * @return true if sourceType is FooView::FieldType otherwise false.
    */
   private fun isSourceTypeView(
      sourceType: QualifiedName,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>
   ): Boolean {
      return qualifiedNameToCaskConfig[sourceType]?.second?.tableName == null
   }

   private fun columnName(
      taxiView: View,
      sourceType: QualifiedName,
      fieldType: Type,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>
   ): SqlExpression {
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

   /**
    * finds the field with the given type in View definition.
    */
   private fun getViewField(targetFieldType: Type): Field {
      return objectType.fields.first { fieldType ->
         fieldType.type == targetFieldType
      }

   }

   private fun getField(sourceType: QualifiedName, fieldType: Type): Field {
      val objectType = schema.type(sourceType.fullyQualifiedName).taxiType as ObjectType
      return objectType.fields.first { field -> field.type == fieldType }
   }

   private fun sqlForViewFieldWithoutAnAccessor(taxiView: View, viewFieldDefinition: Field): SqlExpression {
      return if (viewFieldDefinition.memberSource == null) {
         //case for:
         // fieldName: FieldType case.
         val nullAsSql = PostgresDdlGenerator.selectNullAs(viewFieldDefinition.name, viewFieldDefinition.type)
         parseExpression(nullAsSql, true)
      } else {
         columnName(
            taxiView,
            viewFieldDefinition.memberSource!!,
            viewFieldDefinition.type, qualifiedNameToCaskConfig
         )
      }
   }
}

/**
 * A helper Enum to define the order of the operands in a binary expression
 */
enum class BinaryOpOrder {
   First,
   Second
}
