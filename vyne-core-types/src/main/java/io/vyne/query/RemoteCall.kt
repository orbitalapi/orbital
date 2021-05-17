package io.vyne.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.QualifiedNameAsStringSerializer
import java.util.*

/**
 * A remote call models the response from a remote operation.
 * It's possible there are multiple repsonses for an operation, if the operation
 * returns a stream, or if application code converts a non-streaming response to a streaming
 * response.
 * Therefore, remoteCallId and responseId are seperate, to allow for this one-to-many relationship.
 *
 */
data class RemoteCall(
   /**
    * Use a consistent remoteCallId for all responses.
    */
   val remoteCallId: String = UUID.randomUUID().toString(),
   val responseId: String = UUID.randomUUID().toString(),
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

   @get:JsonIgnore
   val response: Any?
) {
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val operationQualifiedName: QualifiedName = OperationNames.qualifiedName(service.fullyQualifiedName, operation)

   val responseTypeDisplayName: String = responseTypeName.longDisplayName

}

