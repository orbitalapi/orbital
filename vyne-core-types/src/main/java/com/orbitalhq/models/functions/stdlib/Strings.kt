package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.DataSource
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.log
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import kotlin.math.min


object Strings {
   val functions: List<NamedFunctionInvoker> = listOf(
      Left,
      Right,
      Mid,
      Concat,
      Uppercase,
      Lowercase,
      Trim,
      Length,
      Find,
      ContainsString,
      Replace,
      Coalesce
   )
}

object Concat : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Concat.name
   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      rawMessageBeingParsed: Any?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val result = inputValues.mapNotNull { it.value }.joinToString("")
      return TypedInstance.from(
         returnType, result, schema, source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }
}

object Trim : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Trim.name

   override fun doInvoke(
       inputValues: List<TypedInstance>,
       schema: Schema,
       returnType: Type,
       function: FunctionAccessor,
       rawMessageBeingParsed: Any?,
       thisScopeValueSupplier: EvaluationValueSupplier,
       returnTypeFormat: FormatsAndZoneOffset?,
       resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input = inputValues[0].value.toString()
      val output = input.trim()
      return TypedInstance.from(
         returnType, output, schema,
         source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }
}

private fun String.substringOrTypedNull(
   startIndex: Int,
   endIndex: Int,
   returnType: Type,
   functionName: QualifiedName,
   source: DataSource,
   schema: Schema,
   format: FormatsAndZoneOffset?
): TypedInstance {
   return try {
      val result = this.substring(startIndex, endIndex)
      TypedInstance.from(type = returnType, value = result, schema = schema, source = source, format = format)
   } catch (boundsException: StringIndexOutOfBoundsException) {
      log().warn("Cannot invoke function $functionName as inputs are out of bounds: ${boundsException.message}")
      // TODO :  We could propigate the rror up through the source for lineage.
      TypedNull.create(returnType, source)
   }

}

object Left : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Left.name

   override fun doInvoke(
       inputValues: List<TypedInstance>,
       schema: Schema,
       returnType: Type,
       function: FunctionAccessor,
       rawMessageBeingParsed: Any?,
       thisScopeValueSupplier: EvaluationValueSupplier,
       returnTypeFormat: FormatsAndZoneOffset?,
       resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val count: Int = min(inputValues[1].valueAs(), input.length)

      return input.substringOrTypedNull(
         0, count, returnType, Mid.functionName, EvaluatedExpression(
            function.asTaxi(),
            inputValues,
         ),
         schema,
         format = returnTypeFormat
      )
   }
}

object Right : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Right.name

   override fun doInvoke(
       inputValues: List<TypedInstance>,
       schema: Schema,
       returnType: Type,
       function: FunctionAccessor,
       rawMessageBeingParsed: Any?,
       thisScopeValueSupplier: EvaluationValueSupplier,
       returnTypeFormat: FormatsAndZoneOffset?,
       resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val index: Int = inputValues[1].valueAs()

      return input.substringOrTypedNull(
         index, input.length, returnType, Mid.functionName, EvaluatedExpression(
            function.asTaxi(),
            inputValues
         ),
         schema,
         returnTypeFormat
      )
   }
}

object Mid : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Mid.name

   override fun doInvoke(
       inputValues: List<TypedInstance>,
       schema: Schema,
       returnType: Type,
       function: FunctionAccessor,
       rawMessageBeingParsed: Any?,
       thisScopeValueSupplier: EvaluationValueSupplier,
       returnTypeFormat: FormatsAndZoneOffset?,
       resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val start: Int = inputValues[1].valueAs()
      val end: Int = inputValues[2].valueAs()
      return input.substringOrTypedNull(
         start, end, returnType, functionName, EvaluatedExpression(
            function.asTaxi(),
            inputValues
         ),
         schema,
         returnTypeFormat
      )
   }
}

object Uppercase : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Uppercase.name

   override fun doInvoke(
       inputValues: List<TypedInstance>,
       schema: Schema,
       returnType: Type,
       function: FunctionAccessor,
       rawMessageBeingParsed: Any?,
       thisScopeValueSupplier: EvaluationValueSupplier,
       returnTypeFormat: FormatsAndZoneOffset?,
       resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val result = input.toUpperCase()
      return TypedInstance.from(
         returnType, result, schema, source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }
}


object Lowercase : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Lowercase.name

   override fun doInvoke(
       inputValues: List<TypedInstance>,
       schema: Schema,
       returnType: Type,
       function: FunctionAccessor,
       rawMessageBeingParsed: Any?,
       thisScopeValueSupplier: EvaluationValueSupplier,
       returnTypeFormat: FormatsAndZoneOffset?,
       resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val result = input.toLowerCase()
      return TypedInstance.from(
         returnType, result, schema, source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }
}

object Length : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Length.name

   override fun doInvoke(
       inputValues: List<TypedInstance>,
       schema: Schema,
       returnType: Type,
       function: FunctionAccessor,
       rawMessageBeingParsed: Any?,
       thisScopeValueSupplier: EvaluationValueSupplier,
       returnTypeFormat: FormatsAndZoneOffset?,
       resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input = inputValues[0].valueAs<String>()
      return TypedInstance.from(
         schema.type("Int"), input.length, schema, source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }

}

object Find : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Find.name

   override fun doInvoke(
       inputValues: List<TypedInstance>,
       schema: Schema,
       returnType: Type,
       function: FunctionAccessor,
       rawMessageBeingParsed: Any?,
       thisScopeValueSupplier: EvaluationValueSupplier,
       returnTypeFormat: FormatsAndZoneOffset?,
       resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input = inputValues[0].valueAs<String?>()
      val searchString = inputValues[1].valueAs<String>()
      val intVal = input?.indexOf(searchString) ?: -1
      return TypedInstance.from(
         schema.type("Int"), intVal, schema, source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }
}

object ContainsString : NullSafeInvoker() {
   override fun doInvoke(
       inputValues: List<TypedInstance>,
       schema: Schema,
       returnType: Type,
       function: FunctionAccessor,
       rawMessageBeingParsed: Any?,
       thisScopeValueSupplier: EvaluationValueSupplier,
       returnTypeFormat: FormatsAndZoneOffset?,
       resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input = inputValues[0].valueAs<String>()
      val searchString = inputValues[1].valueAs<String>()
      val contains = input.contains(searchString)
      return TypedInstance.from(
         schema.type(PrimitiveType.BOOLEAN.qualifiedName), contains, schema, source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.ContainsString.name
}

object Coalesce : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Coalesce.name
   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      rawMessageBeingParsed: Any?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val firstNotNull = inputValues.firstOrNull { it.value != null }
      return firstNotNull ?: TypedNull.create(returnType)
   }
}

object Replace : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Replace.name
   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      rawMessageBeingParsed: Any?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {

      val input: String = inputValues[0].valueAs()
      val replace: String = inputValues[1].valueAs()
      val with: String = inputValues[2].valueAs()

      val result = input.replace(replace, with)

      return TypedInstance.from(
         returnType, result, schema, source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }
}

