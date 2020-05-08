package io.vyne.schemas

import lang.taxi.types.PrimitiveType
import java.io.IOException
import java.io.Serializable
import java.io.StreamTokenizer
import java.io.StringReader

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
   tokenizer.wordChars('_'.toInt(), '_'.toInt())
   try {
      return parse(tokenizer, listOf(StreamTokenizer.TT_EOF)) // Parse until the end
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

private fun parse(tokenizer: StreamTokenizer, terminalCharacterCodes: List<Int>): GenericTypeName {
   val BOF_MARKER = -4
   val baseNameParts = mutableListOf<String>()
   val params = mutableListOf<GenericTypeName>()
   while (!terminalCharacterCodes.contains(tokenizer.ttype)) {
      when (tokenizer.ttype) {
         BOF_MARKER -> {
            tokenizer.nextToken() // Skip it
         }
         '<'.toInt() -> {
            do {
               tokenizer.nextToken()  // Skip '<' or ','
               params.add(parse(tokenizer, terminalCharacterCodes = listOf('>'.toInt(), ','.toInt())))
            } while (tokenizer.ttype == ','.toInt())
            tokenizer.nextToken() // Skip past the closing >
         }
         else -> {
            baseNameParts.add(tokenizer.sval)
            tokenizer.nextToken()
         }
      }
   }
   return GenericTypeName(baseNameParts.joinToString(""), params)
}

