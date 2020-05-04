package io.vyne

import java.io.Serializable

// TODO : Why do I need this AND VersionedSchema?  There's clearly a big overlap
@Deprecated("Use VersionedSource instead", replaceWith = ReplaceWith("VersionedSource"))
data class NamedSource(val taxi: String, val sourceName: String) : Serializable {
   companion object {
      fun unnamed(taxi: String) = NamedSource(taxi, "<unknown>")
      fun unnamed(taxi: List<String>): List<NamedSource> = taxi.map { unnamed(it) }
   }
}
