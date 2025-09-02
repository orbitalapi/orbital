package com.orbitalhq.models.functions.stdlib

import arrow.core.getOrElse
import com.orbitalhq.models.ConversionService
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypeReferenceInstance
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.models.facts.FactSearch
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.stdlib.collections.createFailureWithTypedNull
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import mu.KotlinLogging

object ObjectFunctions {
   val functions: List<NamedFunctionInvoker> = listOf(
      Equals,
      EmptyInstance,
      CollectAllInstances
   )
}


object CollectAllInstances : NullSafeInvoker() {
   private val logger = KotlinLogging.logger {}
   override fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      rawMessageBeingParsed: Any?,
      thisScopeValueSupplier: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val inputType = inputValues.argumentAsTypedInstanceOrError<TypeReferenceInstance>(0, returnType, function)
         .getOrElse { return it }
      val result = if (thisScopeValueSupplier.hasDataContext) {
         val dataContext = thisScopeValueSupplier.dataContext
         val allInstances = dataContext.getFactOrTypedNull(
            FactSearch.findType(inputType.type, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)
         ).map { typedInstance ->
            DataSourceUpdater.update(typedInstance, EvaluatedExpression(function.asTaxi(), inputValues))
         }
            .getOrElse { it }
         allInstances
      } else {
         val message =
            "Cannot perform merge operation on requested type (${inputType.type.paramaterizedName} as the provided context is a ${thisScopeValueSupplier::class.simpleName} which does not expose a SearchableDataContext"
         logger.warn(message)
         return createFailureWithTypedNull(message, returnType, function, inputValues)
      }
      return result
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.CollectAllInstances.name
}

object Equals : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Equals.name

   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      rawMessageBeingParsed: Any?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val areEqual = inputValues[0] == inputValues[1]
      return TypedValue.from(
         schema.type(PrimitiveType.BOOLEAN),
         areEqual,
         ConversionService.DEFAULT_CONVERTER,
         EvaluatedExpression(function.asTaxi(), inputValues)
      )
   }
}


object EmptyInstance : NullSafeInvoker() {
   override fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      rawMessageBeingParsed: Any?,
      thisScopeValueSupplier: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val typeReference = inputValues[0]
      if (typeReference !is TypeReferenceInstance) {
         return createFailureWithTypedNull(
            "Expected arg 0 to be a TypeReference, but instead got ${typeReference::class.simpleName}",
            returnType,
            function,
            inputValues
         )
      }
      val type = typeReference.type
      val empty = buildEmptyMap(type, schema)
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      val result = TypedInstance.from(type, empty, schema, source = dataSource)
      return result
   }

   private fun buildEmptyMap(type: Type, schema: Schema): Any? {
      return when {
         type.isScalar -> null
         type.isCollection -> emptyList<Any?>()
         type.isStream -> emptyList<Any?>()
         else -> {
            type.attributes.map { (name, field) ->
               name to buildEmptyMap(schema.type(field.type), schema)
            }.toMap()
         }
      }
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.EmptyInstance.name
}
