package com.orbitalhq.models.functions.stdlib

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import com.orbitalhq.models.*
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.stdlib.collections.Append
import com.orbitalhq.models.functions.stdlib.collections.IfEmpty
import com.orbitalhq.models.functions.stdlib.collections.JoinToString
import com.orbitalhq.models.functions.stdlib.collections.ListOf
import com.orbitalhq.models.functions.stdlib.collections.OrEmpty
import com.orbitalhq.models.functions.stdlib.collections.createFailureWithTypedNull
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName
import mu.KotlinLogging

object Collections {
   val functions: List<NamedFunctionInvoker> = listOf(
      Contains,
      AnyOf,
      AllOf,
      NoneOf,
      ListOf,
      JoinToString,
      IfEmpty,
      OrEmpty,
      Append,
      Size,
      IsNullOrEmpty
   )
}


object AnyOf :
   BooleanPredicateEvaluator(lang.taxi.functions.stdlib.AnyOf.name, { inputs: List<Boolean> -> inputs.any { it } })

object AllOf :
   BooleanPredicateEvaluator(lang.taxi.functions.stdlib.AllOf.name, { inputs: List<Boolean> -> inputs.all { it } })

object NoneOf :
   BooleanPredicateEvaluator(lang.taxi.functions.stdlib.NoneOf.name, { inputs: List<Boolean> -> inputs.none { it } })

abstract class BooleanPredicateEvaluator(
   override val functionName: QualifiedName,
   private val reducer: (List<Boolean>) -> Boolean
) :
   NullSafeInvoker() {

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
      return expectAllBoolean(inputValues, function, returnType)
         .map(reducer)
         .map { boolean ->
            TypedInstance.from(
               returnType,
               boolean,
               schema,
               source = EvaluatedExpression(function.asTaxi(), inputValues)
            )
         }
         .getOrHandle { typedNull -> typedNull }
   }

}

private fun expectAllBoolean(
   inputValues: List<TypedInstance>,
   function: FunctionAccessor,
   returnType: Type
): Either<TypedNull, List<Boolean>> {
   val values = inputValues.map { it.value }
   if (values.any { it == null }) {
      return TypedNull.create(
         returnType,
         FailedEvaluatedExpression(function.asTaxi(), inputValues, "Received a null value in inputs")
      )
         .left()
   }
   val typesOtherThanBoolean = values.filter { it !is Boolean }
      .map { it!!::class.simpleName!! }
      .distinct()
   if (typesOtherThanBoolean.isNotEmpty()) {
      return TypedNull.create(
         returnType,
         FailedEvaluatedExpression(function.asTaxi(), inputValues, "Returned types other than Boolean")
      )
         .left()
   }
   return (values as List<Boolean>).right()
}

object Contains : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Contains.name
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
      val collection = inputValues[0] as TypedCollection
      val searchTarget = inputValues[1] as TypedInstance
      val result = collection.any { it.valueEquals(searchTarget) }
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      return TypedInstance.from(returnType, result, schema, source = dataSource)
   }

}


object Size : NullSafeInvoker() {
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
      val collection = inputValues.argumentAsTypedInstanceOrError<TypedCollection>(0, returnType, function)
         .getOrElse { return it }
      return TypedValue.from(returnType, collection.size, source = EvaluatedExpression(function.asTaxi(), inputValues))
   }

   override val functionName: QualifiedName  = lang.taxi.functions.stdlib.Size.name
}


// Intentionally not nullSafeInvoker
object IsNullOrEmpty : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.IsNullOrEmpty.name

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
      val collection = inputValues.nullableArgumentAsTypedInstanceOrError<TypedCollection>(0, returnType, function)
         .getOrElse { return it }
      val result = when {
         collection is TypedNull -> true
         collection is TypedCollection -> collection.isEmpty()
         // This shouldn't happen...
         else -> return createFailureWithTypedNull("Unhandled branch - isNullOrEmpty expected either null or a typed collection", returnType, function, inputValues)
      }
      return TypedValue.from(returnType, result, source = EvaluatedExpression(function.asTaxi(), inputValues))
   }

}
