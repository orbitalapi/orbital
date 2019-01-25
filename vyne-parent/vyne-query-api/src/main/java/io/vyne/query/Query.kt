package io.vyne.query

import io.vyne.FactSetId
import io.vyne.FactSets


data class Fact(val typeName: String, val value: Any, val factSetId: FactSetId = FactSets.DEFAULT)

// TODO : facts should be QualifiedName -> TypedInstance, but need to get
// json deserialization working for that.
data class Query(val queryString: String, val facts: List<Fact> = emptyList(), val queryMode: QueryMode = QueryMode.DISCOVER) {
   constructor(queryString: String, facts: Map<String, Any> = emptyMap(), queryMode: QueryMode = QueryMode.DISCOVER) : this(queryString, facts.map { Fact(it.key, it.value) }, queryMode)
}


enum class QueryMode {
   /**
    * Find a single value
    */
   DISCOVER,

   /**
    * Find all the values
    */
   GATHER
}
