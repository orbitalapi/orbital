package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.annotationFor
import io.vyne.cask.services.CaskServiceSchemaGenerator
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.services.Parameter
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class FindOneByGenerator(val operationGeneratorConfig: OperationGeneratorConfig = OperationGeneratorConfig.empty()): OperationGenerator {
   override fun generate(field: Field?, type: Type): Operation {
      val parameter = Parameter(
         annotations = listOf(Annotation("PathVariable", mapOf("name" to field!!.name))),
         type = TemporalFieldUtils.parameterType(field),
         name = field.name,
         constraints = listOf())

      return Operation(
         name = "${OperationAnnotation.FindOne.annotation}${field.name.capitalize()}",
         parameters = listOf(parameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getFindOneRestPath(type, field)))),
         returnType = type,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = type,
            returnTypeConstraints = listOf(TemporalFieldUtils.constraintFor(field, Operator.EQUAL, field.name)))
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return PrimitiveType.isAssignableToPrimitiveType(field.type) &&
         (annotationFor(field, OperationAnnotation.FindOne.name) != null ||
            annotationFor(field, OperationAnnotation.Id.name) != null ||
            annotationFor(field, OperationAnnotation.Association.name) != null ||
            operationGeneratorConfig.definesOperation(field.type, OperationAnnotation.FindOne))
   }

   override fun expectedAnnotationName(): OperationAnnotation {
      return OperationAnnotation.FindOne
   }

   private fun getFindOneRestPath(type: Type, field: Field): String {
      val typeQualifiedName = type.toQualifiedName()
      val fieldTypeQualifiedName =  TemporalFieldUtils.parameterType(field).toQualifiedName()
      val path = AttributePath.from(typeQualifiedName.toString())
      return "${CaskServiceSchemaGenerator.CaskApiRootPath}${OperationAnnotation.FindOne.annotation}/${path.parts.joinToString("/")}/${field.name}/{$fieldTypeQualifiedName}"
   }
}
