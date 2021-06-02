package io.vyne.cask.ddl.views.taxiViews

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.schemas.Schema
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.vyne.aggregations.SumOver
import lang.taxi.types.AndExpression
import lang.taxi.types.AssignmentExpression
import lang.taxi.types.CalculatedModelAttributeFieldSetExpression
import lang.taxi.types.ComparisonExpression
import lang.taxi.types.ComparisonOperand
import lang.taxi.types.ComparisonOperator
import lang.taxi.types.ConditionalAccessor
import lang.taxi.types.ConstantEntity
import lang.taxi.types.ElseMatchExpression
import lang.taxi.types.EnumValueAssignment
import lang.taxi.types.Field
import lang.taxi.types.InlineAssignmentExpression
import lang.taxi.types.LiteralAssignment
import lang.taxi.types.ModelAttributeFieldReferenceEntity
import lang.taxi.types.ModelAttributeReferenceSelector
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
import net.sf.jsqlparser.expression.AnalyticExpression
import net.sf.jsqlparser.expression.CaseExpression
import net.sf.jsqlparser.expression.Expression
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

class WhenStatementGenerator(private val taxiView: View,
                             private val objectType: ObjectType,
                             private val qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>,
                             private val schema: Schema) {
   fun toWhenSql(viewFieldDefinition: Field): Expression {
      return when (val accessor = viewFieldDefinition.accessor) {
         is ConditionalAccessor -> {
            when (val fieldSetExpression = accessor.expression) {
               is WhenFieldSetCondition -> {
                  val expressions = fieldSetExpression
                     .cases
                     .map { caseBlock -> processWhenCaseMatchExpression(taxiView, caseBlock, qualifiedNameToCaskConfig) }
                  CaseExpression()
                     .addWhenClauses(expressions.filterIsInstance(WhenClause::class.java))
                     .withElseExpression(expressions.minus(expressions.filterIsInstance(WhenClause::class.java)).first())
               }
               is CalculatedModelAttributeFieldSetExpression -> {
                  processCalculatedModelAttributeFieldSetExpression(fieldSetExpression)
               }
               else -> throw IllegalArgumentException("${accessor.expression} is not supported in views")
            }
         }

         is FunctionAccessor -> {
            when (accessor.function.toQualifiedName()) {
               SumOver.name -> {
                  val aggregateStatement = FunctionStatementGenerator.windowFunctionToSql(accessor) { memberSource, memberType ->
                     columnName(taxiView, memberSource, memberType, qualifiedNameToCaskConfig).toString()
                  }
                  parseExpression(aggregateStatement, true)
               }

               lang.taxi.functions.stdlib.Coalesce.name -> {
                  val coalesceStatement = FunctionStatementGenerator.coalesceFunctionToSql(accessor) { memberSource, memberType ->
                     columnName(taxiView, memberSource, memberType, qualifiedNameToCaskConfig).toString()

                  }
                  parseExpression(coalesceStatement, true)
               }
               else -> throw IllegalArgumentException("Illegal Function Accessor only sumOver and coalesce are allowed.")
            }
         }

         else -> {
            val sqlNullStatement = PostgresDdlGenerator.toSqlNull(viewFieldDefinition.type)
            parseExpression(sqlNullStatement, true)

         }
      }
   }

   /**
    * Processes AndExpression for Lhs of a when expression:
    * Foo::Bar <Comparison_op> Baz::FieldType && Foo::Bar1 <Comparison_op> Baz::FieldType1
    */
   private fun processAndExpression(
      caseExpression: AndExpression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): Expression {
      val left = caseExpression.left
      val right = caseExpression.right

      return when {
         left is ComparisonExpression && right is ComparisonExpression -> {
            val andLhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = left, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val andRhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = right, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            net.sf.jsqlparser.expression.operators.conditional.AndExpression(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         left is ComparisonExpression && right is AndExpression -> {
            val andLhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = left, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val andRhsComparisonExpression = processAndExpression(right, qualifiedNameToCaskConfig)
            net.sf.jsqlparser.expression.operators.conditional.AndExpression(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         left is ComparisonExpression && right is OrExpression -> {
            val andLhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = left, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val andRhsComparisonExpression = processOrExpression(right, qualifiedNameToCaskConfig)
            net.sf.jsqlparser.expression.operators.conditional.AndExpression(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         right is ComparisonExpression && left is AndExpression -> {
            val andRhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = right, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val andLhsComparisonExpression = processAndExpression(left, qualifiedNameToCaskConfig)
            net.sf.jsqlparser.expression.operators.conditional
               .AndExpression(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         right is ComparisonExpression && left is OrExpression -> {
            val andRhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = right, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val andLhsComparisonExpression = processOrExpression(left, qualifiedNameToCaskConfig)
             net.sf.jsqlparser.expression.operators.conditional.AndExpression(andLhsComparisonExpression, andRhsComparisonExpression)
         }
         else -> throw IllegalArgumentException("unexpected expression whilst processing AndExpression => $left and $right")
      }
   }

   /**
    * Processes OrExpression for Lhs of a when expression:
    * Foo::Bar <Comparison_op> Baz::FieldType || Foo::Bar1 <Comparison_op> Baz::FieldType1
    */
   private fun processOrExpression(
      caseExpression: OrExpression,
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): Expression {
      val left = caseExpression.left
      val right = caseExpression.right

      return when {
         left is ComparisonExpression && right is ComparisonExpression -> {
            val orLhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = left, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val orRhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = right, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            net.sf.jsqlparser.expression.operators.conditional.OrExpression(orLhsComparisonExpression, orRhsComparisonExpression)
         }

         left is ComparisonExpression && right is AndExpression -> {
            val orLhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = left, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val orRhsComparisonExpression = processAndExpression(right, qualifiedNameToCaskConfig)
            net.sf.jsqlparser.expression.operators.conditional.OrExpression(orLhsComparisonExpression, orRhsComparisonExpression)
         }

         left is ComparisonExpression && right is OrExpression -> {
            val orLhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = left, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val orRhsComparisonExpression = processOrExpression(right, qualifiedNameToCaskConfig)
            net.sf.jsqlparser.expression.operators.conditional.OrExpression(orLhsComparisonExpression, orRhsComparisonExpression)
         }

         right is ComparisonExpression && left is AndExpression -> {
            val andRhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = right, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val andLhsComparisonExpression = processAndExpression(left, qualifiedNameToCaskConfig)
            net.sf.jsqlparser.expression.operators.conditional.AndExpression(andLhsComparisonExpression, andRhsComparisonExpression)
         }

         right is ComparisonExpression && left is OrExpression -> {
            val orRhsComparisonExpression = processComparisonExpression(taxiView, caseExpression = right, qualifiedNameToCaskConfig = qualifiedNameToCaskConfig)
            val orLhsComparisonExpression = processOrExpression(left, qualifiedNameToCaskConfig)
            net.sf.jsqlparser.expression.operators.conditional.OrExpression(orLhsComparisonExpression, orRhsComparisonExpression)
         }
         else -> throw IllegalArgumentException("unexpected expression whilst processing OrExpression => $left and $right")
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
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): Expression {
      val caseExpression = caseBlock.matchExpression
      val assignments = caseBlock.assignments
      val assignmentSql = processAssignments(taxiView, assignments, qualifiedNameToCaskConfig)
      return when (caseExpression) {
         is ComparisonExpression -> {
            val comparisonExpression = processComparisonExpression(taxiView, caseExpression, qualifiedNameToCaskConfig)
            WhenClause().withWhenExpression(comparisonExpression).withThenExpression(assignmentSql)
         }
         is AndExpression -> {
            val and = processAndExpression(caseExpression, qualifiedNameToCaskConfig)
            WhenClause().withWhenExpression(and).withThenExpression(assignmentSql)
         }
         is OrExpression -> {
            val or = processOrExpression(caseExpression, qualifiedNameToCaskConfig)
            WhenClause().withWhenExpression(or).withThenExpression(assignmentSql)
         }
         is ElseMatchExpression -> assignmentSql
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
         is EnumValueAssignment -> {
            parseExpression(expression.enumValue.value.mapSqlValue())
         }
         else -> throw IllegalArgumentException("Unsupported assignment for a when case!")
      }
   }

   /**
    * Processing for the 'THEN' part, i.e. right hand-side of '->' operator when rhs contains a calculation:
    *      Foo::Bar != null -> Foo::Bar - View::FieldType
    */
   private fun processCalculatedModelAttributeFieldSetExpression(accessorExpression: CalculatedModelAttributeFieldSetExpression): Expression {
      // if the accessorExpression is => Foo::Bar - View::FieldType
      // op1 =>  Foo::Bar
      val op1 = accessorExpression.operand1
      // op2 => View::FieldType
      val op2 = accessorExpression.operand2
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
            val viewField = getViewField(op1.memberType)
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
            val viewField = getViewField(op2.memberType)
            processCalculatedModelAttributeFieldSetExpressionWithOneViewFieldOperand(
               viewField,
               op1,
               BinaryOpOrder.First,
               accessorExpression
            )
         }

         //Neither operands refer to a view field, so they must refer to models in 'find' expressions.
         else -> {
            val op1Sql = columnName(taxiView, op1.memberSource, op1.memberType, qualifiedNameToCaskConfig)
            val op2Sql = columnName(taxiView, op2.memberSource, op2.memberType, qualifiedNameToCaskConfig)
            val sqlString = "$op1Sql ${accessorExpression.operator.symbol} $op2Sql"
            parseExpression(sqlString, true)
         }
      }
   }

   /**
    *   Processes right hand side of a when expression for:
    *   View::FieldType1 - View::FieldType2
    */
   private fun processCalculatedModelAttributeFieldSetExpressionWithViewFieldsOnly(accessorExpression: CalculatedModelAttributeFieldSetExpression): Expression {
      // Case for
      // (OrderView::RequestedQuantity - OrderView::CumulativeQuantity)
      // if the accessorExpression is => Foo::Bar - View::FieldType
      val op1 = accessorExpression.operand1
      val op2 = accessorExpression.operand2
      val op1ViewField = getViewField(op1.memberType)
      val op2ViewField = getViewField(op2.memberType)
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
            return CaseExpression().withWhenClauses(whens).withElseExpression(parseExpression(elseExpressionString, true))
         }
         val case1Sql = expression1 as CaseExpression

         val whens = mutableListOf<WhenClause>()
         case1Sql.whenClauses.map { when1Clause ->
            case2Sql.whenClauses.forEach { when2Clause ->
               val andExpression = net.sf.jsqlparser.expression.operators.conditional.AndExpression(when1Clause.whenExpression, when2Clause.whenExpression)
               val thenExpressionString = "${when1Clause.thenExpression} ${accessorExpression.operator.symbol} (${when2Clause.thenExpression})"
               val thenExpression = parseExpression(thenExpressionString, true)
               whens.add(WhenClause().withWhenExpression(andExpression).withThenExpression(thenExpression))
               val elseExpressionString = "${case1Sql.elseExpression} ${accessorExpression.operator.symbol} (${when2Clause.thenExpression})"
               whens.add(WhenClause().withWhenExpression(when2Clause.whenExpression).withThenExpression(parseExpression(elseExpressionString, true)))
            }
            val elseExpressionString = "${when1Clause.thenExpression} ${accessorExpression.operator.symbol} (${case2Sql.elseExpression})"
            whens.add(WhenClause().withWhenExpression(when1Clause.whenExpression).withThenExpression(parseExpression(elseExpressionString, true)))
         }

         return CaseExpression().withWhenClauses(whens).withElseExpression(
            parseExpression("${case1Sql.elseExpression} ${accessorExpression.operator.symbol} (${case2Sql.elseExpression})", true)
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
      accessorExpression: CalculatedModelAttributeFieldSetExpression): Expression {
      val fieldSql = this.toWhenSql(viewField)
      val caseSql = fieldSql as? CaseExpression
      val otherOpSql = columnName(taxiView, otherOp.memberSource, otherOp.memberType, qualifiedNameToCaskConfig)
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

         CaseExpression().withWhenClauses(modifiedWhenSqlStatements).withElseExpression(parseExpression(elseExpressionString, true))
      } else {
         val sqlString = when (otherOpOrder) {
            BinaryOpOrder.First -> "$otherOpSql ${accessorExpression.operator.symbol} $fieldSql"
            BinaryOpOrder.Second -> "$fieldSql ${accessorExpression.operator.symbol} $otherOpSql"
         }
         parseExpression(sqlString, true)
      }
   }

   /**
    * Processing for the 'THEN' part, i.e. right hand-side of '->' operator in a 'When' statement.
    */
   private fun processScalarValueAssignment(expression: ScalarAccessorValueAssignment): Expression {
      return when (val accessor = expression.accessor) {
         is ConditionalAccessor -> {
            val accessorExpression = accessor.expression
            if (accessorExpression is CalculatedModelAttributeFieldSetExpression) {
               processCalculatedModelAttributeFieldSetExpression(accessorExpression)
            } else {
               throw IllegalArgumentException("Unsupported assignment for a when case!")
            }
         }
         is FunctionAccessor -> {
            val sqlString = FunctionStatementGenerator.windowFunctionToSql(accessor) { memberSource, memberType ->
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
      qualifiedNameToCaskConfig: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>): Expression {
      val lhs = processComparisonOperand(taxiView, caseExpression.left, qualifiedNameToCaskConfig)
      val rhs = processComparisonOperand(taxiView, caseExpression.right, qualifiedNameToCaskConfig)
      return when (caseExpression.operator) {
         ComparisonOperator.EQ -> {
            if (rhs is NullValue) {
               IsNullExpression().withLeftExpression(lhs)
            } else {
               EqualsTo(lhs, rhs)
            }
         }
         ComparisonOperator.NQ -> {
            if (rhs is NullValue) {
               IsNullExpression().withLeftExpression(lhs).withNot(true)
            } else {
               NotEqualsTo(lhs, rhs)
            }
         }
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

   /**
    * finds the field with the given type in View definition.
    */
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

/**
 * A helper Enum to define the order of the operands in a binary expression
 */
enum class BinaryOpOrder {
   First,
   Second
}
