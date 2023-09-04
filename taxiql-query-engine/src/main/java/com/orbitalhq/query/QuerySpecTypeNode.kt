package com.orbitalhq.query

import com.fasterxml.jackson.annotation.JsonInclude
import com.orbitalhq.schemas.OutputConstraint
import com.orbitalhq.schemas.Type
import lang.taxi.mutations.Mutation
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging

/**
 * Defines a node within a QuerySpec that
 * describes the expected return type.
 * eg:
 * Given
 * {
 *    Client {  // <---QuerySpecTypeNode
 *       ClientId, ClientFirstName, ClientLastName // <--- 3 Children, all QuerySpecTypeNode's too!
 *    }
 * }
 *
 */
// TODO : Why isn't the type enough, given that has children?  Why do I need to explicitly list the children I want?
// TODO : This concept feels largely deprecated, and should be replaced by something like QueryExpression.
// This was created before we had TaxiQL, and there are better implementations of it's concepts now.
@JsonInclude(JsonInclude.Include.NON_NULL)
data class QuerySpecTypeNode(
   val type: Type,
   @Deprecated("Not used, not required")
   val children: Set<QuerySpecTypeNode> = emptySet(),
   val mode: QueryMode = QueryMode.DISCOVER,
   // Note: Not really convinced these need to be OutputCOnstraints (vs Constraints).
   // Revisit later
   val dataConstraints: List<OutputConstraint> = emptyList(),
   val projection: Projection? = null,
   val mutation: Mutation? = null
) {

   fun anonymousTypes(): Set<Type> {
      return this.type.anonymousTypes + (projection?.type?.anonymousTypes ?: emptySet())
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      if (type.isCollection && type.collectionTypeName!!.fullyQualifiedName == PrimitiveType.ANY.qualifiedName) {
         logger.warn { "Performing a search for Any[] is likely a bug" }
      }
   }

   val description = type.name.shortDisplayName
}


