package com.orbitalhq.query.projection

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.query.Projection
import com.orbitalhq.query.QueryContext
import kotlinx.coroutines.flow.Flow

/**
 * Instances that can run full object projections, including querying and calling services.
 * Providers either run in-process, or can be farmed out to remove processes
 */
interface ProjectionProvider {

    fun project(results: Flow<TypedInstance>, projection: Projection, context: QueryContext, globalFacts: FactBag): Flow<TypedInstance>

}
