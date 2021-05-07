package io.vyne.queryService

import io.vyne.VersionedSource
import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.schemas.EmptyTypeCache
import io.vyne.schemas.Type
import lang.taxi.types.PrimitiveType
import java.util.*

object ErrorType {
   fun error(message: String): TypedInstance {
//      return TypedNull.create(
//         type,
//         source = SearchError(message)
//      )
      return TypedValue.from(
         type,
         value = message,
         source = SearchError(message),
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

data class SearchError(val message: String) : DataSource {
   override val name: String = "SearchError"
   override val id: String = UUID.randomUUID().toString()
}
