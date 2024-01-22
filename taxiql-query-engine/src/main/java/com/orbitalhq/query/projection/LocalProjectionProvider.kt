package com.orbitalhq.query.projection

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.ValueLookupReturnedNull
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.models.facts.asIteratingScope
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.Projection
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.TypeQueryExpression
import com.orbitalhq.query.TypedInstanceWithMetadata
import com.orbitalhq.query.withProcessingMetadata
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lang.taxi.accessors.CollectionProjectionExpressionAccessor
import lang.taxi.types.ArrayType
import lang.taxi.types.Arrays
import lang.taxi.types.StreamType
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.Executors

private val projectingDispatcher = Executors.newFixedThreadPool(16).asCoroutineDispatcher()
private val logger = KotlinLogging.logger {}

@OptIn(FlowPreview::class)
class LocalProjectionProvider : ProjectionProvider {

   private val projectingScope = CoroutineScope(projectingDispatcher)

   override fun project(
      source: Flow<TypedInstance>,
      declaredSourceType: Type,
      projection: Projection,
      context: QueryContext,
      globalFacts: FactBag,
      metricTags: MetricTags
   ): Flow<TypedInstanceWithMetadata> {

      context.cancelFlux.subscribe {
         logger.info { "QueryEngine for queryId ${context.queryId} is cancelling" }
         projectingScope.cancel()
      }

      // This pattern aims to allow the concurrent execution of multiple flows.
      // Normally, flow execution is sequential - ie., one flow must complete befre the next
      // item is taken.  buffer() is used here to allow up to n parallel flows to execute.
      // MP: @Anthony - please leave some comments here that describe the rationale for
      // map { async { .. } }.flatMapMerge { await }
      return source
         .buffer()
         .withIndex()
         .takeWhile { !context.cancelRequested }
         .filter { !context.cancelRequested }
         .distinctUntilChanged()
         .map { emittedResult ->
            logger.debug { "Starting to project instance of ${emittedResult.value.type.qualifiedName.shortDisplayName} (index ${emittedResult.index}) to instance of ${projection.type.qualifiedName.shortDisplayName}" }
            projectingScope.async {
               val startTime = Instant.now()
               if (!isActive) {
                  logger.warn { "Query Cancelled exiting!" }
                  cancel()
               }

               val scopedFact = buildScopedProjectionFact(projection, emittedResult, context)

               projectOrMap(
                  scopedFact,
                  declaredSourceType,
                  context,
                  globalFacts,
                  emittedResult,
                  projection.type,
                  startTime
               )
            }
         }
         .buffer(16).map { it.await() }.flatMapMerge { it }
   }

