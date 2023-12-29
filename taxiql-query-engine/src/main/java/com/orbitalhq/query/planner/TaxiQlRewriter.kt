package com.orbitalhq.query.planner

import lang.taxi.CompilerTokenCache
import lang.taxi.query.TaxiQLQueryString
import org.antlr.v4.runtime.CharStreams

/**
 * Modifies a TaxiQL Query
 */
class TaxiQlRewriter {

   /**
    * Given a streaming query, appends a new stream source in the stream {} directive
    */
   fun appendStreamSource(original: TaxiQLQueryString, streamSource: String): TaxiQLQueryString {
      val parseResult = CompilerTokenCache().parse(CharStreams.fromString(original))

      val query = parseResult.tokens.anonymousQueries.single().second
      val queryDirective = query.queryBody().queryOrMutation().queryDirective().text
      require(queryDirective == "stream") { "Expected a stream query, but was $queryDirective"}
      val typeList = query.queryBody()?.queryOrMutation()?.queryTypeList()
         ?: error("Invalid TaxiQL: No Type list is present.")
      val lastType = typeList.fieldTypeDeclaration().last()
      val charIndexAtEndOfLastType = lastType.stop.stopIndex

      val amended = StringBuilder(original).insert(charIndexAtEndOfLastType + 1, " | $streamSource")
         .toString()
      return amended
   }
}
