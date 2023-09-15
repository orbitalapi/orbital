package com.orbitalhq.models

import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.Ids
import com.orbitalhq.utils.ImmutableEquality
import lang.taxi.expressions.LambdaExpression
import lang.taxi.types.FieldProjection
import lang.taxi.types.FormatsAndZoneOffset

/**
 * A TypedInstance that hasn't yet been evaluated, as it's value is a Lambda function
 */
class DeferredExpression(
   val expression: LambdaExpression,
   private val schema: Schema,
   override val source: DataSource
) : DeferredTypedInstance, TypedInstance {
   override val type: Type = schema.type(expression.returnType)
   override val value: Any = expression.asTaxi()

   private val equality = ImmutableEquality(
      this,
      DeferredExpression::value
   )

   override val nodeId: String = Ids.fastUuid()

   override fun hashCode(): Int = equality.hash()
   override fun equals(other: Any?): Boolean {
      return equality.isEqualTo(other)
   }

   override fun evaluate(
      input: TypedInstance,
      dataSource: DataSource,
      factBag: EvaluationValueSupplier,
      functionResultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val reader = AccessorReader(factBag, schema.functionRegistry, schema, functionResultCache = functionResultCache)
      val evaluated =
         reader.evaluate(input, schema.type(expression.returnType), expression, dataSource = dataSource, format = null)
      return evaluated
   }


}

data class DeferredProjection(
   val sourceInstance: DeferredTypedInstance,
   val projection: FieldProjection,
   val projector: ValueProjector,
   override val type: Type,
   private val schema: Schema,
   private val nullValues: Set<String>,
   override val source: DataSource,
   private val format: FormatsAndZoneOffset?,
   private val nullable: Boolean,
   private val allowContextQuerying: Boolean
) : DeferredTypedInstance {
   //   override val type: Type = schema.type(projection.projectedType)
   override val value: Any? = sourceInstance.value
   override val nodeId: String = Ids.fastUuid()

   private val equality = ImmutableEquality(
      this,
      DeferredProjection::sourceInstance,
      DeferredProjection::projection
   )


   override fun hashCode(): Int = equality.hash()
   override fun equals(other: Any?): Boolean {
      return equality.isEqualTo(other)
   }

   override fun evaluate(
      input: TypedInstance,
      dataSource: DataSource,
      factBag: EvaluationValueSupplier,
      functionResultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val valueToProject = sourceInstance.evaluate(input, dataSource, factBag, functionResultCache)
      val projectedValue = projector.project(valueToProject, projection, type, schema, nullValues, source, format, nullable, allowContextQuerying)
      return projectedValue
   }
}


/**
 * A wrapper around various implementations of TypedInstances
 * that don't have values because they haven't been evaluated yet (eg., lamdas)
 */
interface DeferredTypedInstance : TypedInstance {


   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      TODO("Not yet implemented")
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      TODO("Not yet implemented")
   }

   fun evaluate(
      input: TypedInstance,
      dataSource: DataSource,
      factBag: EvaluationValueSupplier,
      functionResultCache: MutableMap<FunctionResultCacheKey, Any> = mutableMapOf()
   ): TypedInstance
}
