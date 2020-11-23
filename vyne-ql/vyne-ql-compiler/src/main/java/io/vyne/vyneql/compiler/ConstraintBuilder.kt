package io.vyne.vyneql.compiler

import arrow.core.Either
import io.vyne.VyneQLParser
import lang.taxi.CompilationError
import lang.taxi.NamespaceQualifiedTypeResolver
import lang.taxi.TaxiDocument
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.PropertyToParameterConstraintAst
import lang.taxi.services.operations.constraints.PropertyToParameterConstraintProvider
import lang.taxi.types.AttributePath
import lang.taxi.types.Type
import lang.taxi.utils.flattenErrors
import lang.taxi.utils.invertEitherList

class ConstraintBuilder(schema:TaxiDocument, private val typeResolver: NamespaceQualifiedTypeResolver) {
   private val propertyToParameterConstraintProvider = PropertyToParameterConstraintProvider()

   fun build(parameterConstraintExpressionList: VyneQLParser.ParameterConstraintExpressionListContext,
             type: Type
   ): Either<List<CompilationError>, List<Constraint>> {
      val constraints: Either<List<CompilationError>, List<Constraint>> = parameterConstraintExpressionList.parameterConstraintExpression().map { constraintExpression ->
         val ast = parseToAst(constraintExpression.propertyToParameterConstraintExpression())
         propertyToParameterConstraintProvider.build(ast, type, typeResolver, constraintExpression)
      }.invertEitherList().flattenErrors()
      return constraints
   }

   /**
    * We use an AST for parsing constraints as we need to support these in both
    * Taxi and VyneQL.  However, the Antlr generation tree makes using the same code
    * in both contexts very difficult, as it regenerates the classes in a different namespace.
    * This means even though the classes are generated from the exact same source,
    * we can't pass them into the constraint provider code.
    * Therefore, we parse a simple AST, which makes reuse possible
    */
   @Suppress("DuplicatedCode")
   private fun parseToAst(constraint: VyneQLParser.PropertyToParameterConstraintExpressionContext): PropertyToParameterConstraintAst {
      val astLhs = constraint.propertyToParameterConstraintLhs().let { lhs ->
         val typeOrFieldQualifiedName = lhs.qualifiedName().asDotJoinedPath()
         val propertyFieldNameQualifier = lhs.propertyFieldNameQualifier()?.text
         PropertyToParameterConstraintAst.AstLhs(propertyFieldNameQualifier, typeOrFieldQualifiedName)
      }

      val astRhs = constraint.propertyToParameterConstraintRhs().let { rhs ->
         val literal = rhs.literal()?.value()
         val attributePath = rhs.qualifiedName()?.asDotJoinedPath()?.let { AttributePath.from(it) }
         PropertyToParameterConstraintAst.AstRhs(literal, attributePath)
      }

      val operator = constraint.comparisonOperator().text
      return PropertyToParameterConstraintAst(astLhs, operator, astRhs)
   }
}

private fun VyneQLParser.QualifiedNameContext.asDotJoinedPath(): String {
   return this.Identifier().joinToString(".") { it.text }
}

fun VyneQLParser.LiteralContext.value(): Any {
   return when {
      this.StringLiteral() != null -> {
         // can be either ' or "
         val firstChar = this.StringLiteral().text.toCharArray()[0]
         this.StringLiteral().text.trim(firstChar)
      }
      this.IntegerLiteral() != null -> this.IntegerLiteral().text.toInt()
      this.BooleanLiteral() != null -> this.BooleanLiteral().text!!.toBoolean()
      else -> TODO()
//      this.IntegerLiteral() != null -> this.IntegerLiteral()
   }
}
