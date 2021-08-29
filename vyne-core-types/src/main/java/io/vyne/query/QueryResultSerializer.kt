package io.vyne.query

import io.vyne.models.TypedInstance

interface QueryResultSerializer {
   fun serialize(item: TypedInstance): Any?
}
