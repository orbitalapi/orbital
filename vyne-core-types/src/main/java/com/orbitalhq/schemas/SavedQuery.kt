package com.orbitalhq.schemas

import com.orbitalhq.VersionedSource
import lang.taxi.annotations.HttpOperation
import lang.taxi.query.QueryMode

data class SavedQuery(
   val name: QualifiedName,
   val sources: List<VersionedSource>,
   val queryKind: QueryKind,
   val httpEndpoint: HttpOperation? = null
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

