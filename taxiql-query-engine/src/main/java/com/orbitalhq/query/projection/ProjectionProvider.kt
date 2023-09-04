package com.orbitalhq.query.projection

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.query.Projection
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.VyneQueryStatistics
import kotlinx.coroutines.flow.Flow

interface ProjectionProvider {

    fun project(results: Flow<TypedInstance>, projection: Projection, context: QueryContext, globalFacts: FactBag): Flow<Pair<TypedInstance, VyneQueryStatistics>>

}
