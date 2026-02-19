package com.orbitalhq.query.collections

import com.orbitalhq.models.*
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.TypeQueryExpression
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.taxi.toVyneType
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
      val searchType: Type = if (Arrays.isArray(accessor.type)) {
         accessor.type
      } else {
         Arrays.arrayOf(accessor.type)
      }.toVyneType(schema)

      val projectionScopeFacts =
         queryContext.getFactOrNull(searchType, strategy = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
            ?: return TypedNull.create(targetType)
      val collectionToIterate: List<TypedInstance> = when (projectionScopeFacts) {
         is TypedCollection -> projectionScopeFacts.value
         else -> listOf(projectionScopeFacts)
      }

      // Grab any additional scope facts that were speciied in a 'with scope' clause
      val additionalScopeFacts = accessor.projectionScope?.accessors?.map { scopeAccessor ->
         objectFactory.readAccessor(scopeAccessor.returnType.toVyneType(schema), scopeAccessor,null /* TODO : Formats */)
      } ?: emptyList()

      val targetMemberType = targetType.collectionType ?: targetType
      val buildResults = runBlocking {
         collectionToIterate.asFlow()
            .flatMapConcat { collectionMember ->
               queryContext.only(listOf(collectionMember) + additionalScopeFacts)
                  .build(TypeQueryExpression(targetMemberType)).results
            }.toList()
      }

      val builtCollection = TypedCollection.from(buildResults, source)
      return builtCollection
   }


}
