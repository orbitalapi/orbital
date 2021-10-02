package io.vyne.models

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.expressions.LambdaExpression

/**
 * A TypedInstance that hasn't yet been evaluated, as it's value is a Lambda function
 */
class DeferredTypedInstance(val expression: LambdaExpression, private val schema: Schema) : TypedInstance {
   override val type: Type = schema.type(expression.returnType)
   override val value: Any?
      get() = TODO("Not yet implemented")
   override val source: DataSource
      get() = TODO("Not yet implemented")

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      TODO("Not yet implemented")
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      TODO("Not yet implemented")
   }
}
