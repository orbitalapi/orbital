package com.orbitalhq.connectors.redis.serialization

import arrow.core.Either
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schemas.Schema
import lang.taxi.types.ObjectType

object RedisJsonValueReader {
   fun toTypedInstance(
      jsonString: String,
      type: ObjectType,
      schema: Schema,
      dataSource: DataSource
   ): Either<StreamErrorMessage, TypedInstance> {
      return TypedInstance.tryFrom(schema.type(type), jsonString, schema, source = dataSource)
   }
}
