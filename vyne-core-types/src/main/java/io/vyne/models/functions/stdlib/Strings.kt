package io.vyne.models.functions.stdlib

import io.vyne.models.Calculated
import io.vyne.models.TypedInstance
import io.vyne.models.functions.FunctionInvoker
import io.vyne.schemas.Schema
import lang.taxi.functions.stdlib.StdLib
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName



object Strings {
   val functions: List<FunctionInvoker> = listOf(
      Left,
      Right,
      Mid,
      Concat,
      Uppercase,
      Lowercase,
      Trim
//      Coalesce
   )
}

object Concat : FunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Concat.name
   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val result = inputValues.mapNotNull { it.value }.joinToString("")
      return TypedInstance.from(schema.type(PrimitiveType.STRING), result, schema, source = Calculated)
   }
}

object Trim : FunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Trim.name

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input = inputValues[0].value.toString()
      val output = input.trim()
      return TypedInstance.from(schema.type(PrimitiveType.STRING), output, schema, source = Calculated)
   }
}
object Left : FunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Left.name

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val count: Int = inputValues[1].valueAs()

      val result = input.substring(0, count)
      return TypedInstance.from(schema.type(PrimitiveType.STRING), result, schema, source = Calculated)
   }
}

object Right : FunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Right.name

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val index: Int = inputValues[1].valueAs()

      val result = input.substring(index, input.length)
      return TypedInstance.from(schema.type("String"), result, schema, source = Calculated)
   }
}

object Mid : FunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Mid.name

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val start: Int = inputValues[1].valueAs()
      val end: Int = inputValues[2].valueAs()

      val result = input.substring(start, end)
      return TypedInstance.from(schema.type("String"), result, schema, source = Calculated)
   }
}
object Uppercase : FunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Uppercase.name

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val result =  input.toUpperCase()
      return TypedInstance.from(schema.type("String"), result, schema, source = Calculated)
   }
}


object Lowercase : FunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Lowercase.name

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val result =  input.toLowerCase()
      return TypedInstance.from(schema.type("String"), result, schema, source = Calculated)
   }
}

   // This is not included currently, as we seem to be dependent on previous
// behaviour where we would discover values to coalese through the query engine
object Coalesce : FunctionInvoker {
   override val functionName: QualifiedName = QualifiedName.from("vyne.stdlib.coalesce")
   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      TODO("Not yet implemented")
   }
}
