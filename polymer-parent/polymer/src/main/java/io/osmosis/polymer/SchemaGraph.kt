package io.osmosis.polymer

import io.osmosis.polymer.schemas.Path
import io.osmosis.polymer.schemas.QualifiedName
import io.osmosis.polymer.schemas.Type

interface SchemaPathResolver {
   fun findPath(start: QualifiedName, target: QualifiedName): Path
   fun findPath(start: String, target: String): Path
   fun findPath(start: Type, target: Type) : Path
}
