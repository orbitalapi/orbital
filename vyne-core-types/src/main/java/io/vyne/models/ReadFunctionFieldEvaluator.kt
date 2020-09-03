package io.vyne.models

import io.vyne.models.csv.CsvAttributeAccessorParser
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.CalculatedFieldSetExpression
import lang.taxi.types.ColumnAccessor
import lang.taxi.types.FieldSetExpression
import lang.taxi.types.ReadFunction
import lang.taxi.types.ReadFunctionFieldAccessor
import lang.taxi.types.TerenaryFieldSetExpression
import lang.taxi.types.UnaryCalculatedFieldSetExpression
import lang.taxi.types.WhenFieldSetCondition
import org.apache.commons.lang3.StringUtils
import java.lang.StringBuilder

class ReadFunctionFieldEvaluator(private val factory: TypedObjectFactory) {

   fun evaluate(value: Any,
                targetType: Type,
                schema: Schema,
                accessor: ReadFunctionFieldAccessor,
                nullValues: Set<String>,
                source: DataSource,
                nullable: Boolean,
                parseColumnData: (Any, Type, Schema, ColumnAccessor, Set<String>, DataSource, Boolean) -> TypedInstance): TypedInstance {

      return when(accessor.readFunction) {
         ReadFunction.CONCAT -> {
            val arguments = accessor.arguments.mapNotNull { readFunctionArgument ->
               if (readFunctionArgument.columnAccessor != null) {
                  parseColumnData(value, targetType, schema, readFunctionArgument.columnAccessor!!, nullValues, source, nullable).value
               } else {
                  readFunctionArgument.value
               }
            }



            val builder = StringBuilder()
            arguments.forEach { builder.append(it.toString()) }
            TypedInstance.from(targetType, builder.toString(), schema, source = source)
         }

         ReadFunction.LEFTUPPERCASE -> {
            // leftAndUpperCase(column("CCY"), 3)
            val columnAccessor = requireNotNull(accessor.arguments.first().columnAccessor)
            val len = accessor.arguments[1].value as Int
            val columnValue = parseColumnData(value, targetType, schema, columnAccessor, nullValues, source, nullable).value
            if (columnValue == null) {
               TypedInstance.from(targetType, null, schema, source = source)
            } else {
               val leftUpperCaseArg = columnValue.toString()
               TypedInstance.from(targetType, StringUtils.left(leftUpperCaseArg, len), schema, source = source)
            }
         }

         ReadFunction.MIDUPPERCASE -> {

         }

         else -> error("Only concat is allowed")
      }
   }
}
