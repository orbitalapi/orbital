package io.vyne.query.collections

import io.vyne.models.AccessorHandler
import io.vyne.models.DataSource
import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.FactDiscoveryStrategy
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.query.QueryContext
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.toVyneType
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.accessors.CollectionProjectionExpressionAccessor
import lang.taxi.types.Arrays
import kotlin.reflect.KClass

/**
 * Responsible for acting on CollectionProjection accessors
 * eg:
 * model Foo {
 *    friends: Person[]
 *    friendNames: Names[] by [Person]
 * }
 */
class CollectionProjectionBuilder(val queryContext: QueryContext) :
   AccessorHandler<CollectionProjectionExpressionAccessor> {

   override val accessorType: KClass<CollectionProjectionExpressionAccessor> =
      CollectionProjectionExpressionAccessor::class

   override fun process(
      accessor: CollectionProjectionExpressionAccessor,
      objectFactory: EvaluationValueSupplier,
      schema: Schema,
      targetType: Type,
      source: DataSource
   ): TypedInstance {
      val searchType:Type = if (Arrays.isArray(accessor.type) ) {
         accessor.type
      } else {
         Arrays.arrayOf(accessor.type)
      }.toVyneType(schema)
      val projectionScopeFacts =
         queryContext.getFactOrNull(searchType, strategy = FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)
            ?: return TypedNull.create(targetType)
      val discoveredFacts: List<TypedInstance> = when (projectionScopeFacts) {
         is TypedCollection -> projectionScopeFacts.value
         else -> listOf(projectionScopeFacts)
      }

      val buildResults = runBlocking {  discoveredFacts.asFlow()
         .flatMapConcat { discoveredFact ->
            queryContext.only(discoveredFact).build(targetType.qualifiedName.parameterizedName).results
         }.toList() }

      val builtCollection = TypedCollection.from(buildResults)
      return builtCollection
   }

   suspend fun build(
      targetMemberType: Type,
      projectionScopeTypes: List<Type>
   ): TypedInstance? {
      if (projectionScopeTypes.size > 1) {
         error("Support for multiple projection scope types not yet implemented")
      }
      val projectionScopeType = projectionScopeTypes.single()
      val projectionScopeFacts =
         queryContext.getFactOrNull(projectionScopeType, strategy = FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)
            ?: return null
      val discoveredFacts: List<TypedInstance> = when (projectionScopeFacts) {
         is TypedCollection -> projectionScopeFacts.value
         else -> listOf(projectionScopeFacts)
      }

      val buildResults = discoveredFacts.asFlow()
         .flatMapConcat { discoveredFact ->
            queryContext.only(discoveredFact).build(targetMemberType.name).results
         }.toList()

      val builtCollection = TypedCollection.from(buildResults)
      return builtCollection
   }


}
