package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.services.CaskServiceSchemaGenerator
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import org.springframework.stereotype.Component

/**
 * For:
 *
 * FooId inherits String
 * model Foo {
 *   @Id
 *   fooId: FooId
 * }
 *
 * this class generates the following operation definition for the corresponding cask service:
 *
 *  @HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/Foo/fooId/{id}")
*   operation findSingleByFooId(@PathVariable(name = "id") id : FooId) : Foo(FooId = id)
 */
@Component
class FindByIdGenerators(val operationGeneratorConfig: OperationGeneratorConfig = OperationGeneratorConfig.empty()): OperationGenerator {
   override fun generate(field: Field?, type: Type): Operation {
      val parameterType = TemporalFieldUtils.parameterType(field!!)
      val equalsParameter = TemporalFieldUtils.parameterFor(
         parameterType,
         PathVariableName,
         listOf(Annotation("PathVariable", mapOf("name" to PathVariableName))))
      val equalsConstraint = TemporalFieldUtils.constraintFor(field, Operator.EQUAL, PathVariableName)
      return Operation(
         name = "findSingleBy${field.name.capitalize()}",
         parameters = listOf(equalsParameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getRestPath(type, field)))),
         returnType = type,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = type,
            returnTypeConstraints = listOf(equalsConstraint))
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return PrimitiveType.isAssignableToPrimitiveType(field.type) &&
         (TemporalFieldUtils.annotationFor(field, expectedAnnotationName.annotation) != null ||
            operationGeneratorConfig.definesOperation(field.type, expectedAnnotationName))
   }

   override fun expectedAnnotationName(): OperationAnnotation {
      return expectedAnnotationName
   }

   companion object {
      private val expectedAnnotationName = OperationAnnotation.Id
      const val PathVariableName = "id"

      private fun getRestPath(type: Type, field: Field): String {
         val typeQualifiedName = type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "${CaskServiceSchemaGenerator.CaskApiRootPath}findSingleBy/${path.parts.joinToString("/")}/${field.name}/{$PathVariableName}"
      }
   }
}
