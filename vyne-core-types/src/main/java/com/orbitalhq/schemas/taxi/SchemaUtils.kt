package com.orbitalhq.schemas.taxi

import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.Ids
import lang.taxi.CompilationException
import lang.taxi.errors

fun Schema.compileExpression(
   expression: String,
   returnType: Type,
   expressionTypeName: String = Ids.id("InlineExpression")
): Pair<TaxiSchema, Type> {
   val additionalSchema = "type $expressionTypeName inherits ${returnType.fullyQualifiedName} by $expression"
   val currentSchema = this.asTaxiSchema()
   val (errors, schemaWithType) =
      TaxiSchema.compiled(
         taxi = additionalSchema,
         importSources = listOf(currentSchema),
         functionRegistry = currentSchema.functionRegistry
      )
   if (errors.errors().isNotEmpty()) {
      throw CompilationException(errors.errors())
   }
   val expressionType = schemaWithType.type(expressionTypeName)
   return schemaWithType to expressionType
}

fun Schema.compileExpression(
   expression: String,
   returnTypeName: String,
   expressionTypeName: String = Ids.id("InlineExpression")
): Pair<TaxiSchema, Type> {
   val returnType = this.type(returnTypeName)
   return this.compileExpression(expression, returnType, expressionTypeName)
}
