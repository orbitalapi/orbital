package io.vyne.query.projection

import io.vyne.models.TypedInstance
import io.vyne.models.facts.FactBag
import io.vyne.query.Projection
import io.vyne.query.QueryContext
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.VyneQueryStatistics
import io.vyne.schemas.Type
import kotlinx.coroutines.flow.Flow

interface ProjectionProvider {

    fun project(results: Flow<TypedInstance>, projection: Projection, context: QueryContext, globalFacts: FactBag): Flow<Pair<TypedInstance, VyneQueryStatistics>>

}
