package com.orbitalhq.schemaServer.core.editor

import com.orbitalhq.VersionedSource
import lang.taxi.Compiler
import lang.taxi.source

object QueryEditor {
   /**
    * Adds a query { } block to an existing find { ... } TaxiQL
    * query, if not already present.
    *
    * The name of the query is taken from the VersionedSource
    */
   fun prependQueryBlockIfMissing(source: VersionedSource): VersionedSource {
      val query = source.content
      val (tokens, errors) = Compiler(query).parseResult
      require(tokens.anonymousQueries.size == 1 || tokens.namedQueries.size == 1) { "Expected exactly one query - there were ${tokens.anonymousQueries.size} unnamed and ${tokens.namedQueries} names queries" }
      if (tokens.namedQueries.size == 1) {
         return source
      }
      val imports = tokens.imports.joinToString("\n") { it.second.source().content }
      val querySource = tokens.anonymousQueries.single().second.source().content

      val indentedSource = querySource.lines().joinToString("\n") { it.prependIndent("   ") }
      val namedQuery = """$imports

query ${source.name.removeSuffix(".taxi")} {
$indentedSource
}""".trim()

      return source.copy(content = namedQuery)
   }

}
