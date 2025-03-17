package com.orbitalhq.query.projection

import com.orbitalhq.models.ProjectionFunctionScopeEvaluator
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.models.facts.asIteratingScope
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.Projection
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.TypeQueryExpression
import com.orbitalhq.query.TypedInstanceWithMetadata
import com.orbitalhq.query.withProcessingMetadata
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.isActive
import kotlinx.coroutines.slf4j.MDCContext
import lang.taxi.types.Arrays
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


private val logger = KotlinLogging.logger {}

@OptIn(FlowPreview::class)
class LocalProjectionProvider : ProjectionProvider {
   companion object {
      private const val threadPoolSize: Int = 16
      private val projectingDispatcher =
         ThreadPoolExecutor(
            threadPoolSize, threadPoolSize,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            OrbitalProjectionProviderThreadFactory()
         ).asCoroutineDispatcher()
   }

   private val projectingScope = CoroutineScope(projectingDispatcher + MDCContext())

   /**
    * processes the given source
    */
   override fun process(
      source: Flow<TypedInstanceWithMetadata>,
      context: QueryContext,
      block: suspend kotlinx.coroutines.CoroutineScope.(item: TypedInstanceWithMetadata) -> Flow<TypedInstanceWithMetadata>
   ): Flow<TypedInstanceWithMetadata> {
      context.cancelFlux.subscribe {
         logger.info { "QueryEngine for queryId ${context.queryId} is cancelling" }
         projectingScope.cancel()
      }

      return source
         .buffer()
         .withIndex()
         .takeWhile { !context.cancelRequested }
         .filter { !context.cancelRequested }
         .distinctUntilChanged()
         .map { emittedResult ->
            //  logger.trace { "Starting to project instance of ${emittedResult.value.type.qualifiedName.shortDisplayName} (index ${emittedResult.index}) to instance of ${projection.type.qualifiedName.shortDisplayName}" }
            projectingScope.async {

               val startTime = Instant.now()
               if (!isActive) {
                  logger.warn { "Query Cancelled exiting!" }
                  cancel()
               }
               block(emittedResult.value)
            }
         }
         .buffer(threadPoolSize).map {
            val result = it.await()
            // logger.trace { "projected or mapped instance of ${projection.type.qualifiedName.shortDisplayName} completed" }
            result
         }.flatMapMerge { it }
   }

