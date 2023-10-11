package com.orbitalhq.query

import com.orbitalhq.schemas.Schema

class QueryParser(val schema: Schema) {
   @Deprecated(message = "Use a TypeNameQueryExpression instead", replaceWith = ReplaceWith("parse(QueryExpression)"))
   fun parse(query: String): Set<QuerySpecTypeNode> {
      return parse(TypeNameQueryExpression(query))
   }

   fun parse(query: QueryExpression): Set<QuerySpecTypeNode> {
      // This is tech debt from when we used to support
      // queries that were JSON objects
      // In reality, this can be simplified as we now only support queries via TaxiQL.

      return when (query) {
         is TypeQueryExpression -> setOf(QuerySpecTypeNode(query.type))
         is TypeNameQueryExpression -> parseSingleType(query)
         is TypeNameListQueryExpression -> parseQueryList(query)
         // Don't get excited... we never implemented GraphQL...
         is GraphQlQueryExpression -> parseQueryObject(query)
         is ConstrainedTypeNameQueryExpression -> parseQueryObject(query)
         is MutationOnlyExpression -> parseMutation(query)
         // TODO :  How do we support the mutation phase?
         is QueryAndMutateExpression -> parse(query.query).map { it.copy(mutation = query.mutation) }.toSet()
         is ProjectedExpression -> {
            parse(query.source).map { it.copy(projection = query.projection) }.toSet()
         }
         else -> throw IllegalArgumentException("The query passed was neither a Json object, nor a recognized type.  Unable to proceed:  $query")
      }
   }

   private fun parseMutation(query: MutationOnlyExpression): Set<QuerySpecTypeNode> {
      return setOf(QuerySpecTypeNode(schema.type(query.mutation.operation.returnType), mutation = query.mutation))
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
