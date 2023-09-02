package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asTaxiSource
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import com.orbitalhq.schemas.fqn
import lang.taxi.CompilationException
import lang.taxi.Compiler
import lang.taxi.TaxiDocument

data class CreateOrReplaceSource(
   val sources: List<VersionedSource>
) : SchemaEditOperation() {
   override val loadExistingState: Boolean = false

   override fun calculateAffectedTypes(): List<Pair<SchemaMemberKind, QualifiedName>> {

      val tokens = Compiler(sources.asTaxiSource()).tokens
      val types = tokens.unparsedTypes.map { (name, _) ->
         SchemaMemberKind.TYPE to name.fqn()
      }
      val services = tokens.unparsedServices.map { (name, _) ->
         SchemaMemberKind.SERVICE to name.fqn()
      }
      return types + services
   }

   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, Pair<SourcePackage, TaxiDocument>> {

      val edits = sources.map { SourcePackageEdit(it.name, Replace, it.content) }
      return applyEditAndCompile(
         edits,
         sourcePackage, taxiDocument
      )
   }

   override val editKind: EditKind = EditKind.CreateOrReplace
}
