package io.vyne.cask.ddl.views

import io.vyne.utils.log
import lang.taxi.accessors.ConditionalAccessor
import lang.taxi.expressions.FieldReferenceExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.sources.SourceLocation
import lang.taxi.toCompilationUnit
import lang.taxi.types.CalculatedFieldSetExpression
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.FieldReferenceSelector
import lang.taxi.types.ObjectType
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type

/**
 * Handles resolution of duplicate fields present
 * across the types we're using to construct a view
 */
class CaskViewFieldFilter(private val viewName: QualifiedName, private val types: List<ObjectType>, private val typeToPrefer: ObjectType) {
   private val fieldsToExclude: Map<ObjectType, List<Field>>

   init {
      require(types.contains(typeToPrefer)) { "${typeToPrefer.qualifiedName} is not one of the candidate types" }
      // find fields with duplicate return types
      // This type is a mess .. key is the return type from a field.
      // Value is a collection of the types that defined the field, and the field itself
      val fieldReturnTypesToAppearances: Map<Type, List<Pair<ObjectType, Field>>> = types.flatMap { type ->
         type.fields.map { field ->
            field.type to (type to field)
         }
      }.groupBy { it.first }
         .mapValues { (_, value) -> value.map { it.second } }

      val duplicates = fieldReturnTypesToAppearances.filter { it.value.size > 1 }
      this.fieldsToExclude = duplicates.flatMap { (duplicateTypeName, locations) ->
         val locationDescription = locations.joinToString(",") { (declaringType, field) -> "${declaringType.qualifiedName}:${field.name}" }
         log().warn("In view definition of $viewName, ${duplicateTypeName.qualifiedName} is present multiple times: $locationDescription. Will prefer declaration on ${typeToPrefer.qualifiedName} ")
         locations.filter { (declaringType, _) -> declaringType != typeToPrefer }
      }.groupBy(keySelector = { it.first }, valueTransform = { it.second })
   }

   fun getOriginalFieldsForType(type: ObjectType): List<Field> {
      val exclusions = fieldsToExclude.getOrDefault(type, emptyList())
      return type.fields.filter { !exclusions.contains(it) }
         // Exclude fields with formulas, as these aren't persisted
          .filterNot { (it.accessor as? OperatorExpression).let { operatorExpression -> operatorExpression?.lhs is TypeExpression || operatorExpression?.lhs is TypeExpression } }

   }

   fun getRenamedFieldsForType(type: ObjectType): List<Field> {
      return getOriginalFieldsForType(type).map { field ->
         val accessor = field.accessor?.let { accessor ->
            when (accessor) {
               is ConditionalAccessor -> renameConditionalAccessor(type, field, accessor)
               is OperatorExpression -> renameOperatorExpression(type, field, accessor)
               else -> accessor
            }


         }
         field.copy(
            name = renameField(type, field.name),
            accessor = accessor
         )
      }
   }

   private fun renameField(owningType: ObjectType, fieldName: String) =
      owningType.toQualifiedName().typeName.uncapitalize() + "_" + fieldName.capitalize()

   private fun renameOperatorExpression(owningType: ObjectType, field: Field, accessor: OperatorExpression): OperatorExpression {
      val lhs = (accessor.lhs as? FieldReferenceExpression)?.let { renameFieldReferenceExpression(owningType, field, it) } ?: accessor.lhs
      val rhs = (accessor.rhs as? FieldReferenceExpression)?.let { renameFieldReferenceExpression(owningType, field, it) } ?: accessor.rhs
      val compilationUnit = accessor.compilationUnits.first()
      return OperatorExpression(lhs, accessor.operator, rhs, listOf(
         compilationUnit.copy(source = compilationUnit.source.copy(content = "${lhs.compilationUnits.first().source.content} ${accessor.operator.symbol} ${rhs.compilationUnits.first().source.content}"))
      ))
   }

   private fun renameFieldReferenceExpression(owningType: ObjectType, field: Field, expression: FieldReferenceExpression): FieldReferenceExpression {
      val renamedField = renameField(owningType, expression.fieldName)
      val selector =  FieldReferenceSelector(renamedField, expression.returnType)
      val existingCompilationUnit = expression.compilationUnits.first()
      val compilationUnit = CompilationUnit(
         expression,
         existingCompilationUnit.source.copy(content = "this.$renamedField"),
         existingCompilationUnit.location
      )
      return FieldReferenceExpression(selector, listOf(compilationUnit))
   }

   private fun renameConditionalAccessor(owningType: ObjectType, field: Field, accessor: ConditionalAccessor): ConditionalAccessor {
      return when (accessor.expression) {
         is CalculatedFieldSetExpression -> {
            val expression = accessor.expression as CalculatedFieldSetExpression
            accessor.copy(expression =
            expression.copy(
               operand1 = FieldReferenceSelector(renameField(owningType, expression.operand1.fieldName), accessor.returnType),
               operand2 = FieldReferenceSelector(renameField(owningType, expression.operand2.fieldName), accessor.returnType)
            )
            )
         }
//         is UnaryCalculatedFieldSetExpression -> {
//            val expression = accessor.expression as UnaryCalculatedFieldSetExpression
//            accessor.copy(expression = expression.copy(operand = FieldReferenceSelector(renameField(owningType, expression.operand.fieldName))))
//         }
//         is TerenaryFieldSetExpression -> {
//            val expression = accessor.expression as TerenaryFieldSetExpression
//            accessor.copy(expression = expression.copy(operand1 = FieldReferenceSelector(renameField(owningType, expression.operand1.fieldName)),
//            operand2 = FieldReferenceSelector(renameField(owningType, expression.operand2.fieldName)),
//               operand3 = FieldReferenceSelector(renameField(owningType, expression.operand3.fieldName))))
//
//         }
         // TDOO... what are the other types?  Do we need to change 'em?
         else -> accessor
      }
   }

   fun getAllFieldsOriginal(): List<Field> {
      return this.types.flatMap { getOriginalFieldsForType(it) }
   }

   fun getAllFieldsRenamed(): List<Field> {
      return this.types.flatMap { getRenamedFieldsForType(it) }
   }


}

private fun String.uncapitalize(): String {
   return if (isNotEmpty() && this[0].isUpperCase()) substring(0, 1).toLowerCase() + substring(1) else this
}
