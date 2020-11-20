package io.vyne.vyneql

import io.vyne.models.TypedInstance
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type

data class DiscoveryType(
   val type: QualifiedName,
   val constraints: List<Constraint>
)

data class VyneQlQuery(
   val name: String,
   val facts: Map<String,TypedInstance>,
   val queryMode: QueryMode,
   val parameters: Map<String, QualifiedName>,
   val typesToFind: List<DiscoveryType>,
   val projectedType: ProjectedType?
)

enum class QueryMode(val directive: String) {
   FIND_ONE("findOne"),
   FIND_ALL("findAll"); // TODO : Stream
}


typealias VyneQLQueryString = String

data class ProjectedType(val concreteTypeName: QualifiedName?, val anonymousTypeDefinition: AnonymousTypeDefinition?) {
   companion object {
      fun fromConcreteTypeOnly(qualifiedName: QualifiedName) = ProjectedType(qualifiedName, null)
      fun fomAnonymousTypeOnly(anonymousTypeDefinition: AnonymousTypeDefinition) = ProjectedType(null, anonymousTypeDefinition)
   }
}

data class AnonymousTypeDefinition(
   val isList: Boolean = false,
   val fields: List<AnonymousFieldDefinition>)

interface AnonymousFieldDefinition { val fieldName: String }
// Anonymous field definitions like:
// orderId
// productId: ProductId
data class SimpleAnonymousFieldDefinition(
   override val fieldName: String,
   val fieldType: QualifiedName
): AnonymousFieldDefinition

// Anonymous field Definitions like:
// traderEmail : EmailAddress(from this.traderUtCode)
data class SelfReferencedFieldDefinition(
   override val fieldName: String,
   val fieldType: QualifiedName,
   val referenceFieldName: String,
   val referenceFieldContainingType: QualifiedName): AnonymousFieldDefinition

// Anonymous field Definitions like:
//    salesPerson {
//        firstName : FirstName
//        lastName : LastName
//    }(from this.salesUtCode)
data class ComplexFieldDefinition(
   override val fieldName: String,
   val referenceFieldName: String,
   val referenceFieldContainingType: QualifiedName,
   val fieldDefinitions: List<SimpleAnonymousFieldDefinition>
) : AnonymousFieldDefinition
