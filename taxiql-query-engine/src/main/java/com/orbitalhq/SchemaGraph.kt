package com.orbitalhq

import com.orbitalhq.schemas.Path
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Type

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
