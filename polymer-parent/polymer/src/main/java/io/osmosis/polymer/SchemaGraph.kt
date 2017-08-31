package io.osmosis.polymer

import io.osmosis.polymer.schemas.Path
import io.osmosis.polymer.schemas.QualifiedName
import io.osmosis.polymer.schemas.Type

// Evaluating a path independently of the search result
// no longer makes sense, as the actual path isn't accurately known
// until we try to evaluate it at search time.
@Deprecated("Use SearchResult.path")
interface SchemaPathResolver {
   @Deprecated("Use SearchResult.path")
   fun findPath(start: QualifiedName, target: QualifiedName): Path
   @Deprecated("Use SearchResult.path")
   fun findPath(start: String, target: String): Path
   @Deprecated("Use SearchResult.path")
   fun findPath(start: Type, target: Type) : Path
}
