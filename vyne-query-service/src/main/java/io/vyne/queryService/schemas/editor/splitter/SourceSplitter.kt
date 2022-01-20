package io.vyne.queryService.schemas.editor.splitter

import io.vyne.VersionedSource
import lang.taxi.types.CompilationUnit
import lang.taxi.types.ImportableToken

/**
 * Takes a collection of compiled sources, and splits out to the
 * source files that constitue them.
 *
 * Different strategies can apply different splitting mechansims
 */
interface SourceSplitter {

   fun toVersionedSources(typesAndSources: List<Pair<ImportableToken, List<CompilationUnit>>>): List<VersionedSource>
}
