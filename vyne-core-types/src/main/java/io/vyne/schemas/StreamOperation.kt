package io.vyne.schemas

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/**
 * A StreamOperation is an operation that connects to a streaming data source.
 * (eg., a Message broker like Kafka).
 * Note that other operations (such as http operation and query operations)
 * can return Stream<T>.  That's perfectly valid.
 *
 * Also, while an Operation may return Stream<T>, a stream operation MUST return
 * Stream<T> - ie., it is invalid for a stream operation to return T.
 *
 * StreamOperations encapsulate operations that exclusively require a special
 * streaming connector and driver.
 */
// Need to use @JsonDeserialize on this type, as the PartialXxxx
// interface is overriding default deserialization behaviour
// causing all of these to be deserialized as partials, even when
// they're the real thing
@JsonDeserialize(`as` = StreamOperation::class)
data class StreamOperation(
   override val qualifiedName: QualifiedName,
   @get:JsonSerialize(using = TypeAsNameJsonSerializer::class)
   override val returnType: Type,
   override val metadata: List<Metadata> = emptyList(),
   override val typeDoc: String? = null
) : PartialOperation, MetadataTarget, SchemaMember, RemoteOperation {
   override val parameters: List<Parameter> = emptyList()
   override val contract = OperationContract(returnType)
   override val operationType: String? = null

   override val returnTypeName: QualifiedName = returnType.name
   override val schemaMemberKind: SchemaMemberKind = SchemaMemberKind.OPERATION
   override val operationKind: OperationKind = OperationKind.Stream
}
