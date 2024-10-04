package com.orbitalhq.models.functions.stdlib.collections

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.get
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

typealias TypedNullErrorSupplier = (String) -> TypedNull
object Intersection: NamedFunctionInvoker, CollectionFilteringFunction() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Intersection.name

   private fun List<TypedInstance>.getCollectionAt(index: Int, errorSupplier: TypedNullErrorSupplier): Either<TypedNull, TypedCollection> {
      return when (val collection = this[index]) {
         is TypedNull -> {
            return errorSupplier("Expected a collection in param 0, but got null.").left()
         }

         is TypedCollection -> collection.right()
         else -> error("Expected either a TypedNull or a TypedCollection in param 0.  Got a ${collection::class.simpleName}")
      }
   }

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
      fun createTypeNullFailure(message: String): TypedNull {
         return createFailureWithTypedNull(
            message, returnType, function, inputValues
         )
      }
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      val result = inputValues.getCollectionAt(0,::createTypeNullFailure).flatMap { collectionA ->
         inputValues.getCollectionAt(1, ::createTypeNullFailure).map { collectionB ->
            val result = collectionA.filter { collectionB.contains(it) }
            TypedCollection.arrayOf(returnType, result, dataSource)
         }
      }.get() as TypedInstance
      return result
   }

}
