package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.query.VyneQlGrammar
import lang.taxi.services.OperationScope
import lang.taxi.services.QueryOperationCapability

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
   override val operationType: OperationScope = OperationScope.READ_ONLY
   override val returnTypeName: QualifiedName = returnType.name
   override val schemaMemberKind: SchemaMemberKind = SchemaMemberKind.OPERATION
   override val operationKind: OperationKind = OperationKind.Table

   companion object {
       fun findOneOperationName(operationName: String, returnTypeName: QualifiedName) =
           "${operationName}_findOne${returnTypeName.shortDisplayName}"

       fun findManyOperationName(operationName: String, returnTypeName: QualifiedName) =
           "${operationName}_findMany${returnTypeName.shortDisplayName}"

      fun build(
         qualifiedName: QualifiedName,
         returnType: Type,
         metadata: List<Metadata> = emptyList(),
         typeDoc: String? = null,
         schema: Schema
      ): TableOperation {
         val tableOperationQueryParam = Parameter(schema.type(VyneQlGrammar.QUERY_TYPE_NAME), "body", typeDoc = null)
         val queryOperations = buildQueryOperations(qualifiedName, returnType, tableOperationQueryParam)
         return TableOperation(
            qualifiedName, returnType, metadata, typeDoc, queryOperations
         )
      }


      /**
       * Constructs the query operations that the table operation is wrapping.
       *
       * A table really only exposes two query operations:
       *  - findExpectOne(query): T
       *  - findExpectMany(query): T[]
       *
       * It's down to the query constructor to build a query with the correct params that
       * control the specifics of "find what".
       *
       * ie: Don't build a series of findBySomeProperty(PropertyType):T, as that defeats the purpose of query operations.
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
                findOneOperationName(operationName.name, singleReturnType.qualifiedName)
            ),
            parameters = listOf(queryBodyParam),
            returnType = singleReturnType,
            grammar = VyneQlGrammar.GRAMMAR_NAME,
            capabilities = QueryOperationCapability.ALL
         )
         val collectionQueryOperation = QueryOperation(
            qualifiedName = QualifiedName.from(
               operationName.namespace,
                findManyOperationName(operationName.name, singleReturnType.qualifiedName)
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
