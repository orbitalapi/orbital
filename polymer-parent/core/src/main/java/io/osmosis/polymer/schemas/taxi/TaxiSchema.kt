package io.osmosis.polymer.schemas.taxi

import io.osmosis.polymer.schemas.*
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.types.ObjectType

class TaxiSchema(document: TaxiDocument) : Schema {
   override val types: Set<Type>
   override val links: Set<Link>
   override val attributes: Set<QualifiedName>

   init {
      val types = mutableSetOf<Type>()
      val links = mutableSetOf<Link>()
      val attributes = mutableSetOf<QualifiedName>()
      document.types.forEach { type: lang.taxi.Type ->

         when (type) {
            is ObjectType -> {
               val typeName = QualifiedName(type.qualifiedName)
               attributes.add(typeName)
               type.fields.forEach { field ->
                  val fieldName = QualifiedName(field.name)
                  attributes.add(fieldName)
                  links.add(Link(typeName, Relationship.HAS_ATTRIBUTE, fieldName, cost = 1))
                  links.add(Link(fieldName, Relationship.IS_ATTRIBUTE_OF, typeName, cost = 1))
               }
            }
         }
      }
      this.links = links
      this.types = types
      this.attributes = attributes
   }

   companion object {
      fun from(taxi: String): TaxiSchema {
         return TaxiSchema(Compiler(taxi).compile())
      }
   }
}
