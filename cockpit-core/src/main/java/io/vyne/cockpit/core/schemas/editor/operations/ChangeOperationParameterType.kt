package io.vyne.cockpit.core.schemas.editor.operations

import arrow.core.Either
import io.vyne.SourcePackage
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.source

data class ChangeOperationParameterType(
   val symbol: QualifiedName,
   val parameterName: String,
   val newType: QualifiedName
) : SchemaEditOperation() {
   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, Pair<SourcePackage, TaxiDocument>> {
      val compiler = buildCompiler(sourcePackage, taxiDocument)
      val (serviceName, operationName) = OperationNames.serviceAndOperation(symbol.fullyQualifiedName)
      val (_, serviceDefinition) = compiler.tokens.unparsedServices[serviceName]
         ?: error("Could not find service ${symbol.fullyQualifiedName} in this source")

      val operation = serviceDefinition.serviceBody().serviceBodyMember()
         .firstOrNull { it.serviceOperationDeclaration()?.operationSignature()?.identifier()?.text == operationName }
         ?: error("No operation named $operationName was found in service $serviceName")

      val parameter = operation.serviceOperationDeclaration().operationSignature()
         .operationParameterList()?.operationParameter()
         ?.firstOrNull { it.parameterName().identifier().text == parameterName }
         ?: error("No parameter named $parameterName is present on operation $operationName")

      val mutation = SourcePackageEdit(
         operation.source().sourceName,
         parameter.optionalTypeReference().asCharacterPositionRange(),
         newType.parameterizedName
      )
      return applyEditAndCompile(listOf(mutation), sourcePackage, taxiDocument)
   }

   override val editKind: EditKind = EditKind.ChangeOperationParameterType

}
