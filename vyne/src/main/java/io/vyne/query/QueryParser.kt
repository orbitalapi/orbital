package io.vyne.query

import io.vyne.schemas.Schema

class QueryParser(val schema: Schema) {
   @Deprecated(message = "Use a TypeNameQueryExpression instead", replaceWith = ReplaceWith("parse(QueryExpression)"))
   fun parse(query: String): Set<QuerySpecTypeNode> {
      return parse(TypeNameQueryExpression(query))
   }

   fun parse(query: QueryExpression): Set<QuerySpecTypeNode> {
      return when (query) {
         is TypeNameQueryExpression -> parseSingleType(query)
         is TypeNameListQueryExpression -> parseQueryList(query)
         is GraphQlQueryExpression -> parseQueryObject(query)
         is ConstrainedTypeNameQueryExpression -> parseQueryObject(query)
         is ProjectedExpression -> {
            parse(query.source).map { it.copy(projection = query.projection) }.toSet()
         }
         else -> throw IllegalArgumentException("The query passed was neither a Json object, nor a recognized type.  Unable to proceed:  $query")
      }
   }

   private fun parseQueryObject(query: ConstrainedTypeNameQueryExpression): Set<QuerySpecTypeNode> {
      return setOf(QuerySpecTypeNode(schema.type(query.typeName), dataConstraints = query.constraint))
   }

   private fun parseSingleType(expression: TypeNameQueryExpression): Set<QuerySpecTypeNode> {
      return setOf(getQueryNode(expression.typeName))
   }

   private fun getQueryNode(typeName: String): QuerySpecTypeNode {
      return QuerySpecTypeNode(schema.type(typeName))
   }

   private fun parseQueryList(expression: TypeNameListQueryExpression): Set<QuerySpecTypeNode> {
      return expression.typeNames.map { getQueryNode(it) }.toSet()
   }

   private fun parseQueryObject(query: GraphQlQueryExpression): Set<QuerySpecTypeNode> {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }
}
