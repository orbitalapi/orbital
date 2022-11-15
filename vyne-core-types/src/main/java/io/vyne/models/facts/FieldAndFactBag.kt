package io.vyne.models.facts

import io.vyne.models.TypedInstance
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A structure which is BOTH
 *  - A collection of facts, modelled as a FactBag, which allows searching using FactDiscovery strategies,
 *  - A map of field to attributes, used when building an object and searching by field name.
 */
class FieldAndFactBag(
   private val fields: Map<AttributeName, TypedInstance>,
   private val otherFacts: List<TypedInstance>,
   scopedFacts: List<ScopedFact>,
   private val schema: Schema
) :
   Map<AttributeName, TypedInstance> by fields,
   CopyOnWriteFactBag(
      facts = CopyOnWriteArrayList(fields.values + otherFacts),
      scopedFacts = scopedFacts,
      schema = schema
   ) {

   override fun merge(other: FactBag): FactBag {
      return if (other is FieldAndFactBag) {
         FieldAndFactBag(this.fields + other.fields, this.otherFacts + other.otherFacts, this.scopedFacts + other.scopedFacts, schema)
      } else {
         FieldAndFactBag(this.fields, this.otherFacts + other.toList(), this.scopedFacts + other.scopedFacts, schema)
      }
   }

   override fun merge(fact: TypedInstance): FactBag {
      return FieldAndFactBag(
         this.fields, this.otherFacts + fact, scopedFacts, schema
      )
   }

   override fun copy(): FieldAndFactBag {
      return FieldAndFactBag(fields.toMap(), otherFacts.toList(), scopedFacts, schema)
   }

   override fun excluding(facts: Set<TypedInstance>): FactBag {
      if (facts.isEmpty()) {
         return copy()
      }


      val filteredFacts = otherFacts.filterNot { facts.contains(it) }
      val filteredFields = fields
         .filterNot { (_,value)  -> facts.contains(value) }

      return FieldAndFactBag(
         filteredFields,
         filteredFacts,
         scopedFacts,
         schema
      )
   }
}
