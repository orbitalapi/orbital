package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import com.orbitalhq.SourcePackage
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser.TypeDeclarationContext
import lang.taxi.source

data class ChangeInheritedType(
   val symbol: QualifiedName,
   val newBaseType: QualifiedName
) : SchemaEditOperation() {
   override val loadExistingState: Boolean = true

   override fun calculateAffectedTypes(): List<Pair<SchemaMemberKind, QualifiedName>> {
      return listOf(SchemaMemberKind.TYPE to symbol)
   }

   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, Pair<SourcePackage, TaxiDocument>> {
      val compiler = buildCompiler(sourcePackage, taxiDocument)
      val (_, token) = compiler.tokens.unparsedTypes[symbol.fullyQualifiedName]
         ?: error("Could not find type ${symbol.fullyQualifiedName} in this source")

      val typeDeclarationContext = token as TypeDeclarationContext
      require(
         (typeDeclarationContext
            .listOfInheritedTypes()
            ?.typeReference()
            ?.size ?: 0) <= 1
      ) { "Updating inheritance only supported on types with 1 or fewer types" }


      val edit: SourcePackageEdit = when {
         typeDeclarationContext.listOfInheritedTypes() == null -> addNewInheritedType(typeDeclarationContext)
         typeDeclarationContext.listOfInheritedTypes().typeReference().isNullOrEmpty() -> addNewInheritedType(typeDeclarationContext)

         typeDeclarationContext.listOfInheritedTypes().typeReference().size == 1 -> replaceInheritedType(
            typeDeclarationContext
         )

         else -> error("There should be only 0 or 1 inherited types") // you shouldn't hit this
      }
      return applyEditAndCompile(listOf(edit), sourcePackage, taxiDocument)
   }

   private fun replaceInheritedType(typeDeclarationContext: TypeDeclarationContext): SourcePackageEdit {
      return SourcePackageEdit(
         sourceName = typeDeclarationContext.source().sourceName,
         range = typeDeclarationContext.listOfInheritedTypes().typeReference().single()
            .asCharacterPositionRange(),
         newText = newBaseType.parameterizedName
      )
   }

   private fun addNewInheritedType(typeDeclarationContext: TypeDeclarationContext): SourcePackageEdit {
      return SourcePackageEdit(
         sourceName = typeDeclarationContext.source().sourceName,
         range = typeDeclarationContext.identifier().asCharacterInsertionPoint(EditPosition.AfterPosition),
         newText = " inherits ${newBaseType.parameterizedName}"
      )
   }

   override val editKind: EditKind = EditKind.ChangeInheritedType
}



