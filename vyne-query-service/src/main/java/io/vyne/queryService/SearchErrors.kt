package io.vyne.queryService

import io.vyne.VersionedSource
import io.vyne.models.FailedSearch
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.schemas.EmptyTypeCache
import io.vyne.schemas.Type
import lang.taxi.types.PrimitiveType

object ErrorType {
   fun error(message: String): TypedInstance {
//      return TypedNull.create(
//         type,
//         source = SearchError(message)
//      )
      return TypedValue.from(
         type,
         value = message,
         source = FailedSearch(message),
         performTypeConversions = false
      )
   }

   val type = Type(
      "vyne.errors.Error",
      sources = listOf(VersionedSource.sourceOnly("")),
      taxiType = PrimitiveType.STRING,
      typeCache = EmptyTypeCache,
      inheritsFromTypeNames = emptyList()
   )
}

