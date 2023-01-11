package io.vyne.models

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.ImmutableEquality
import lang.taxi.expressions.LambdaExpression

/**
 * A TypedInstance that hasn't yet been evaluated, as it's value is a Lambda function
 */
class DeferredTypedInstance(
   val expression: LambdaExpression,
   private val schema: Schema,
   override val source: DataSource
) : TypedInstance {
   override val type: Type = schema.type(expression.returnType)
   override val value: Any = expression.asTaxi()

   private val equality = ImmutableEquality(
      this,
      DeferredTypedInstance::value
   )

   override fun hashCode(): Int = equality.hash()
   override fun equals(other: Any?): Boolean {
      return equality.isEqualTo(other)
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      TODO("Not yet implemented")
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      TODO("Not yet implemented")
   }
}
