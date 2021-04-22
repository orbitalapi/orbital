package io.vyne.cask.ddl.views.taxiViews

import io.vyne.cask.ddl.PostgresDdlGenerator
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.ModelAttributeReferenceSelector
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type

object WindowFunctionStatementGenerator {
   fun windowFunctionToSql(accessor: FunctionAccessor, modelAttributeToColumNameMapper: (memberSource: QualifiedName, memberType: Type) -> String): String {
      if (accessor.function.toQualifiedName() != QualifiedName.from("vyne.aggregations.sumOver")) {
         throw IllegalArgumentException("only vyne.aggregations.sumOver is supported")
      }
      val isArgumentsAreModelAttributeReferenceSelector = accessor.inputs.all { input -> input is ModelAttributeReferenceSelector }
      if (!isArgumentsAreModelAttributeReferenceSelector) {
         throw IllegalArgumentException("arguments should be in SourceType::FieldType format!")
      }

      val columnNamesForArgs = accessor.inputs.map { input ->
         val modelAttributeReferenceSelector = input as ModelAttributeReferenceSelector
         val memberSource = modelAttributeReferenceSelector.memberSource
         val memberType = modelAttributeReferenceSelector.memberType
         modelAttributeToColumNameMapper(memberSource, memberType)
      }
      val orderByExpression =  if (columnNamesForArgs.size == 3)  " ORDER BY ${columnNamesForArgs[2]}" else ""
      val partitionBy = "PARTITION BY ${columnNamesForArgs[1]}"
      return "SUM (${columnNamesForArgs[0]}) OVER ($partitionBy  $orderByExpression)"
   }
}
