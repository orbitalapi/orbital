package io.vyne.cask.ddl.views.taxiViews

import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.ModelAttributeReferenceSelector
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type

object FunctionStatementGenerator {
   fun windowFunctionToSql(accessor: FunctionAccessor, modelAttributeToColumNameMapper: (memberSource: QualifiedName, memberType: Type) -> String): String {
      if (accessor.function.toQualifiedName() != QualifiedName.from("vyne.aggregations.sumOver")) {
         throw IllegalArgumentException("only vyne.aggregations.sumOver is supported")
      }
      val isArgumentsAreModelAttributeReferenceSelector = accessor.inputs.all { input -> input is ModelAttributeReferenceSelector }
      if (!isArgumentsAreModelAttributeReferenceSelector) {
         throw IllegalArgumentException("arguments should be in SourceType::FieldType format!")
      }

      val columnNamesForArgs = columnNamesForFunctionArguments(accessor, modelAttributeToColumNameMapper)
      val orderByExpression =  if (columnNamesForArgs.size == 3)  " ORDER BY ${columnNamesForArgs[2]}" else ""
      val partitionBy = "PARTITION BY ${columnNamesForArgs[1]}"
      return "SUM (${columnNamesForArgs[0]}) OVER ($partitionBy  $orderByExpression)"
   }

   fun coalesceFunctionToSql(accessor: FunctionAccessor, modelAttributeToColumNameMapper: (memberSource: QualifiedName, memberType: Type) -> String): String {
      val columnNamesForArgs = columnNamesForFunctionArguments(accessor, modelAttributeToColumNameMapper)
      return "COALESCE(${columnNamesForArgs.joinToString(",")})"
   }

   private fun columnNamesForFunctionArguments(
      accessor: FunctionAccessor,
      modelAttributeToColumNameMapper: (memberSource: QualifiedName, memberType: Type) -> String): List<String> {
      return accessor.inputs.map { input ->
         val modelAttributeReferenceSelector = input as ModelAttributeReferenceSelector
         val memberSource = modelAttributeReferenceSelector.memberSource
         val memberType = modelAttributeReferenceSelector.targetType
         modelAttributeToColumNameMapper(memberSource, memberType)
      }
   }
}
