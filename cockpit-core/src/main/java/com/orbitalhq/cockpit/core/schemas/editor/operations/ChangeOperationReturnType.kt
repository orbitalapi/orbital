package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import com.orbitalhq.SourcePackage
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser
import lang.taxi.TaxiParser.ServiceBodyMemberContext
import lang.taxi.source

data class ChangeOperationReturnType(
   val symbol: QualifiedName,
   val newReturnType: QualifiedName
) : SchemaEditOperation() {
   override val loadExistingState: Boolean = true

   override fun calculateAffectedTypes(): List<Pair<SchemaMemberKind, QualifiedName>> {
      return listOf(SchemaMemberKind.OPERATION to symbol)
   }

   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, SourceEditResult> {
      val compiler = buildCompiler(sourcePackage, taxiDocument)
      val (serviceName, operationName) = OperationNames.serviceAndOperation(symbol)
      val (_, serviceDefinition) = compiler.tokens.unparsedServices[serviceName]
         ?: error("Could not find service $serviceName in this source")

      val operationDefinition = serviceDefinition.serviceBody().serviceBodyMember()
         .singleOrNull { member -> member.name() == operationName }
         ?: error("Can not find an operation named $operationName in the definition of service $serviceName")

      val edit = when {
         operationDefinition.serviceOperationDeclaration() != null -> applyToApiOperation(operationDefinition.serviceOperationDeclaration())
         operationDefinition.tableDeclaration() != null -> applyToTableOperation(operationDefinition.tableDeclaration())
         operationDefinition.streamDeclaration() != null -> applyToStreamOperation(operationDefinition.streamDeclaration())
         else -> error("Unexpected type of service member: ${operationDefinition.source().content}")
      }

      return applyEditAndCompile(listOf(edit), sourcePackage, taxiDocument)
   }

   private fun applyToStreamOperation(streamDeclaration: TaxiParser.StreamDeclarationContext): SourceEdit {
      return SourceEdit(
         streamDeclaration.source().sourceName,
         streamDeclaration.typeReference().asCharacterPositionRange(),
         newReturnType.parameterizedName
      )
   }

   private fun applyToTableOperation(tableDeclaration: TaxiParser.TableDeclarationContext): SourceEdit {
      return SourceEdit(
         tableDeclaration.source().sourceName,
         tableDeclaration.typeReference().asCharacterPositionRange(),
         newReturnType.parameterizedName
      )
   }

   private fun applyToApiOperation(serviceOperationDeclaration: TaxiParser.ServiceOperationDeclarationContext): SourceEdit {
      return SourceEdit(
         serviceOperationDeclaration.source().sourceName,
         serviceOperationDeclaration.operationSignature().operationReturnType().typeReference().asCharacterPositionRange(),
         newReturnType.parameterizedName
      )
   }

   override val editKind: EditKind = EditKind.ChangeOperationReturnType
}

private fun ServiceBodyMemberContext.name(): String {
   return when {
      this.serviceOperationDeclaration() != null -> this.serviceOperationDeclaration().operationSignature()
         .identifier().text

      this.tableDeclaration() != null -> this.tableDeclaration().identifier().text
      this.streamDeclaration() != null -> this.streamDeclaration().identifier().text
      this.queryOperationDeclaration() != null -> this.queryOperationDeclaration().identifier().text
      else -> error("Unexpected type of service member: ${this.source().content}")
   }
}
