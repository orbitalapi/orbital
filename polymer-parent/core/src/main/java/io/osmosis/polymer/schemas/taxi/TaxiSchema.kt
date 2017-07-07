package io.osmosis.polymer.schemas.taxi

import io.osmosis.polymer.schemas.Link
import io.osmosis.polymer.schemas.QualifiedName
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.Type
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
      document.types.forEach { taxiType: lang.taxi.Type ->

         when (taxiType) {
            is ObjectType -> {
               val typeName = QualifiedName(taxiType.qualifiedName)
//               attributes.add(typeName)
               val fields = taxiType.fields.map { field ->
                  field.name to QualifiedName(field.type.qualifiedName)
//                  attributes.add(fieldName)
//                  links.add(Link(typeName, Relationship.HAS_ATTRIBUTE, fieldName, cost = 1))
//                  links.add(Link(fieldName, Relationship.IS_ATTRIBUTE_OF, typeName, cost = 1))
               }.toMap()
               types.add(Type(typeName, fields))
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
