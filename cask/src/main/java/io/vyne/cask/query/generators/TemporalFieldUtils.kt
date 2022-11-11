package io.vyne.cask.query.generators

import lang.taxi.Operator
import lang.taxi.services.Parameter
import lang.taxi.services.operations.constraints.PropertyToParameterConstraint
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.services.operations.constraints.RelativeValueExpression
import lang.taxi.types.*
import lang.taxi.types.Annotation

object TemporalFieldUtils {
   fun validate(field: Field): PrimitiveType? {
      if (!PrimitiveType.isAssignableToPrimitiveType(field.type)) {
         return null
      }

      val primitive = PrimitiveType.getUnderlyingPrimitive(field.type)
      if (primitive != PrimitiveType.INSTANT && primitive != PrimitiveType.LOCAL_DATE) {
         return null
      }

      return primitive
   }

   fun annotationFor(field: Field, name: String): Annotation? {
      return field
         .annotations
         .find { annotation -> annotation.name == name }
   }

   fun constraintFor(field: Field, op: Operator, path: String): PropertyToParameterConstraint{
      val inheritedType = parameterType(field)
      return constraintFor(inheritedType, op, path)
   }

   fun constraintFor(inheritedType: Type, op: Operator, path: String): PropertyToParameterConstraint{
      return PropertyToParameterConstraint(
         propertyIdentifier = PropertyTypeIdentifier(inheritedType.toQualifiedName()),
         operator = op,
         expectedValue = RelativeValueExpression(AttributePath.from(path)),
         compilationUnits = listOf(CompilationUnit.unspecified())
      )
   }

   fun parameterFor(type: Type, name: String, annotations: List<Annotation> = listOf()) = Parameter(
      annotations = annotations,
      type = type,
      name = name,
      constraints = listOf())

   fun parameterType(field: Field) = field.type

   fun collectionTypeOf(type: Type) = ArrayType(type = type, source = CompilationUnit.unspecified())

   fun streamTypeOf(type: Type) = StreamType(type = type, source = CompilationUnit.unspecified())

   const val Start = "start"
   const val End = "end"
   const val After = "after"
   const val Before = "before"
}
