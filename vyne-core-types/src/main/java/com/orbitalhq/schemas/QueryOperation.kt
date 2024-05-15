package com.orbitalhq.schemas

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.orbitalhq.utils.ImmutableEquality
import lang.taxi.services.FilterCapability
import lang.taxi.services.OperationScope
import lang.taxi.services.QueryOperationCapability

@JsonDeserialize(`as` = QueryOperation::class)
data class QueryOperation(
   override val qualifiedName: QualifiedName,
   override val parameters: List<Parameter>,
   @get:JsonSerialize(using = TypeAsNameJsonSerializer::class)
   override val returnType: Type,
   override val metadata: List<Metadata> = emptyList(),
   override val grammar: String,
   override val capabilities: List<QueryOperationCapability>,
   override val typeDoc: String? = null
) : MetadataTarget, SchemaMember, RemoteOperation, PartialQueryOperation {
   override val contract = OperationContract(returnType)
   override val operationType: OperationScope = OperationScope.READ_ONLY
   private val filterCapability: FilterCapability? = capabilities
      .filterIsInstance<FilterCapability>()
      .firstOrNull()

   override val hasFilterCapability = this.filterCapability != null
   override val supportedFilterOperations = filterCapability?.supportedOperations ?: emptyList()

   override val returnTypeName: QualifiedName = returnType.name

   private val cachedHashCode: Int = run {
      var result = name.hashCode()
      result = 31 * result + parameters.hashCode()
      result = 31 * result + metadata.hashCode()
      result = 31 * result + returnType.hashCode()
      result = 31 * result + typeDoc.hashCode()
      result
   }

   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is QueryOperation) return false

      return name == other.name &&
         parameters == other.parameters &&
         metadata == other.metadata &&
         returnType == other.returnType &&
         typeDoc == other.typeDoc
   }

   override fun hashCode(): Int {
      return cachedHashCode
   }

   override val schemaMemberKind: SchemaMemberKind = SchemaMemberKind.OPERATION
   override val operationKind: OperationKind = OperationKind.Query
}
