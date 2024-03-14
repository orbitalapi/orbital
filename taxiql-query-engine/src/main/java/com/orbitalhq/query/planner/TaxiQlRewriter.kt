package com.orbitalhq.query.planner

import lang.taxi.CompilerTokenCache
import lang.taxi.TaxiParser
import lang.taxi.query.TaxiQLQueryString
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Modifies a TaxiQL Query
 */
class TaxiQlRewriter {
   private val tokenCache = CompilerTokenCache()
   /**
    * Given a streaming query, appends a new stream source in the stream {} directive
    */
   fun appendStreamSource(original: TaxiQLQueryString, streamSource: String): TaxiQLQueryString {
      val query = parseQuery(original)
      val queryDirective = query.queryBody().queryOrMutation().queryDirective().text
      require(queryDirective == "stream") { "Expected a stream query, but was $queryDirective"}
      val queryExpression = query.queryBody()?.queryOrMutation()?.expressionGroup()
         ?: error("Invalid TaxiQL: No Type list is present.")
      val lastType = queryExpression.children.last()
      require (lastType is ParserRuleContext) { "Error rewriting TaxiQL to append stream source - expected a parser rule but found ${lastType::class.simpleName}" }
      val charIndexAtEndOfLastType = lastType.stop.stopIndex

      val amended = StringBuilder(original).insert(charIndexAtEndOfLastType + 1, " | $streamSource")
         .toString()
      return amended
   }

   private fun parseQuery(original: TaxiQLQueryString): TaxiParser.AnonymousQueryContext {
      val parseResult = tokenCache.parse(CharStreams.fromString(original))

      val query = parseResult.tokens.anonymousQueries.single().second
      return query
   }

   fun appendQueryAnnotationIfNotPresent(original: TaxiQLQueryString, annotation: String): TaxiQLQueryString {
      val parseResult = parseQuery(original)
      val annotations = parseResult.queryBody().annotation() ?: emptyList()
      if (annotations.any { it.text.startsWith(annotation) }) {
         return original
      }
      val insertPosition = parseResult.queryBody().annotation()?.lastOrNull()?.stop?.stopIndex
         ?: parseResult.queryBody()?.queryOrMutation()?.start?.startIndex?.let { it - 1 }
         ?: error("Could not find an appropriate place to insert the annotation - does this query have a body?")
      val lineBreakAlreadyPresent = original[insertPosition + 1] == '\n'
      var insertText = annotation
      if (insertPosition != -1) {
         insertText = "\n" + insertText
      }
      if (!lineBreakAlreadyPresent) {
         insertText += "\n"
      }
      val amended = StringBuilder(original).insert(insertPosition + 1, insertText)
         .toString()

      return amended
   }
}
