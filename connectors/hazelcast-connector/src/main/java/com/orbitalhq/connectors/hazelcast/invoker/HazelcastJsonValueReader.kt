package com.orbitalhq.connectors.hazelcast.invoker

import arrow.core.Either
import com.hazelcast.core.HazelcastJsonValue
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schemas.Schema
import lang.taxi.types.ObjectType

object HazelcastJsonValueReader {
   fun toTypedInstance(
      jsonValue: HazelcastJsonValue,
      type: ObjectType,
      schema: Schema,
      dataSource: DataSource
   ): Either<StreamErrorMessage, TypedInstance> {
      return TypedInstance.tryFrom(schema.type(type), jsonValue.value, schema, source = dataSource)
   }
}
