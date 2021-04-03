package io.vyne.queryService.history

import io.vyne.models.TypeNamedInstance
import io.vyne.schemas.fqn
import lang.taxi.utils.takeHead
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

object QueryHistoryResultNodeFinder {
   // eg: [0] or [230]
   private val numberInSquareBracketsRegex = "\\[\\d+\\]".toRegex()
   tailrec fun find(pathParts:List<String>, valueToParse:Any, originalPath:String): QueryResultNodeDetail {
      if (pathParts.isEmpty()) {
         if (valueToParse is TypeNamedInstance) {
            return QueryResultNodeDetail(
               originalPath.split(".").last(),
               originalPath,
               valueToParse.typeName.fqn(), valueToParse.source)
         } else {
            throw InvalidPathException("When parsing the final node, expected to find a TypeNamedInstance, but was ${valueToParse::class.simpleName}")
         }
      }

      val (thisPart,rest) = pathParts.takeHead()
      val thisValue = when {
         thisPart.matches(numberInSquareBracketsRegex) -> {
            val index = thisPart.removeSurrounding("[","]").toInt()
            if (valueToParse is List<*>) {
               valueToParse[index] ?: throw InvalidPathException("Array index $index is out of bounds on array with size ${valueToParse.size}")
            } else {
               throw InvalidPathException("Found an array index '$thisPart' but the value present was not an array.  This path looks invalid")
            }
         }
         else -> {
            val typeNamedInstanceValue = if (valueToParse is TypeNamedInstance) {
               when (valueToParse.value) {
                   null -> throw InvalidPathException("Found a null value at path part $thisPart")
                   is Map<*,*> -> {
                      val typeNamedInstanceValue = valueToParse.value as Map<String,Any>
                      typeNamedInstanceValue[thisPart] ?: throw InvalidPathException("Found a null value at path part $thisPart")
                   }
                  else -> throw InvalidPathException("Expected to find a map when reading property $thisPart, found a ${valueToParse.value!!::class.simpleName}")
               }
            } else {
               throw InvalidPathException("Expected to find a TypeNamedInstance at path $thisPart, but was a ${valueToParse::class.simpleName}")
            }
            typeNamedInstanceValue
         }
      }
      return find(rest,thisValue, originalPath)
   }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidPathException(message:String):RuntimeException(message)
