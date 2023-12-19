package com.orbitalhq.query

import com.orbitalhq.query.streams.MergedStream
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type

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
         is StreamJoiningExpression -> {
            val streamNodes = query.streamExpressions.flatMap { parse(it) }
            val projections = streamNodes.mapNotNull { it.projection }.distinct()
            if (projections.size > 1) { error("Found multiple projections in a stream joining query - this is an error") }
            val mergedStreamType = MergedStream.buildMergedStreamType(streamNodes.map { it.type })

            val mutations = streamNodes.mapNotNull { it.mutation }.distinct()
            if (projections.size > 1) { error("Found multiple mutations in a stream joining query - this is an error") }

            setOf(QuerySpecTypeNode(
               type = mergedStreamType,
               // Remove the mutation and the projection, as we want to operate those on the MERGED stream,
               // not the individual ones.
               children = streamNodes.map { it.copy(mutation = null, projection = null) }.toSet(),
               projection = projections.singleOrNull(),
               mutation = mutations.singleOrNull()
            ))
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
