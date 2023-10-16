package com.orbitalhq.query

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Schema

interface QueryResultSerializer {
   fun serialize(item: TypedInstance, schema: Schema): Any?
}
