package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import com.orbitalhq.SourcePackage
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser
import lang.taxi.source

data class AddOrRemoveFieldAnnotation (
   val symbol: QualifiedName,
   val annotationName: String,
   val operation: Operation,
   val fieldName: String,
) : SchemaEditOperation() {
   override val loadExistingState: Boolean = true

   override fun calculateAffectedTypes(): List<Pair<SchemaMemberKind, QualifiedName>> {
      return listOf(SchemaMemberKind.FIELD to symbol)
   }

   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, Pair<SourcePackage, TaxiDocument>> {

      val compiler = buildCompiler(sourcePackage, taxiDocument)
      val (_, typeDefinition) = compiler.tokens.unparsedTypes[symbol.fullyQualifiedName]
         ?: error("Could not find type ${symbol.fullyQualifiedName} in this source")

      val fieldDefinition = (typeDefinition as TaxiParser.TypeDeclarationContext).typeBody().typeMemberDeclaration()
         .firstOrNull { memberDefinition -> memberDefinition.fieldDeclaration().identifier().text == fieldName }
         ?: error("Can not find a field named $fieldName in the definition of type ${symbol.fullyQualifiedName}")

      val edit =  when (operation) {
         Operation.Add -> addAnnotationToField(fieldDefinition)
         Operation.Remove -> removeAnnotationFromField(fieldDefinition)
      }
      return applyEditAndCompile(listOf(edit), sourcePackage, taxiDocument)
   }

   private fun removeAnnotationFromField(fieldDefinition: TaxiParser.TypeMemberDeclarationContext): SourcePackageEdit {
      val matchingAnnotation = fieldDefinition.annotation()
         .filter { it.qualifiedName().text == annotationName }
      require (matchingAnnotation.size == 1) { "Expected a single annotation with name '$annotationName', but found ${matchingAnnotation.size}"}
      val annotation = matchingAnnotation.single()
      return SourcePackageEdit(
         sourceName = fieldDefinition.source().sourceName,
         range = annotation.asCharacterPositionRange(),
         newText = ""
      )
   }

   private fun addAnnotationToField(fieldDefinition: TaxiParser.TypeMemberDeclarationContext): SourcePackageEdit {
      val insertionPoint = fieldDefinition.asCharacterInsertionPoint(EditPosition.BeforePosition)
      return SourcePackageEdit(
         fieldDefinition.source().sourceName,
         insertionPoint,
         "@$annotationName \n"
      )
   }

   override val editKind: EditKind = EditKind.AddOrRemoveFieldAnnotation

   enum class Operation {
      Add, Remove
   }
}



