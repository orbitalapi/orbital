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

   private val equality = ImmutableEquality(
      this,
      QueryOperation::name,
      // 11-Aug-22: Added attributes and docs as needed for diffing.
      // However, if this trashes performance, we can revert,and we'll find another way.
      QueryOperation::parameters,
      QueryOperation::metadata,
      QueryOperation::returnType,
      QueryOperation::typeDoc
   )

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()
   override val schemaMemberKind: SchemaMemberKind = SchemaMemberKind.OPERATION
   override val operationKind: OperationKind = OperationKind.Query
}
