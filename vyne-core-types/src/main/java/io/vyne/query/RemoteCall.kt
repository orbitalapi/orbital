package io.vyne.query

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringSerializer

data class RemoteCall(
   @get:JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   val service: QualifiedName,
   val addresss: String,
   val operation: String,
   @get:JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   val responseTypeName: QualifiedName,
   val method: String,
   val requestBody: Any?,
   val resultCode: Int,
   val durationMs: Long,
   val response: Any?
) {
   @get:JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   val operationQualifiedName: QualifiedName = OperationNames.qualifiedName(service.fullyQualifiedName, operation)
}

