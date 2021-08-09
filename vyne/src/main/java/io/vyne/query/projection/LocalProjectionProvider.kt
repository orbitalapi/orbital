package io.vyne.query.projection

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.VyneQueryStatistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import java.util.concurrent.Executors

private val projectingDispatcher = Executors.newFixedThreadPool(16).asCoroutineDispatcher()

class LocalProjectionProvider : ProjectionProvider {

    private val projectingScope = CoroutineScope(projectingDispatcher)

    override fun project(results: Flow<TypedInstance>, context: QueryContext): Flow<Pair<TypedInstance, VyneQueryStatistics>>
    {

        // This pattern aims to allow the concurrent execution of multiple flows.
        // Normally, flow execution is sequential - ie., one flow must complete befre the next
        // item is taken.  buffer() is used here to allow up to n parallel flows to execute.
        // MP: @Anthony - please leave some comments here that describe the rationale for
        // map { async { .. } }.flatMapMerge { await }
        return results.buffer().withIndex()
            .filter { !context.cancelRequested }.map {
                projectingScope.async {
                    val actualProjectedType = context.projectResultsTo?.collectionType ?: context.projectResultsTo
                    val projectionContext = context.only(it.value)
                    val buildResult = projectionContext.build(actualProjectedType!!.qualifiedName)
                    buildResult.results.map { it to  projectionContext.vyneQueryStatistics}
                }
            }
            .buffer(16).map { it.await() }.flatMapMerge { it }


    }

}