package io.vyne.queryService.schemas.editor.splitter

import io.vyne.VersionedSource
import io.vyne.schemaServer.editor.FileNames
import lang.taxi.services.Service
import lang.taxi.types.CompilationUnit
import lang.taxi.types.ImportableToken
import lang.taxi.types.ObjectType

/**
 * Simple implementation, where we place a single type per file.
 * This is verbose on the file system, but makes for managing edits easier, since
 * we don't need to worry about insertions / modifications within the middle of a file.
 * However, this likely won't scale, and we need to find a better approach
 */
object SingleTypePerFileSplitter : SourceSplitter {
   override fun toVersionedSources(typesAndSources: List<Pair<ImportableToken, List<CompilationUnit>>>): List<VersionedSource> {
      val versionedSources = typesAndSources.map { (type, compilationUnits) ->
         val source =
            compilationUnits.joinToString("\n") { it.source.content }//reconstructSource(type, compilationUnits)

         val imports = when (type) {
            is ObjectType -> type.referencedTypes
            is Service -> type.referencedTypes
            else -> emptyList()
         }.flatMap { it.typeParameters() + it }
            .filter { it.toQualifiedName().namespace != "lang.taxi" }

         val sourceWithImports = if (imports.isNotEmpty()) {
            imports.joinToString("\n") { "import ${it.qualifiedName}" } + "\n" + source
         } else {
            source
         }
         VersionedSource.unversioned(
            FileNames.fromQualifiedName(type.qualifiedName),
            sourceWithImports
         )
      }
      return versionedSources
   }
}
