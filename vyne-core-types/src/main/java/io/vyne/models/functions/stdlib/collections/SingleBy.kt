package io.vyne.models.functions.stdlib.collections

import com.google.common.base.Stopwatch
import io.vyne.models.*
import io.vyne.models.functions.FunctionResultCacheKey
import io.vyne.models.functions.NamedFunctionInvoker
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName
import mu.KotlinLogging

object SingleBy : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.SingleBy.name
   private val logger = KotlinLogging.logger {}

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

      if (inputValues[0] is TypedNull) {
         return TypedNull.create(returnType, FailedEvaluatedExpression(function.asTaxi(), inputValues, "Recevied null for the collection to iterate"))
      }
      val collection = inputValues[0] as TypedCollection
      val deferredInstance = inputValues[1] as DeferredTypedInstance
      val searchValue = inputValues[2] as TypedInstance
      val expressionReturnType = schema.type(deferredInstance.expression.returnType)

      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)

      val resultCacheKey = FunctionResultCacheKey(
         functionName,
         listOf(collection, deferredInstance)
      )
      val groupedData = resultCache.getOrPut(
         resultCacheKey
      ) {
         val stopwatch = Stopwatch.createStarted()
         val grouped = collection.groupBy { collectionMember ->
            val factBag = FactBagValueSupplier.of(listOf(collectionMember), schema, thisScopeValueSupplier = objectFactory)
            val reader = AccessorReader(factBag, schema.functionRegistry, schema, functionResultCache = resultCache)
            val evaluated = reader.evaluate(
               collectionMember,
               expressionReturnType,
               deferredInstance.expression,
               dataSource = dataSource,
               format = null
            )
            evaluated
         }
         logger.debug { "singleBy grouping function took ${stopwatch.elapsed().toMillis()}ms" }
         grouped
      } as Map<TypedInstance, List<TypedInstance>>

      val matchedCollection = groupedData[searchValue]
      val result = when {
         matchedCollection == null -> TypedNull.create(returnType, FailedEvaluatedExpression(function.asTaxi(), inputValues, "No matching value in collection of ${collection.memberType.longDisplayName} with ${collection.size} elements matched value ${searchValue.value}"))
         matchedCollection.size == 1 -> matchedCollection.single()
         else -> TypedNull.create(returnType, FailedEvaluatedExpression(function.asTaxi(), inputValues, "Search key  ${searchValue.value} matched ${matchedCollection.size} elements, expected a single match."))
      }
      // TODO  :Shouldn't we be returning a new TypedInstance with a datasource here?

      return result
   }
}
