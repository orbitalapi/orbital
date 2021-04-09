package io.vyne.query

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.QualifiedNameAsStringSerializer
import java.util.*

data class RemoteCall(
   val remoteCallId:String = UUID.randomUUID().toString(),
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val service: QualifiedName,
   val address: String,
   val operation: String,
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val responseTypeName: QualifiedName,
   val method: String,
   val requestBody: Any?,
   val resultCode: Int,
   val durationMs: Long,
   val response: Any?
) {
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val operationQualifiedName: QualifiedName = OperationNames.qualifiedName(service.fullyQualifiedName, operation)

   val responseTypeDisplayName:String = responseTypeName.longDisplayName
}

