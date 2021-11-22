package io.vyne.models

import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A structure which is BOTH
 *  - A collection of facts, modelled as a FactBag, which allows searching using FactDiscovery strategies,
 *  - A map of field to attributes, used when building an object and searching by field name.
 */
class FieldAndFactBag(private val fields: Map<AttributeName, TypedInstance>, private  val otherFacts: List<TypedInstance>, private val schema: Schema) :
   Map<AttributeName, TypedInstance> by fields,
   CopyOnWriteFactBag(
      facts = CopyOnWriteArrayList(fields.values + otherFacts),
      schema = schema
   ) {

   override fun merge(other: FactBag): FactBag {
      return if (other is FieldAndFactBag) {
         FieldAndFactBag(this.fields + other.fields, this.otherFacts + other.otherFacts, schema)
      } else {
         FieldAndFactBag(this.fields, this.otherFacts + other.toList(), schema)
      }
   }
   }
