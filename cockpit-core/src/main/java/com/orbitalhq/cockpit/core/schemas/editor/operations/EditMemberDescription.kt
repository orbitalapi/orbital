package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import com.orbitalhq.SourcePackage
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import lang.taxi.CompilationException
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser.TypeDeclarationContext
import lang.taxi.TaxiParser.TypeDocContext
import lang.taxi.source
import lang.taxi.sources.SourceCode
import org.antlr.v4.runtime.ParserRuleContext

data class EditMemberDescription(
   val memberKind: SchemaMemberKind,
   val memberName: String? = null,
   val symbol: QualifiedName,
   val typeDoc: String,
) : SchemaEditOperation() {
   override val loadExistingState: Boolean = true

   override fun calculateAffectedTypes(): List<Pair<SchemaMemberKind, QualifiedName>> {
      return listOf(
         memberKind to symbol
      )
   }

   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException,SourceEditResult> {
      val compiler = buildCompiler(sourcePackage, taxiDocument)

      val (editRange, sourceCode) = when (memberKind) {
         SchemaMemberKind.TYPE -> getTypedocRangeForType(compiler)
         SchemaMemberKind.SERVICE -> getTypedocRangeForService(compiler)
         SchemaMemberKind.OPERATION -> getTypedocRangeForOperation(compiler)
         else -> TODO()
      }
      val typeDocText = "\n[[ $typeDoc ]]\n"
      val edit = SourceEdit(
         sourceCode.sourceName,
         editRange,
         typeDocText
      )
      return applyEditAndCompile(listOf(edit), sourcePackage, taxiDocument)
   }

   private fun getTypedocRangeForOperation(compiler: Compiler): Pair<CharacterPositionRange, SourceCode> {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(symbol.parameterizedName)
      val (_, compilerToken) = compiler.tokens.unparsedServices[serviceName]
         ?: error("Could not find service $serviceName in this source")

      val operationToken = compilerToken.serviceBody().serviceBodyMember().singleOrNull {
         it.tableDeclaration()?.identifier()?.text == operationName ||
            it.streamDeclaration()?.identifier()?.text == operationName ||
            it.serviceOperationDeclaration()?.operationSignature()?.identifier()?.text == operationName
      } ?: error("Could not find operation $operationName on service $serviceName in this source")

      val range = when {
         operationToken.tableDeclaration() != null -> editRangeForTypedocOrBeforeElement(operationToken.tableDeclaration().typeDoc(), operationToken.tableDeclaration())
         operationToken.streamDeclaration() != null -> editRangeForTypedocOrBeforeElement(operationToken.streamDeclaration().typeDoc(), operationToken.streamDeclaration())
         operationToken.serviceOperationDeclaration() != null -> editRangeForTypedocOrBeforeElement(operationToken.serviceOperationDeclaration().typeDoc(), operationToken.serviceOperationDeclaration())
         else -> error("Unhandled branch for finding operation $operationName on service $serviceName")
      }
      return range to operationToken.source()
   }

   private fun getTypedocRangeForService(compiler: Compiler): Pair<CharacterPositionRange, SourceCode> {
      val (_, compilerToken) = compiler.tokens.unparsedServices[symbol.parameterizedName]
         ?: error("Could not find service ${symbol.parameterizedName} in this source")

      return editRangeForTypedocOrBeforeElement(compilerToken.typeDoc(), compilerToken) to compilerToken.source()
   }

   private fun getTypedocRangeForType(
      compiler: Compiler,
   ): Pair<CharacterPositionRange, SourceCode> {
      val (_, compilerToken) = compiler.tokens.unparsedTypes[symbol.parameterizedName]
         ?: error("Could not find type ${symbol.parameterizedName} in this source")
      val typeDefinition = compilerToken as TypeDeclarationContext

      return if (memberName != null) {
         val fieldToken = typeDefinition.typeBody().typeMemberDeclaration()
            .firstOrNull { it.fieldDeclaration().identifier().text == memberName }
            ?: error("Could not find field $memberName in the definition for type ${symbol.parameterizedName}")
         editRangeForTypedocOrBeforeElement(fieldToken.typeDoc(), fieldToken) to typeDefinition.source()
      } else {
         editRangeForTypedocOrBeforeElement(typeDefinition.typeDoc(), typeDefinition) to typeDefinition.source()
      }
   }

   private fun editRangeForTypedocOrBeforeElement(
      typeDocElement: TypeDocContext?,
      elementBeingAnnotated: ParserRuleContext
   ): CharacterPositionRange {
      return typeDocElement?.asCharacterPositionRange()
         ?: elementBeingAnnotated.asCharacterInsertionPoint(EditPosition.BeforePosition)
   }

   override val editKind: EditKind = EditKind.EditMemberDescription
}



