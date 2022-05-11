package io.vyne.query

import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import java.util.UUID

object VyneQlGrammar {
   const val QUERY_TYPE_NAME = "vyne.vyneQl.VyneQlQuery"
   const val QUERY_TYPE_TAXI = """namespace vyne.vyneQl { type VyneQlQuery inherits String }"""
   const val GRAMMAR_NAME = "vyneQl"
}

/**
 * A data source that describes how a query was constructed - ie.,
 * the inputs that were used to generate the query itself.
 */
data class ConstructedQueryDataSource(
   val inputs: List<TypedInstance>,
   override val id: String = UUID.randomUUID().toString()
) : DataSource {
   override val name: String = "Constructed query"
   override val failedAttempts: List<DataSource> = emptyList()
}
