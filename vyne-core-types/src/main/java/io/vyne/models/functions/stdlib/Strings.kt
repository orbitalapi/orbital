package io.vyne.models.functions.stdlib

import io.vyne.models.Calculated
import io.vyne.models.ConversionService
import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedValue
import io.vyne.models.functions.FunctionInvoker
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.QualifiedName
import kotlin.math.min


object Strings {
   val functions: List<FunctionInvoker> = listOf(
      Left,
      Right,
      Mid,
      Concat,
      Uppercase,
      Lowercase,
      Trim,
      Length,
      Find
//      Coalesce
   )
}

/**
 * Helper class which will return TypedNull if any of the provided arguments were null.
 */
abstract class NullSafeInvoker : FunctionInvoker {
   protected abstract fun doInvoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      return if (inputValues.any { it is TypedNull }) {
         val indexOfFirstNull = inputValues.indexOfFirst { it is TypedNull } + 1
         log().warn("Function ${this.functionName} does not permit null arguments, but received null for argument $indexOfFirstNull.  Not invoking this function, and returning null")
         TypedNull.create(returnType)
      } else {
         doInvoke(inputValues, schema, returnType)
      }
   }
}

object Concat : FunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Concat.name
   override fun invoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      val result = inputValues.mapNotNull { it.value }.joinToString("")
      return TypedInstance.from(returnType, result, schema, source = Calculated)
   }
}

object Trim : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Trim.name

   override fun doInvoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      val input = inputValues[0].value.toString()
      val output = input.trim()
      return TypedInstance.from(returnType, output, schema, source = Calculated)
   }
}

private fun String.substringOrTypedNull(
   startIndex: Int,
   endIndex: Int,
   returnType: Type,
   functionName: QualifiedName,
   source: DataSource = Calculated
): TypedInstance {
   return try {
      val result = this.substring(startIndex, endIndex)
      TypedValue.from(returnType, result, source = Calculated, converter = ConversionService.DEFAULT_CONVERTER)
   } catch (boundsException: StringIndexOutOfBoundsException) {
      log().warn("Cannot invoke function $functionName as inputs are out of bounds: ${boundsException.message}")
      // TODO :  We could propigate the rror up through the source for lineage.
      TypedNull.create(returnType, source)
   }

}

object Left : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Left.name

   override fun doInvoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val count: Int = min(inputValues[1].valueAs(), input.length)

      return input.substringOrTypedNull(0, count, returnType, Mid.functionName)
   }
}

object Right : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Right.name

   override fun doInvoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val index: Int = inputValues[1].valueAs()

      return input.substringOrTypedNull(index, input.length, returnType, Mid.functionName)
   }
}

object Mid : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Mid.name

   override fun doInvoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val start: Int = inputValues[1].valueAs()
      val end: Int = inputValues[2].valueAs()
      return input.substringOrTypedNull(start, end, returnType, functionName)
   }
}

object Uppercase : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Uppercase.name

   override fun doInvoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val result = input.toUpperCase()
      return TypedInstance.from(returnType, result, schema, source = Calculated)
   }
}


object Lowercase : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Lowercase.name

   override fun doInvoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val result = input.toLowerCase()
      return TypedInstance.from(returnType, result, schema, source = Calculated)
   }
}

object Length : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Length.name

   override fun doInvoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      val input = inputValues[0].valueAs<String>()
      return TypedInstance.from(schema.type("Int"), input.length, schema, source = Calculated)
   }

}

object Find : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Find.name

   override fun doInvoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      val input = inputValues[0].valueAs<String?>()
      val searchString = inputValues[1].valueAs<String>()
      val intVal = input?.indexOf(searchString) ?: -1
      return TypedInstance.from(schema.type("Int"), intVal, schema, source = Calculated)
   }
}

// This is not included currently, as we seem to be dependent on previous
// behaviour where we would discover values to coalese through the query engine
object Coalesce : FunctionInvoker {
   override val functionName: QualifiedName = QualifiedName.from("vyne.stdlib.coalesce")
   override fun invoke(inputValues: List<TypedInstance>, schema: Schema, returnType: Type): TypedInstance {
      TODO("Not yet implemented")
   }
}