   /**
    * When the projection defined a scoped fact:
    * find { foo ) as ( Movie[] ) -> { // <--- that's a scoped fact, right there.
    * }
    *
    * This code returns the actual fact, selecting the value from the inbound value.
    */
   private fun buildScopedProjectionFact(
      projection: Projection,
      emittedResult: IndexedValue<TypedInstance>,
      context: QueryContext
   ) = projection.scope?.let { scope ->
      val emittedType = emittedResult.value.type
      // Adding this guard clause.
      // We're getting the incorrect type passed in.
      // It's an upstream problbem, but if we let it flow any further, we spend huge CPU cycles
      // trying to project the wrong source type.
      // TODO : Investigate the cause - I suspect it's coming from the Graph search strategy, when service invocation fails.
      val isAssignable = when (scope.type) {
         is ArrayType -> emittedType.taxiType.isAssignableTo((scope.type as ArrayType).memberType)
         is StreamType -> emittedType.taxiType.isAssignableTo((scope.type as StreamType).type)
         else -> emittedType.taxiType.isAssignableTo(scope.type)
      }

      val schema = context.schema
      if (!isAssignable) {
         val scopeType = schema.type(scope.type)
         val selectedFact = try {
            FactBag.of(emittedResult.value, schema)
               .getFact(scopeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
         } catch (e: Exception) {
            TypedNull.create(
               scopeType, source = ValueLookupReturnedNull(
                  "Projection scope requested type ${scopeType.qualifiedName.shortDisplayName}, which was not found on the type of ${emittedResult.value.typeName}",
                  scopeType.name
               )
            )
         }
         ScopedFact(scope, selectedFact)
      } else {
         ScopedFact(scope, emittedResult.value)
      }

   } ?: null

   /**
    * Will either directly project the provided value to the target type,
    * or - in the case of arrays - map (iterate and project each member).
    *
    * Also considers scenarios where we're operating on a single item that's part
    * of a broader array (ie., streamed results).
    */
   private suspend fun projectOrMap(
      scopedFact: ScopedFact?,
      declaredSourceType: Type,
      context: QueryContext,
      globalFacts: FactBag,
      emittedResult: IndexedValue<TypedInstance>,
      projectionType: Type,
      startTime: Instant
   ): Flow<TypedInstanceWithMetadata> {
      val valueToProject = scopedFact?.fact ?: emittedResult.value

      return when {
         // We're working against an array that's being streamed. Common usecase for find { Movie[] } as { ... }[]
         declaredSourceType.isCollection && valueToProject.type.isAssignableTo(declaredSourceType.collectionType!!) && projectionType.isCollection -> {
           doProjection(scopedFact, context, globalFacts, emittedResult, projectionType.collectionType!!, startTime)
         }
         // Streams.
         // Note that streams are projected to arrays, so projectionType should be T[]
         declaredSourceType.isStream && valueToProject.type.isAssignableTo(declaredSourceType.typeParameters[0]!!) && projectionType.isCollection -> {
            doProjection(scopedFact, context, globalFacts, emittedResult, projectionType.typeParameters[0], startTime)
         }
         // Map A[] -> B[]. Use-case when mapping a full array that we already have. (eg: find { MovieSchedule } as (Movie[]) -> { .... }
         Arrays.isArray(projectionType.paramaterizedName) && Arrays.isArray(valueToProject.typeName) -> {
            // We're projecting Array -> Array, so do a map() on the results
            if (scopedFact == null) {
               // If this error occurs, understand the flow, as I think all use-cases are covered, and by
               // this stage we should have a scopedFact. However, it could be I missed a scenario, which should
               // just be added to the when clause.
               error("Expected a scoped fact. Perhaps the conditions are non-exhaustive")
            }
            doMappingProjection(
               scopedFact,
               declaredSourceType,
               context,
               globalFacts,
               emittedResult,
               projectionType,
               startTime
            )
         }

         else ->  doProjection(scopedFact, context, globalFacts, emittedResult, projectionType, startTime)
      }


   }

   /**
    * Iterates a collection, projecting each member.
    */
   private suspend fun doMappingProjection(
      scopedFact: ScopedFact,
      declaredSourceType: Type,
      context: QueryContext,
      globalFacts: FactBag,
      emittedResult: IndexedValue<TypedInstance>,
      projectionType: Type,
      startTime: Instant
   ): Flow<TypedInstanceWithMetadata> {
      val collection = scopedFact!!.fact as TypedCollection
      val memberFlows = collection.map { member ->
         val memberFact = ScopedFact(scopedFact.scope.asIteratingScope(), member)
         projectOrMap(
            memberFact,
            declaredSourceType,
            context,
            globalFacts,
            emittedResult,
            projectionType.collectionType!!,
            startTime
         )
      }
      return merge(*memberFlows.toTypedArray())
   }

   private suspend fun doProjection(
      scopedFact: ScopedFact?,
      context: QueryContext,
      globalFacts: FactBag,
      emittedResult: IndexedValue<TypedInstance>,
      projectionType: Type,
      startTime: Instant
   ): Flow<TypedInstanceWithMetadata> {
      // If the projection scope was explicitly defined,
      // add the thing we're projecting as a specific scoped fact.
      // This makes it available for both type-based-searches (standard),
      // and when searching by scope.
      // Otherwise, just add it as a normal fact at the root.
      // Note: In time, we should probably refactor so that there's ALWAYS a root
      // scope, with a name of "this" if not otherwise specified.
      val projectionContext = if (scopedFact == null) {
         context.only(globalFacts.rootFacts() + emittedResult.value)
      } else {
         context.only(globalFacts.rootFacts(), scopedFacts = listOf(scopedFact))
      }
      val buildResult = projectionContext.build(TypeQueryExpression(projectionType))
      return buildResult.results.map {
         it.withProcessingMetadata(asOf = startTime)
      }
   }
}
