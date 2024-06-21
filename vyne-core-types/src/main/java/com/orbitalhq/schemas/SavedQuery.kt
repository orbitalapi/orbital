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
