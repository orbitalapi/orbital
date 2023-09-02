package com.orbitalhq.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * Used for deserialization of fields annotated with @JsonRawValue
 */
class KeepAsJsonDeserializer : JsonDeserializer<String>() {
   override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): String {
      val tree = jsonParser.codec.readTree<TreeNode>(jsonParser)
      return tree.toString()
   }

}
