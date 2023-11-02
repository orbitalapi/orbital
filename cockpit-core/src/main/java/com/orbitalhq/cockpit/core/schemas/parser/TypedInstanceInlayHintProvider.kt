package com.orbitalhq.cockpit.core.schemas.parser

import com.fasterxml.jackson.core.JsonLocation
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.QualifiedName
import lang.taxi.sources.SourceLocation
import lang.taxi.types.AttributePath

/**
 * Describes the type at a specific location in a document.
 */
data class TypePosition(
   val start: SourceLocation,
   val startOffset: Long,
   val path: String,
   val type: QualifiedName
)

/**
 * Generates Inlay Hint information for a Monaco code editor to display.
 * Will show the type name after the field name of a TypedInstance, when rendered as JSON.
 *
 * Returns both the JSON (pretty printed), and the type hint information.
 * Note that formatting the JSON again may change it's layout, in which case code hints might be off.
 */
class TypedInstanceInlayHintProvider {

   /**
    * Returns a JSON string representing the TypedInstance,
    * along with hints of which type is present at each location in the JSON
    */
   fun generateHints(
      instance: TypedInstance,
      objectMapper: ObjectMapper = jacksonObjectMapper()
   ): Pair<String, List<TypePosition>> {
      require(instance is TypedObject || instance is TypedCollection) { "Must be a TypedObject or TypedCollection" }
      val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance.toRawObject())
      val lines = json.lines()

      val tree = objectMapper.factory.createParser(json)
      val positions = walkTree(tree) { location, path ->
         val value = when (instance) {
            is TypedObject -> instance.get(AttributePath(path))
            is TypedCollection -> instance.get(path.joinToString("."))
            else -> error("Must be TypedObject or TypedCollection")
         }
         val fieldLabel = path.last()
         // Offset from the start of the label + it's length, so it displays after
         // the field name.  Add 2 to compensate for open and close ""

         // JsonLocation line-numbers are 1-based.
         val line = lines[location.lineNr - 1]

         // The start index is hit-and-miss from the token stream
         // Normally, we're on a field declaration ,looking something like:
         // "aa" : {
         // So, position just before the :
         // If we're not sure, use the position information
         val charStart = if (line.count { it == ':' } == 1) {
            // We want to be just before the colon,
            // so should be an offset of -1.
            // However, char indexes are 1-based, so they cancel each other out
            line.indexOf(':')
         } else {
            location.columnNr + fieldLabel.length + 2
         }
         TypePosition(
            start = SourceLocation(
               location.lineNr, charStart
            ),
            startOffset = location.charOffset,
            path.joinToString("."),
            value.type.name,


            )
      }
      return json to positions
   }

   /**
    * This function traverses the stream of JSON tokens.
    * Originally, we tried to do this recursively, so that things like Objects could have tidy starts and finishes.
    * However, we lose position information in Jackson when we do that.
    * So, this design works by just reading the stream of tokens, building up our current path,
    * and parsing the types.
    */
   private fun <T> walkTree(
      parser: JsonParser,
      callback: (location: JsonLocation, path: List<String>) -> T
   ): List<T> {
      val results = mutableListOf<T>()
      val currentPath = mutableListOf<String>()
      fun replaceLastPathElement(path: String) {
         if (currentPath.isEmpty()) {
            currentPath.add(path)
         } else {
            currentPath.removeLast()
            currentPath.add(path)
         }
      }

      fun currentElementIsArrayIndex(): Boolean {
         return currentPath.isNotEmpty() && currentPath.last().startsWith("[")
      }

      while (parser.nextToken() != null) {
         when (parser.currentToken) {
            JsonToken.FIELD_NAME -> {
               val fieldName = parser.currentName
               if (currentElementIsArrayIndex()) {
                  currentPath.add(fieldName)
               } else {
                  replaceLastPathElement(fieldName)
               }

               results.add(callback(parser.currentTokenLocation(), currentPath))
            }

            JsonToken.START_OBJECT -> {
               if (currentElementIsArrayIndex()) {
                  val currentIndex = currentPath.last().removeSurrounding("[", "]").toInt()
                  replaceLastPathElement("[${currentIndex + 1}]")
               } else {
                  currentPath.add("?")
               }
            }

            JsonToken.END_OBJECT -> {
               currentPath.removeLast()
            }

            JsonToken.START_ARRAY -> currentPath.add("[-1]")
            JsonToken.END_ARRAY -> currentPath.removeLast()

            else -> {
               // Do nothing
            }
         }
      }
      return results
   }

}
