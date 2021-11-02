package io.vyne.models

import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A structure which is BOTH
 *  - A collection of facts, modelled as a FactBag, which allows searching using FactDiscovery strategies,
 *  - A map of field to attributes, used when building an object and searching by field name.
 */
class FieldAndFactBag(fields: Map<AttributeName, TypedInstance>, otherFacts: List<TypedInstance>, schema: Schema) :
   Map<AttributeName, TypedInstance> by fields,
   CopyOnWriteFactBag(
      facts = CopyOnWriteArrayList(fields.values + otherFacts),
      schema = schema
   )
