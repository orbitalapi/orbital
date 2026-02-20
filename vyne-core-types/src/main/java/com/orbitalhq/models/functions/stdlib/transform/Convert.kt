package com.orbitalhq.models.functions.stdlib.transform

import com.orbitalhq.models.*
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.stdlib.collections.SingleBy
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.stdlib.Collections
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName
import mu.KotlinLogging

object Convert : NullSafeInvoker() {
   override suspend fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      rawMessageBeingParsed: Any?,
      thisScopeValueSupplier: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val source = inputValues[0]
      val targetType = (inputValues[1] as TypeReferenceInstance).resolveType(schema)
      val resultCacheKey = FunctionResultCacheKey(
         functionName,
         // Don't pass the input TypeReferenceInstance, as it hasn't been resolved against the
         // current schema
         listOf(source, TypeReferenceInstance.from(targetType))
      )

      // When converting, if the targetType is a raw type (String etc),
      // the TypedObjectFactory may not perform the conversion correctly.
      // Here, we just re-type it back to the requested type post-conversion, if
      // required.
      fun TypedInstance.convertToRawTypeIfRequired(): TypedInstance {
         val targetMemberType = targetType.collectionType ?: targetType
         return if (targetMemberType.isPrimitive && this.type != targetMemberType) {
            this.withTypeAlias(targetMemberType)
         } else {
            this
         }
      }

      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      val converted = resultCache.getOrPut(resultCacheKey) {
         kotlinx.coroutines.runBlocking {
            if (targetType.isCollection && source is Collection<*>) {
               val typedInstances = source.map { member ->
                  TypedObjectFactory(targetType.collectionType!!, FactBag.of(member as TypedInstance, schema), schema, source = dataSource, functionRegistry = schema.functionRegistry)
                     .build()
                     .convertToRawTypeIfRequired()
               }
               TypedCollection.arrayOf(targetType.collectionType!!, typedInstances, dataSource)
            } else {
               TypedObjectFactory(targetType, FactBag.of(source, schema), schema, source = dataSource, functionRegistry = schema.functionRegistry)
                  .build()
                  .convertToRawTypeIfRequired()
            }
         }
      }
      return converted as TypedInstance
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Convert.name

   private val logger = KotlinLogging.logger {}

}
