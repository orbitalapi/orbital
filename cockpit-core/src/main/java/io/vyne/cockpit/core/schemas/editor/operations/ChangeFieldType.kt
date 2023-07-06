package io.vyne.cockpit.core.schemas.editor.operations

import arrow.core.Either
import io.vyne.SourcePackage
import io.vyne.schemas.QualifiedName
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser
import lang.taxi.source

data class ChangeFieldType(
   val symbol: QualifiedName,
   val fieldName: String,
   val newReturnType: QualifiedName
) : SchemaEditOperation() {
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

      val fieldReturnTypeDefinition =
         fieldDefinition.fieldDeclaration()?.fieldTypeDeclaration()?.optionalTypeReference()?.typeReference()
            ?: error("Field $fieldName does not define a type")

      val mutation = SourcePackageEdit(
         fieldReturnTypeDefinition.source().sourceName,
         fieldReturnTypeDefinition.asCharacterPositionRange(),
         newReturnType.parameterizedName
      )
      return applyEditAndCompile(listOf(mutation), sourcePackage, taxiDocument)

   }


   override val editKind: EditKind = EditKind.ChangeFieldType
}
