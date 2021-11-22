package io.vyne.history

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import io.vyne.utils.ExceptionProvider
import io.vyne.history.rest.QueryResultNodeDetail
import io.vyne.models.TypeNamedInstance
import io.vyne.schemas.fqn
import lang.taxi.utils.takeHead

object QueryHistoryResultNodeFinder {
   // eg: [0] or [230]
   private val numberInSquareBracketsRegex = "\\[\\d+\\]".toRegex()
   tailrec fun find(
      pathParts: List<String>,
      valueToParse: Any,
      originalPath: String,
      exceptionProvider: ExceptionProvider): QueryResultNodeDetail {
      if (pathParts.isEmpty()) {
         if (valueToParse is TypeNamedInstance) {
            return QueryResultNodeDetail(
               originalPath.split(".").last(),
               originalPath,
               valueToParse.typeName.fqn(), valueToParse.dataSourceId, null
            )
         } else {
            throw exceptionProvider.invalidPathException("When parsing the final node, expected to find a TypeNamedInstance, but was ${valueToParse::class.simpleName}")
         }
      }

      val (thisPart, rest) = pathParts.takeHead()
      val thisValue = when {
         thisPart.matches(numberInSquareBracketsRegex) -> {
            val index = thisPart.removeSurrounding("[", "]").toInt()
            if (valueToParse is List<*>) {
               valueToParse[index]
                  ?: throw exceptionProvider.invalidPathException("Array index $index is out of bounds on array with size ${valueToParse.size}")
            } else {
               throw exceptionProvider.invalidPathException("Found an array index '$thisPart' but the value present was not an array.  This path looks invalid")
            }
         }
         else -> {
            val typeNamedInstanceValue = if (valueToParse is TypeNamedInstance) {
               when (valueToParse.value) {
                  null -> throw exceptionProvider.invalidPathException("Found a null value at path part $thisPart")
                  is Map<*, *> -> {
                     val typeNamedInstanceValue = valueToParse.value as Map<String, Any>
                     typeNamedInstanceValue[thisPart]
                        ?: throw exceptionProvider.invalidPathException("Found a null value at path part $thisPart")
                  }
                  else -> throw exceptionProvider.invalidPathException("Expected to find a map when reading property $thisPart, found a ${valueToParse.value!!::class.simpleName}")
               }
            } else {
               throw exceptionProvider.invalidPathException("Expected to find a TypeNamedInstance at path $thisPart, but was a ${valueToParse::class.simpleName}")
            }
            typeNamedInstanceValue
         }
      }
      return find(rest, thisValue, originalPath, exceptionProvider)
   }
}

/**
 * Used for deserialization of fields annotated with @JsonRawValue
 */
class KeepAsJsonDeserializer: JsonDeserializer<String>() {
   override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): String {
      val tree = jsonParser.codec.readTree<TreeNode>(jsonParser)
      return tree.toString()
   }

}


