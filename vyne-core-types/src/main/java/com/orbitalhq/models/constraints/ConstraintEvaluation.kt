package com.orbitalhq.models.constraints

import com.orbitalhq.models.InPlaceQueryEngine
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.schemas.ConstraintEvaluation
import com.orbitalhq.schemas.ConstraintExpressionEvaluationFailed
import com.orbitalhq.schemas.ConstraintViolationValueUpdater
import com.orbitalhq.schemas.DefaultConstraintEvaluation
import com.orbitalhq.schemas.ReplaceFieldValueUpdater
import com.orbitalhq.schemas.ReplaceValueUpdater
import com.orbitalhq.schemas.Schema
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.services.operations.constraints.PropertyFieldNameIdentifier
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.ArgumentSelector
import mu.KotlinLogging
import kotlin.reflect.KClass

suspend fun Constraint.evaluate(
   value: TypedInstance,
   schema: Schema,
   context: InPlaceQueryEngine
): ConstraintEvaluation {
   return when {
      this is ExpressionConstraint -> this.evaluate(value, schema, context)
      else -> error("Unsupported constraint type: ${this::class.simpleName}")
   }
}

suspend fun ExpressionConstraint.evaluate(
   value: TypedInstance,
   schema: Schema,
   context: InPlaceQueryEngine
): ConstraintEvaluation {
   val scopedFact = ScopedFact(
      scope = ProjectionFunctionScope.implicitThis(value.type.taxiType),
      fact = value
   )
   val evaluationResult =
      context.evaluate(this.expression, FactBag.empty().withAdditionalScopedFacts(listOf(scopedFact), schema))
   return when (evaluationResult.value) {
      true -> ConstraintEvaluation.valid(value)
      false -> {
         val updater = when (value) {
            is TypedObject -> getFieldUpdater(value, this)
            is TypedValue -> ReplaceValueUpdater
            else -> error("Can't create constraint updater for type ${value::class.java} ")
         }
         DefaultConstraintEvaluation(
            value,
            ConstraintExpressionEvaluationFailed(
               value,
               value.type,
               updater,
               this.expression,
               evaluationResult,
               schema
            )
         )
      }

      else -> error("Expected that constraint expression would evaluate to boolean, but found ${evaluationResult.typeName} (${evaluationResult.value}) from expression ${this.expression.asTaxi()}")
   }
}


private fun <T : Any> Iterable<T>.containsType(requestedClass: KClass<*>): Boolean {
   return this.any { it::class == requestedClass }
}

private fun <T : Any> Iterable<T>.containsTypes(vararg requestedClass: KClass<*>): Boolean {
   return requestedClass.all { containsType(it) }
}

private val logger = KotlinLogging.logger {}
fun getFieldUpdater(
   paramValue: TypedObject,
   expressionConstraint: ExpressionConstraint
): ConstraintViolationValueUpdater {
   // Try to find the field that needs updating.
   // This isn't easy, given we're working against an expression.
   val expression = expressionConstraint.expression
   if (expression !is OperatorExpression) {
      error("Unable to determine which field to update in an expression of type ${expression::class.simpleName} - ${expression.asTaxi()}")
   }
   val components = setOf(expression.lhs, expression.rhs)
   when {
      components.containsTypes(ArgumentSelector::class, LiteralExpression::class) -> {
         // This is a comparison of field-to-literal (eg: this.money == 'GBP')
         val selector = components.first { it::class == ArgumentSelector::class } as ArgumentSelector
         return ReplaceFieldValueUpdater(paramValue, PropertyFieldNameIdentifier(selector.path))
      }

      components.containsTypes(TypeExpression::class, LiteralExpression::class) -> {
         // This is a comparison of type-to-literal (eg: this::CurrencySymbol == 'GBP')
         val selector = components.first { it::class == TypeExpression::class } as TypeExpression
         return ReplaceFieldValueUpdater(paramValue, PropertyTypeIdentifier(selector.type))
      }

      else -> {
         val message =
            "Unable to determine path of property to update when expression is pair of ${components.map { it::class.simpleName }}"
         logger.error { message }
         error(message)
      }
   }
}
