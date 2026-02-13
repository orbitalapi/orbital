package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import com.fasterxml.jackson.annotation.JsonIgnore
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asTaxiSource
import com.orbitalhq.schemaServer.core.editor.QueryEditor
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import com.orbitalhq.schemas.fqn
import lang.taxi.CompilationException
import lang.taxi.Compiler
import lang.taxi.TaxiDocument

abstract class BaseSourceCreatingEditOperation : SchemaEditOperation() {

   /**
    * Returns the sources with any server-side edits applied.
    * This can be different from what the client submitted.
    *
    * Is used to determine which types, services or queries were affected
    * by this change.
    */
   abstract val sourcesWithServerEdits: List<VersionedSource>

   override val loadExistingState: Boolean = false

   override fun calculateAffectedTypes(): List<Pair<SchemaMemberKind, QualifiedName>> {

      val tokens = Compiler(sourcesWithServerEdits.asTaxiSource()).tokens
      val types = tokens.unparsedTypes.map { (name, _) ->
         SchemaMemberKind.TYPE to name.fqn()
      }
      val services = tokens.unparsedServices.map { (name, _) ->
         SchemaMemberKind.SERVICE to name.fqn()
      }
      val queries = tokens.namedQueries.map { (_,namespace, namedQueryContext) ->
         val queryName = namedQueryContext.queryName().identifier().text
         val queryFqn = QualifiedName.from(namespace, queryName)
         SchemaMemberKind.QUERY to queryFqn
      }
      return types + services + queries
   }
}

class CreateOrReplaceSource(
   val sources: List<VersionedSource>
) : BaseSourceCreatingEditOperation() {


   @JsonIgnore
   override val sourcesWithServerEdits: List<VersionedSource> = sources

   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, SourceEditResult> {

      val edits = sourcesWithServerEdits.map { SourceEdit(it.name, Replace, it.content) }
      return applyEditAndCompile(
         edits,
         sourcePackage, taxiDocument
      )
   }

   override val editKind: EditKind = EditKind.CreateOrReplace
}

class CreateOrReplaceQuery(
   val sources: List<VersionedSource>
) : BaseSourceCreatingEditOperation() {

   @JsonIgnore
   override val sourcesWithServerEdits: List<VersionedSource> = kotlin.run {
      if (sources.size != 1) {
         error("CreateOrReplaceQuery expects to operate on a single source file, but found ${sources.size}")
      }
      val sourceWithQueryBlock = QueryEditor.prependQueryBlockIfMissing(sources.single())
      listOf(sourceWithQueryBlock)
   }

   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, SourceEditResult> {
      val versionedSource = sourcesWithServerEdits.single()
      val edit = SourceEdit(versionedSource.name, Replace, versionedSource.content)
      return applyEditAndCompile(listOf(edit), sourcePackage, taxiDocument)
   }

   override val editKind: EditKind = EditKind.CreateOrReplaceQuery

}
