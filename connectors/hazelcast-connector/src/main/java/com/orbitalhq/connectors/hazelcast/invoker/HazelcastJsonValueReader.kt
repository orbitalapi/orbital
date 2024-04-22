package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.core.HazelcastJsonValue
import com.hazelcast.nio.serialization.genericrecord.GenericRecord
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Schema
import lang.taxi.types.ObjectType

object HazelcastJsonValueReader {
   fun toTypedInstance(
      jsonValue: HazelcastJsonValue,
      type: ObjectType,
      schema: Schema,
      dataSource: DataSource
   ): TypedInstance {
      return TypedInstance.from(schema.type(type), jsonValue.value, schema, source = dataSource)
   }
}
