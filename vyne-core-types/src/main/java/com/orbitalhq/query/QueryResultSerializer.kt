package com.orbitalhq.query

import com.orbitalhq.models.TypedInstance

interface QueryResultSerializer {
   fun serialize(item: TypedInstance): Any?
}
