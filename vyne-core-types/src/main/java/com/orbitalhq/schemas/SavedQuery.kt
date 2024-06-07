package com.orbitalhq.schemas

import com.orbitalhq.VersionedSource
import com.orbitalhq.schemas.SavedQuery.QueryKind
import lang.taxi.CompilationMessage
import lang.taxi.annotations.HttpOperation
import lang.taxi.annotations.WebsocketOperation
import lang.taxi.errors
import lang.taxi.query.QueryMode
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery

data class SavedQuery(
   val name: QualifiedName,
   val sources: List<VersionedSource>,
   val queryKind: QueryKind,
   val httpEndpoint: HttpOperation? = null,
   val websocketOperation: WebsocketOperation? = null
) {
   enum class QueryKind {
      Stream,
      Query;

      companion object {
         fun forQueryMode(queryMode: QueryMode): QueryKind {
            return when (queryMode) {
               QueryMode.STREAM -> Stream
               else -> Query
            }
         }
      }
   }
}

/**
 * The result of a parser request.
 * Contains both the query and the SavedQuery
 * metadata if successful, or error messages
 * if unsuccesful.
 *
 * Generally, the query is not yet saved, so we don't
 * have as much information as a SavedQuery
 */
data class ParsedQuery(
   val taxi: TaxiQLQueryString,
   val queryKind: QueryKind?,
   // Unlike a saved query, the name could be null
   val name: QualifiedName?,
   // TODO : Plan is to add query diagram
   // data here, so that we can
   // render a query plan on the UI
   val messages: List<CompilationMessage>
) {
   constructor(query:TaxiQlQuery, messages: List<CompilationMessage> = emptyList()):this(
      query.source,
      QueryKind.forQueryMode(query.queryMode),
      query.name.toVyneQualifiedName(),
      messages
   )
   val hasErrors = messages.errors().isNotEmpty()
}
