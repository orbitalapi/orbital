package com.orbitalhq.query.projection

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.Projection
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.TypedInstanceWithMetadata
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.flow.Flow

/**
 * Instances that can run full object projections, including querying and calling services.
 * Providers either run in-process, or can be farmed out to remove processes
 */
interface ProjectionProvider {

    fun project(
       source: Flow<TypedInstance>,
       /**
        * In the case of streams and arrays, the declared source type may be of Foo[],
        * and the projection may be of type Foo.
        * This determines how the downstream projection will be performed (map vs transform)
        */
       declaredSourceType: Type,
       projection: Projection,
       context: QueryContext,
       globalFacts: FactBag,
       metricTags: MetricTags = MetricTags.NONE
    ): Flow<TypedInstanceWithMetadata>

}
