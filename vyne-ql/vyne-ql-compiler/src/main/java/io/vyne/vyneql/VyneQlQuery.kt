package io.vyne.vyneql

import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type

data class DiscoveryType(
   val type: QualifiedName,
   val constraints: List<Constraint>
)

data class VyneQlQuery(
   val name: String,
   val queryMode:QueryMode,
   val parameters: Map<String, QualifiedName>,
   val typesToFind: List<DiscoveryType>,
   val projectedType: QualifiedName?
)

enum class QueryMode(val directive: String) {
   FIND_ONE("findOne"),
   FIND_ALL("findAll"); // TODO : Stream
}
