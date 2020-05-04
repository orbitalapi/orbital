package io.vyne.schemas

import java.io.IOException
import java.io.Serializable
import java.io.StreamTokenizer
import java.io.StringReader
import lang.taxi.types.PrimitiveType

data class QualifiedName(val fullyQualifiedName: String, val parameters: List<QualifiedName> = emptyList()) : Serializable {
   val name: String
      get() = fullyQualifiedName.split(".").last()

   val parameterizedName: String
      get() {
         return if (parameters.isEmpty()) {
            fullyQualifiedName
         } else {
            val params = this.parameters.joinToString(",") { it.parameterizedName }
            "$fullyQualifiedName<$params>"
         }
      }

   fun rawTypeEquals(other: QualifiedName): Boolean {
      return this.fullyQualifiedName == other.fullyQualifiedName
   }

   val namespace: String
      get() {
         return fullyQualifiedName.split(".").dropLast(1).joinToString(".")
      }

   override fun toString(): String = fullyQualifiedName
}




fun String.fqn(): QualifiedName {

   return when {
      OperationNames.isName(this) -> QualifiedName(this, emptyList())
      ParamNames.isParamName(this) -> QualifiedName("param/" + ParamNames.typeNameInParamName(this).fqn().parameterizedName)
      else -> parse(this).toQualifiedName()
   }

}

private data class GenericTypeName(val baseType: String, val params: List<GenericTypeName>) {
   fun toQualifiedName(): QualifiedName {
      return QualifiedName(this.baseType, this.params.map { it.toQualifiedName() })
   }
}

private fun parse(s: String): GenericTypeName {
   val expandedName = convertArrayShorthand(s)
   val tokenizer = StreamTokenizer(StringReader(expandedName))
//   tokenizer.w .wordChars(".",".")
   try {
      tokenizer.nextToken()  // Skip "BOF" token
      return parse(tokenizer)
   } catch (e: IOException) {
      throw RuntimeException()
   }

}


// Converts Foo[] to lang.taxi.Array<Foo>
private fun convertArrayShorthand(name: String): String {
   if (name.endsWith("[]")) {
      val arrayType = name.removeSuffix("[]")
      return PrimitiveType.ARRAY.qualifiedName + "<$arrayType>"
   } else {
      return name
   }
}

private fun parse(tokenizer: StreamTokenizer): GenericTypeName {
   val baseName = tokenizer.sval
   tokenizer.nextToken()
   val params = mutableListOf<GenericTypeName>()
   if (tokenizer.ttype == '<'.toInt()) {
      do {
         tokenizer.nextToken()  // Skip '<' or ','
         params.add(parse(tokenizer))
      } while (tokenizer.ttype == ','.toInt())
      tokenizer.nextToken()  // skip '>'
   }
   return GenericTypeName(baseName, params)
}

