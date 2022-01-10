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

         // Here we remove any imports that were present in the compilation units.  We'll concat them
         // with other imports we detect (trimming for duplicates)
         val (sourceInImports, sourceWithoutImports) = removeImports(source)
         val importsFromTypes = when (type) {
            is ObjectType -> type.referencedTypes
            is Service -> type.referencedTypes
            else -> emptyList()
         }.flatMap { it.typeParameters() + it }
            .filter { it.toQualifiedName().namespace != "lang.taxi" }
            .map { "import ${it.qualifiedName}" }
         val imports = (sourceInImports + importsFromTypes).distinct()

         val sourceWithImports = if (imports.isNotEmpty()) {
            imports.joinToString("\n") + "\n" + sourceWithoutImports
         } else {
            sourceWithoutImports
         }
         VersionedSource.unversioned(
            FileNames.fromQualifiedName(type.qualifiedName),
            sourceWithImports
         )
      }
      return versionedSources
   }

   private fun removeImports(source: String): Pair<List<String>, String> {
      val imports = source
         .lines()
         .filter { it.trim().startsWith("import") }
      val sourceWithoutImports = source.lines()
         .filterNot { it.trim().startsWith("import") }
         .joinToString("\n")
      return imports to sourceWithoutImports
   }
}
