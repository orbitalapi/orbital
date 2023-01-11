package io.vyne.query.graph.edges

import io.vyne.models.facts.CopyOnWriteFactBag
import io.vyne.models.facts.FactDiscoveryStrategy
import io.vyne.models.MixedSources
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.query.QueryContext
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.SearchGraphExclusion
import io.vyne.query.graph.operationInvocation.UnresolvedOperationParametersException
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Type
import io.vyne.utils.log
import kotlinx.coroutines.flow.firstOrNull
import lang.taxi.types.PrimitiveType
import java.util.concurrent.CopyOnWriteArrayList

class ParameterFactory {
   suspend fun discover(paramType: Type, context: QueryContext, candidateValue: TypedInstance? = null, operation: RemoteOperation? = null): TypedInstance {
      // First, search only the top level for facts
      val firstLevelDiscovery = context.getFactOrNull(paramType, strategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY)
      if (hasValue(firstLevelDiscovery)) {
         // TODO (1) : Find an instance that is linked, somehow, rather than just something random
         // TODO (2) : Fail if there are multiple instances
         return firstLevelDiscovery!!
      }

      // Check to see if there's exactly one instance somewhere within the context
      // Note : I'm cheating here, I really should find the value required by retracing steps
      // walked in the path.  But, it's unclear if this is possible, given the scattered way that
      // the algorithims are evaluated
      val anyDepthOneDistinct =
         context.getFactOrNull(paramType, strategy = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
      if (hasValue(anyDepthOneDistinct)) {
         // TODO (1) : Find an instance that is linked, somehow, rather than just something random
         return anyDepthOneDistinct!!
      }

//      if (startingPoint.type == paramType) {
//         return EvaluatedLink.success(link, startingPoint, startingPoint)
//      }
      if (!paramType.isScalar && !paramType.isParameterType) {
         throw UnresolvedOperationParametersException(
             "No instance of type ${paramType.name} is present in the graph, and the type is not a parameter type, so cannot be constructed. ",
             context.evaluatedPath(),
             context.profiler.root,
             emptyList()
         )
      }
      // This is a parameter type.  Try to construct an instance
      return attemptToConstruct(paramType, context, operation, candidateValue)
   }

   private fun hasValue(instance: TypedInstance?): Boolean {
      return when {
         instance == null -> false
         instance is TypedNull -> false
         // This is a big call, treating empty strings as not populated
         // It's probably the wrong call.
         // But we need to consider how to filter this data upstream from providers,
         // and allow that filtering to be configurable.
         // Adding this here now because it's caused a bug at a client, let's
         // revisit if/when it becomes problematic.
         instance.type.taxiType.basePrimitive == PrimitiveType.STRING && instance.value == "" -> false
         else -> true
      }
   }

   private suspend fun attemptToConstruct(
       paramType: Type,
       context: QueryContext,
       operation: RemoteOperation?,
       candidateValue: TypedInstance? = null,
       typesCurrentlyUnderConstruction: Set<Type> = emptySet()
   ): TypedInstance {
      val fields = paramType.attributes.map { (attributeName, field) ->
         val attributeType = context.schema.type(field.type.fullyQualifiedName)

         var attributeValue: TypedInstance? =
            context.getFactOrNull(attributeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)

         /**
          *  see io.vyne.query.graph.ParameterTypeTest for the below if block.
          */
         if (attributeValue is TypedNull && candidateValue != null) {
            val factBag = CopyOnWriteFactBag(CopyOnWriteArrayList(setOf(candidateValue)), context.schema)
            attributeValue =
              context.copy(facts = factBag).getFactOrNull(attributeType,
                  FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT
              ) ?: attributeValue
         }

         // First, look in the context to see if it's there.
         if (attributeValue == null) {
            // ... if not, try and find the value in the graph
            // When searching to construct a parameter for an operation, exclude the operation itself.
            // Otherwise, if an operation result includes an input parameter, we can end up in a recursive loop, trying to
            // construct a request for the operation to discover a parameter needed to construct a request for the operation.
            val excludedOperations = operation?.let {
               setOf(
                   SearchGraphExclusion(
                       "Operation is excluded as we're searching for an input for it",
                       it
                   )
               )
            } ?: emptySet()
            val queryResult = context.find(QuerySpecTypeNode(attributeType), excludedOperations)
            if (queryResult.isFullyResolved) {
               attributeValue = queryResult.results.firstOrNull() ?:
                  // TODO : This might actually be legal, as it could be valid for a value to resolve to null
                  throw IllegalArgumentException("Expected queryResult to return attribute with type ${attributeType.fullyQualifiedName} but the returned value was null")
            } else {
               // ... finally, try constructing the value...
               if (!attributeType.isScalar && !typesCurrentlyUnderConstruction.contains(attributeType)) {
                  log().debug(
                     "Parameter of type {} not present within the context.  Attempting to construct one.",
                     attributeType.name.fullyQualifiedName
                  )
                  val constructedType = attemptToConstruct(
                     attributeType,
                     context,
                     typesCurrentlyUnderConstruction = typesCurrentlyUnderConstruction + attributeType,
                     operation = operation
                  )
                  log().debug(
                     "Parameter of type {} constructed: {}",
                     constructedType,
                     attributeType.name.fullyQualifiedName
                  )
                  attributeValue = constructedType
               }
            }
         }

         when {
            attributeValue is TypedNull && !field.nullable -> {
               throw UnresolvedOperationParametersException(
                   "Unable to construct instance of type ${paramType.name}, as field $attributeName (of type ${attributeType.name}) is not present within the context, and is not constructable ",
                   context.evaluatedPath(),
                   context.profiler.root,
                   attributeValue.source.failedAttempts
               )
            }

            (attributeValue == null) -> {
               throw UnresolvedOperationParametersException(
                   "Unable to construct instance of type ${paramType.name}, as field $attributeName (of type ${attributeType.name}) is not present within the context, and is not constructable ",
                   context.evaluatedPath(),
                   context.profiler.root,
                   emptyList()
               )
            }

            else -> attributeName to attributeValue
         }
      }.toMap()
      return TypedObject(paramType, fields, MixedSources)
   }

}
