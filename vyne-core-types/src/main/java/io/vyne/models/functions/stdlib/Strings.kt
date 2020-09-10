package io.vyne.models.functions.stdlib

import io.vyne.models.Calculated
import io.vyne.models.TypedInstance
import io.vyne.models.functions.FunctionInvoker
import io.vyne.models.functions.SelfDescribingFunction
import io.vyne.schemas.Schema
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName



object Strings {
   val functions: List<FunctionInvoker> = listOf(
      Left,
      Right,
      Mid,
      Concat,
      Uppercase,
      Lowercase
//      Coalesce
   )
}

object Concat : SelfDescribingFunction {
   override val taxiDeclaration: String = "declare function concat(String...):String"
   override val functionName: QualifiedName = QualifiedName.from("vyne.stdlib.concat")
   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val result = inputValues.joinToString("") { it.valueAs<String>() }
      return TypedInstance.from(schema.type(PrimitiveType.STRING), result, schema, source = Calculated)
   }

}

object Left : SelfDescribingFunction {
   override val taxiDeclaration: String = "declare function left(String,Int):String"
   override val functionName: QualifiedName = QualifiedName.from("vyne.stdlib.left")

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val count: Int = inputValues[1].valueAs()

      val result = input.substring(0, count)
      return TypedInstance.from(schema.type("String"), result, schema, source = Calculated)
   }
}

object Right : SelfDescribingFunction {
   override val taxiDeclaration: String = "declare function right(String,Int):String"
   override val functionName: QualifiedName = QualifiedName.from("vyne.stdlib.right")

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val index: Int = inputValues[1].valueAs()

      val result = input.substring(index, input.length)
      return TypedInstance.from(schema.type("String"), result, schema, source = Calculated)
   }
}

object Mid : SelfDescribingFunction {
   override val taxiDeclaration: String = "declare function mid(String,Int,Int):String"
   override val functionName: QualifiedName = QualifiedName.from("vyne.stdlib.mid")

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val start: Int = inputValues[1].valueAs()
      val end: Int = inputValues[2].valueAs()

      val result = input.substring(start, end)
      return TypedInstance.from(schema.type("String"), result, schema, source = Calculated)
   }
}
object Uppercase : SelfDescribingFunction {
   override val taxiDeclaration: String = "declare function upperCase(String):String"
   override val functionName: QualifiedName = QualifiedName.from("vyne.stdlib.upperCase")

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val result =  input.toUpperCase()
      return TypedInstance.from(schema.type("String"), result, schema, source = Calculated)
   }
}


object Lowercase : SelfDescribingFunction {
   override val taxiDeclaration: String = "declare function lowerCase(String):String"
   override val functionName: QualifiedName = QualifiedName.from("vyne.stdlib.lowerCase")

   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      val input: String = inputValues[0].valueAs<String>()
      val result =  input.toLowerCase()
      return TypedInstance.from(schema.type("String"), result, schema, source = Calculated)
   }
}

   // This is not included currently, as we seem to be dependent on previous
// behaviour where we would discover values to coalese through the query engine
object Coalesce : SelfDescribingFunction {
   override val taxiDeclaration: String = "declare function coalesce(String...):String"
   override val functionName: QualifiedName = QualifiedName.from("vyne.stdlib.coalesce")
   override fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance {
      TODO("Not yet implemented")
   }
}
