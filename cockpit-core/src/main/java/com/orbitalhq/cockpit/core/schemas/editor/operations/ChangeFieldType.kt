package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import com.orbitalhq.SourcePackage
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser
import lang.taxi.source

data class ChangeFieldType(
   val symbol: QualifiedName,
   val fieldName: String,
   val newReturnType: QualifiedName,
   val nullable: Boolean
) : SchemaEditOperation() {
   override val loadExistingState: Boolean = true

   override fun calculateAffectedTypes(): List<Pair<SchemaMemberKind, QualifiedName>> {
      return listOf(SchemaMemberKind.TYPE to symbol)
   }
   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, SourceEditResult> {


      val compiler = buildCompiler(sourcePackage, taxiDocument)
      val (_, typeDefinition) = compiler.tokens.unparsedTypes[symbol.fullyQualifiedName]
         ?: error("Could not find type ${symbol.fullyQualifiedName} in this source")

      val fieldDefinition = (typeDefinition as TaxiParser.TypeDeclarationContext).typeBody().typeMemberDeclaration()
         .firstOrNull { memberDefinition -> memberDefinition.fieldDeclaration().identifier().text == fieldName }
         ?: error("Can not find a field named $fieldName in the definition of type ${symbol.fullyQualifiedName}")

      val fieldReturnTypeDefinition =
         fieldDefinition.fieldDeclaration()?.fieldTypeDeclaration()?.typeExpression()?.nullableTypeReference()?.typeReference()
            ?: error("Field $fieldName does not define a type")

      val mutation = SourceEdit(
         fieldReturnTypeDefinition.source().sourceName,
         fieldReturnTypeDefinition.asCharacterPositionRange(),
         // TODO: we need a test for this
         newReturnType.parameterizedName.let{ name -> if (nullable) "$name?" else name }
      )
      return applyEditAndCompile(listOf(mutation), sourcePackage, taxiDocument)

   }


   override val editKind: EditKind = EditKind.ChangeFieldType
}
