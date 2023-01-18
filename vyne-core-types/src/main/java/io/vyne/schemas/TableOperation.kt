package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.query.VyneQlGrammar
import lang.taxi.services.QueryOperationCapability
import mu.KotlinLogging

/**
 * A Table operation is syntactic sugar for a query operation - it
 * represents table in a database that is queryable, but with a more concise
 * syntax.
 *
 * A table operation can be converted into two query operations, one returning T, and one
 * returning T[].
 */
@JsonDeserialize(`as` = TableOperation::class)
// Private constructor, as we should be calling "build", to ensure the
// query operations get constructed properly
data class TableOperation private constructor(
   override val qualifiedName: QualifiedName,
   @get:JsonSerialize(using = TypeAsNameJsonSerializer::class)
   override val returnType: Type,
   override val metadata: List<Metadata> = emptyList(),
   override val typeDoc: String? = null,
   // A TableOperation is really just a wrapper
   // around query operations.
   // To keep interoperability simple, we unwrap the table operation
   // to multiple query operations
   @get:JsonIgnore
   val queryOperations: List<QueryOperation>
) : PartialOperation, MetadataTarget, SchemaMember, RemoteOperation {
   override val parameters: List<Parameter> = emptyList()
   override val contract = OperationContract(returnType)
   override val operationType: String? = null
   override val returnTypeName: QualifiedName = returnType.name


   companion object {
      private val logger = KotlinLogging.logger {}
      fun findOneOperationName(operationName: String) = "${operationName}_FindOne"
      fun findManyOperationName(operationName: String) = "${operationName}_FindMany"

      fun build(
         qualifiedName: QualifiedName,
         returnType: Type,
         metadata: List<Metadata> = emptyList(),
         typeDoc: String? = null,
         schema: Schema
      ): TableOperation {
         val tableOperationQueryParam = try {
            Parameter(schema.type(VyneQlGrammar.QUERY_TYPE_NAME), "body")
         } catch (e: Exception) {
            logger.warn { "${VyneQlGrammar.QUERY_TYPE_NAME} is not present in the current schema, so Table operations will not currently work.  If you're seeing this in production, this could cause queries to fail.  Ensure that a query service has published Vyne's core schema" }
            null
         }


         val queryOperations =
            tableOperationQueryParam?.let { queryParam -> buildQueryOperations(qualifiedName, returnType, queryParam) }
               ?: emptyList()
         return TableOperation(
            qualifiedName, returnType, metadata, typeDoc, queryOperations
         )
      }


      /**
       * Constructs the query operations that the table operation is wrapping
       */
      private fun buildQueryOperations(
         operationName: QualifiedName,
         returnType: Type,
         queryBodyParam: Parameter
      ): List<QueryOperation> {
         require(returnType.isCollection) { "Expected the return type of a table operation to be an array" }
         val singleReturnType = returnType.collectionType!!
         val singleRecordQueryOperation = QueryOperation(
            qualifiedName = QualifiedName.from(
               operationName.namespace,
               findOneOperationName(operationName.name)
            ),
            parameters = listOf(queryBodyParam),
            returnType = singleReturnType,
            grammar = VyneQlGrammar.GRAMMAR_NAME,
            capabilities = QueryOperationCapability.ALL
         )
         val collectionQueryOperation = QueryOperation(
            qualifiedName = QualifiedName.from(
               operationName.namespace,
               findManyOperationName(operationName.name)
            ),
            parameters = listOf(queryBodyParam),
            returnType = returnType,
            grammar = VyneQlGrammar.GRAMMAR_NAME,
            capabilities = QueryOperationCapability.ALL
         )
         return listOf(
            singleRecordQueryOperation,
            collectionQueryOperation
         )
      }
   }

}
