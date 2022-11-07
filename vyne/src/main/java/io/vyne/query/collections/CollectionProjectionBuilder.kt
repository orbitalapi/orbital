package io.vyne.query.collections

import io.vyne.models.AccessorHandler
import io.vyne.models.DataSource
import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.facts.FactDiscoveryStrategy
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
         objectFactory.readAccessor(scopeAccessor.returnType.toVyneType(schema), scopeAccessor)
      } ?: emptyList()

      val targetMemberType = targetType.collectionType ?: targetType
      val buildResults = runBlocking {
         collectionToIterate.asFlow()
            .flatMapConcat { collectionMember ->
               queryContext.only( listOf(collectionMember) + additionalScopeFacts )
                  .build(targetMemberType.qualifiedName.parameterizedName).results
            }.toList()
      }

      val builtCollection = TypedCollection.from(buildResults, source)
      return builtCollection
   }


}
