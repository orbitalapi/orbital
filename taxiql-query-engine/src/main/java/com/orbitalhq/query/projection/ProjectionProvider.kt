package com.orbitalhq.query.projection

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.Projection
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.TypedInstanceWithMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Instances that can run full object projections, including querying and calling services.
 * Providers either run in-process, or can be farmed out to remove processes
 */
interface ProjectionProvider {

    fun project(
       source: Flow<TypedInstance>,
       projection: Projection,
       context: QueryContext,
       globalFacts: FactBag,
       metricTags: MetricTags = MetricTags.NONE
    ): Flow<TypedInstanceWithMetadata>

    fun process(source: Flow<TypedInstanceWithMetadata>, context: QueryContext, block: suspend kotlinx.coroutines.CoroutineScope.(item: TypedInstanceWithMetadata) -> Flow<TypedInstanceWithMetadata>): Flow<TypedInstanceWithMetadata>

    fun project(
        source: Flow<TypedInstanceWithMetadata>,
        projection: Projection,
        context: QueryContext,
        globalFacts: FactBag
    ): Flow<TypedInstanceWithMetadata>



}
