package io.vyne.cockpit.core.schemas.editor.operations

import arrow.core.Either
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument

data class CreateOrReplaceSource(
   val sources: List<VersionedSource>
) : SchemaEditOperation() {
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
