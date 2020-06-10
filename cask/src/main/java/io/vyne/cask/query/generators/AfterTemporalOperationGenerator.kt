package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.annotationFor
import io.vyne.cask.query.generators.TemporalFieldUtils.collectionTypeOf
import io.vyne.cask.query.generators.TemporalFieldUtils.constraintFor
import io.vyne.cask.query.generators.TemporalFieldUtils.parameterFor
import io.vyne.cask.query.generators.TemporalFieldUtils.validate
import io.vyne.cask.services.CaskServiceSchemaGenerator
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class AfterTemporalOperationGenerator: OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val parameterType = TemporalFieldUtils.parameterType(field)
      val afterParameter = parameterFor(
         parameterType,
         TemporalFieldUtils.After,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.After))))
      val greaterThanConstraint = constraintFor(field, Operator.GREATER_THAN, TemporalFieldUtils.After)
      val returnType = collectionTypeOf(type)
      return Operation(
         name = "findBy${field.name.capitalize()}${ExpectedAnnotationName}",
         parameters = listOf(afterParameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getRestPath(type, field)))),
         returnType = returnType,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = returnType,
            returnTypeConstraints = listOf(greaterThanConstraint))
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return validate(field) != null && annotationFor(field, ExpectedAnnotationName) != null
   }

   companion object {
      const val ExpectedAnnotationName = "After"
      private fun getRestPath(type: Type, field: Field): String {
         val typeQualifiedName = type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/$ExpectedAnnotationName/{after}"
      }
   }
}
