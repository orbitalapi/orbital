package io.vyne.query.projection

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.VyneQueryStatistics
import kotlinx.coroutines.flow.Flow

interface ProjectionProvider {

    fun project(results: Flow<TypedInstance>, context: QueryContext): Flow<Pair<TypedInstance, VyneQueryStatistics>>

}