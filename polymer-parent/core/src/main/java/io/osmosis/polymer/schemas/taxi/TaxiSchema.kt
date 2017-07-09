package io.osmosis.polymer.schemas.taxi

import io.osmosis.polymer.schemas.*
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.types.ArrayType
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
                  when (field.type) {
                     is ArrayType -> field.name to TypeReference((field.type as ArrayType).type.qualifiedName.fqn(), isCollection = true)
                     else -> field.name to TypeReference(field.type.qualifiedName.fqn())
                  }

//                  attributes.add(fieldName)
//                  links.add(Link(typeName, Relationship.HAS_ATTRIBUTE, fieldName, cost = 1))
//                  links.add(Link(fieldName, Relationship.IS_ATTRIBUTE_OF, typeName, cost = 1))
               }.toMap()
               types.add(Type(typeName, fields))
            }
            is ArrayType -> TODO()
            else -> types.add(Type(QualifiedName(taxiType.qualifiedName)))
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