   private suspend fun projectItem(
      index: Int,
      emittedResult: TypedInstance,
      declaredSourceType: Type,
      projection: Projection,
      context: QueryContext,
      globalFacts: FactBag,
      inputStartTime: Instant? = null): Deferred<Flow<TypedInstanceWithMetadata>> {
      logger.trace { "Starting to project instance of ${emittedResult.type.qualifiedName.shortDisplayName} (index $index) to instance of ${projection.type.qualifiedName.shortDisplayName}" }
      return projectingScope.async {
         val startTime = inputStartTime ?: Instant.now()
         if (!isActive) {
            logger.warn { "Query Cancelled exiting!" }
            cancel()
         }

         val scopedFacts = buildScopedProjectionFacts(projection, emittedResult, context)
         logger.trace { "project or map instance of ${emittedResult.type.qualifiedName.shortDisplayName} (index ${index}) to instance of ${projection.type.qualifiedName.shortDisplayName}" }

         projectOrMap(
            scopedFacts,
            declaredSourceType,
            context,
            globalFacts,
            emittedResult,
            projection.type,
            startTime
         )
      }
   }
   override fun project(
      source: Flow<TypedInstanceWithMetadata>,
      declaredSourceType: Type,
      projection: Projection,
      context: QueryContext,
      globalFacts: FactBag
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
         .map { emittedResultWithMetadata ->
            val emittedResult = emittedResultWithMetadata.value.instance
            val startTime = emittedResultWithMetadata.value.processingStart
            projectItem(emittedResultWithMetadata.index,
               emittedResult,
               declaredSourceType,
               projection,
               context,
               globalFacts,
               startTime)
         }
         .buffer(threadPoolSize).map {
            val result = it.await()
            logger.trace { "projected or mapped instance of ${projection.type.qualifiedName.shortDisplayName} completed" }
            result
         }.flatMapMerge { it }
   }

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
            projectItem(emittedResult.index,
               emittedResult.value,
               declaredSourceType,
               projection,
               context,
               globalFacts)
         }
         .buffer(threadPoolSize).map {
            val result = it.await()
            logger.trace { "projected or mapped instance of ${projection.type.qualifiedName.shortDisplayName} completed" }
            result
         }.flatMapMerge { it }
   }

   /**
    * When the projection defined a scoped fact:
    * find { foo ) as ( Movie[] ) -> { // <--- that's a scoped fact, right there.
    * }
    *
    * This code returns the actual fact, selecting the value from the inbound value.
    */
   private fun buildScopedProjectionFacts(
      projection: Projection,
      emittedResult: TypedInstance,
      context: QueryContext
   ): List<ScopedFact> {
      return ProjectionFunctionScopeEvaluator.build(
         projection.scopedVars,
         listOf(emittedResult),
         context
      )
   }


   /**
    * Will either directly project the provided value to the target type,
    * or - in the case of arrays - map (iterate and project each member).
    *
    * Also considers scenarios where we're operating on a single item that's part
    * of a broader array (ie., streamed results).
    */
   private suspend fun projectOrMap(
      scopedFacts: List<ScopedFact>,
      declaredSourceType: Type,
      context: QueryContext,
      globalFacts: FactBag,
      emittedResult: TypedInstance,
      projectionType: Type,
      startTime: Instant
   ): Flow<TypedInstanceWithMetadata> {
      val primaryFact = when {
         scopedFacts.isEmpty() -> {
            emittedResult
         }

         scopedFacts.size == 1 -> scopedFacts.single().fact
         else -> {
            // 26-Feb-24:
            // By adding support for multiple scoped facts in the projection context,
            // the above logic (which decides "what is the thing we're projecting?" became less obvious.
            // Considered:
            // - Build an anonymous object with all the scoped facts - however, this is complex at this point of execution,
            //   and if we were to do this, it makes sense to do it inside the query compiler. It also changes scope evaluation rules,
            //   as something like ( f: Foo ) -> {} changes from "f is the name of the scope" to ( { f: Foo } ) -> {} , where "f is the name
            //   of the property on the scope", meaning that named scopes are just properties. That's a big change, and not one I want to undertake
            //   as a side-effect.
            // - Treat the first item as the "thing to project". This is what I went with - turns out that later we just
            //   merge everything into a context set anyway, so it's really only material for deciding what type of projection we're performing.
            //   (the logic that follows next)
            //   "first thing is the thing" follows the pattern used in List / Array mapping in other languages, so feels ok-ish.
            //   This becomes important below in Map A[] -> B[], where if the user has declared multiple scoped facts, we are using the first
            //   as the "value to map"
            scopedFacts.first().fact
         }
      }


      return when {
         // We're working against an array that's being streamed. Common usecase for find { Movie[] } as { ... }[]
         declaredSourceType.isCollection && primaryFact.type.isAssignableTo(declaredSourceType.collectionType!!) && projectionType.isCollection -> {
            doProjection(scopedFacts, context, globalFacts, emittedResult, projectionType.collectionType!!, startTime)
         }
         // Streams.
         // Note that streams are projected to arrays, so projectionType should be T[]
         declaredSourceType.isStream && primaryFact.type.isAssignableTo(declaredSourceType.typeParameters[0]!!) && projectionType.isCollection -> {
            doProjection(scopedFacts, context, globalFacts, emittedResult, projectionType.typeParameters[0], startTime)
         }
         // Map A[] -> B[]. Use-case when mapping a full array that we already have. (eg: find { MovieSchedule } as (Movie[]) -> { .... }[]
         Arrays.isArray(projectionType.paramaterizedName) && Arrays.isArray(primaryFact.typeName) -> {
            // We're projecting Array -> Array, so do a map() on the results.

            if (scopedFacts.isEmpty()) {
               // If this error occurs, understand the flow, as I think all use-cases are covered, and by
               // this stage we should have a scopedFact. However, it could be I missed a scenario, which should
               // just be added to the when clause.
               error("Expected a scoped fact. Perhaps the conditions are non-exhaustive")
            }

            val projectionScopedFacts = if (scopedFacts.size > 1) {
               // At this point, the user has declared mapping A[] -> B[], but they've also declared other
               // facts within the scope.
               // eg:
               // find { MovieSchedule } as ( Movie[], SomethingElse) -> { .... }[]
               // We need to use Movie[] as the value to iterate, so we promote SomethingElse etc., to the globalFacts
               val otherFacts = scopedFacts.drop(1)
               globalFacts.withAdditionalScopedFacts(otherFacts, context.schema)
            } else globalFacts

            doMappingProjection(
               scopedFacts.single(),
               declaredSourceType,
               context,
               projectionScopedFacts,
               emittedResult,
               projectionType,
               startTime
            )
         }

         else -> doProjection(scopedFacts, context, globalFacts, emittedResult, projectionType, startTime)
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
      emittedResult: TypedInstance,
      projectionType: Type,
      startTime: Instant
   ): Flow<TypedInstanceWithMetadata> {
      if (scopedFact.fact is TypedNull) {
         return emptyFlow()
      }
      val collection = scopedFact!!.fact as TypedCollection
      val memberFlows = collection.map { member ->
         val memberFact = ScopedFact(scopedFact.scope.asIteratingScope(), member)
         projectOrMap(
            listOf(memberFact),
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
      scopedFacts: List<ScopedFact>,
      context: QueryContext,
      globalFacts: FactBag,
      emittedResult: TypedInstance,
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
      val projectionContext = if (scopedFacts.isEmpty()) {
         context.only(globalFacts.rootFacts() + emittedResult, scopedFacts = context.scopedFacts)
      } else {
         context.only(globalFacts.rootFacts(), scopedFacts = scopedFacts + context.scopedFacts)
      }
      val buildResult = projectionContext.build(TypeQueryExpression(projectionType))
      return buildResult.results.map {
         it.withProcessingMetadata(asOf = startTime)
      }
   }
}


private class OrbitalProjectionProviderThreadFactory : ThreadFactory {
   private val group: ThreadGroup = Thread.currentThread().threadGroup
   private val threadNumber = AtomicInteger(1)
   private val namePrefix: String = "orbital_projection-"

   override fun newThread(r: Runnable): Thread {
      val t = Thread(
         group, r,
         namePrefix + threadNumber.getAndIncrement(),
         0
      )
      if (t.isDaemon) t.isDaemon = false
      if (t.priority != Thread.NORM_PRIORITY) t.priority = Thread.NORM_PRIORITY
      logger.debug { "created the projection thread - ${t.name}" }
      return t
   }
}
