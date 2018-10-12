package io.osmosis.polymer.query

import io.osmosis.polymer.schemas.Schema

class QueryParser(val schema: Schema) {
   fun parse(query: String): Set<QuerySpecTypeNode> {
      if (query.trim().startsWith("{")) {
         return parseQueryObject(query)
      } else if (schema.hasType(query)) {
         return parseSingleType(query)
      } else {
         throw IllegalArgumentException("The query passed was neither a Json object, nor a recognized type.  Unable to proceed:  $query")
      }
   }

   private fun parseSingleType(typeName: String): Set<QuerySpecTypeNode> {
      return setOf(QuerySpecTypeNode(schema.type(typeName)))
   }

   private fun parseQueryObject(query: String): Set<QuerySpecTypeNode> {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }
}
