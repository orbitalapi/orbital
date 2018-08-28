package io.vyne.query

// TODO : facts should be QualifiedName -> TypedInstance, but need to get
// json deserialization working for that.
data class Query(val queryString: String, val facts: Map<String, Any> = emptyMap(), val queryMode: QueryMode = QueryMode.DISCOVER)


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
