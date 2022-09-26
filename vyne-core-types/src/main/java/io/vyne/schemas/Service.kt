package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.VersionedSource
import io.vyne.models.TypedInstance
import io.vyne.query.RemoteCall
import io.vyne.utils.ImmutableEquality
import lang.taxi.Equality
import lang.taxi.services.FilterCapability
import lang.taxi.services.QueryOperationCapability
import java.io.Serializable


typealias OperationName = String
typealias ServiceName = String

object ParamNames {
   fun isParamName(input: String): Boolean {
      return input.startsWith("param/")
   }

   fun typeNameInParamName(paramName: String): String {
      return paramName.removePrefix("param/")
   }

   fun toParamName(typeName: String): String {
      return "param/$typeName"
   }
}

object OperationNames {
   private const val DELIMITER: String = "@@"
   fun name(serviceName: String, operationName: String): String {
      return listOf(serviceName, operationName).joinToString(DELIMITER)
   }

   fun displayName(serviceName: String, operationName: OperationName): String {
      return "$serviceName / $operationName"
   }

   fun qualifiedName(serviceName: ServiceName, operationName: OperationName): QualifiedName {
      return name(serviceName, operationName).fqn()
   }

   fun serviceAndOperation(qualifiedOperationName: String): Pair<ServiceName, OperationName> {
      val parts = qualifiedOperationName.split(DELIMITER)
      require(parts.size == 2) { "$qualifiedOperationName is not a valid operation name." }
      return parts[0] to parts[1]
   }

   fun serviceAndOperation(qualifiedOperationName: QualifiedName): Pair<ServiceName, OperationName> {
      return serviceAndOperation(qualifiedOperationName.fullyQualifiedName)
   }

   fun operationName(qualifiedOperationName: QualifiedName): OperationName {
      return serviceAndOperation(qualifiedOperationName).second
   }

   fun serviceName(qualifiedOperationName: QualifiedName): ServiceName {
      return serviceAndOperation(qualifiedOperationName).first
   }

   fun isName(memberName: String): Boolean {
      return memberName.contains(DELIMITER)
   }

   fun isName(memberName: QualifiedName): Boolean {
      return memberName.fullyQualifiedName.contains(DELIMITER)
   }

   fun shortDisplayNameFromOperation(operationName: QualifiedName): String {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(operationName)
      return displayName(serviceName.fqn().shortDisplayName, operationName)
   }

   fun displayNameFromOperationName(operationName: QualifiedName): String {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(operationName)
      return displayName(serviceName, operationName)
   }
}

data class Parameter(
   @get:JsonSerialize(using = TypeAsNameJsonSerializer::class)
   val type: Type,
   override val name: String? = null,
   override val metadata: List<Metadata> = emptyList(),
   val constraints: List<InputConstraint> = emptyList()
) : MetadataTarget, PartialParameter {
   fun isNamed(name: String): Boolean {
      return this.name != null && this.name == name
   }

   private val equality = ImmutableEquality(
      this,
      Parameter::name,
      Parameter::type,
      Parameter::metadata,
   )

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()

   override val typeName: QualifiedName = type.name
}

data class Operation(
   override val qualifiedName: QualifiedName,
   override val parameters: List<Parameter>,
   @get:JsonSerialize(using = TypeAsNameJsonSerializer::class)
   override val returnType: Type,
   // similar to scope in taxi - ie., read / write
   override val operationType: String? = null,
   override val metadata: List<Metadata> = emptyList(),
   override val contract: OperationContract = OperationContract(returnType),
   @get:JsonIgnore
   val sources: List<VersionedSource>,
   override val typeDoc: String? = null
) : MetadataTarget, SchemaMember, RemoteOperation, PartialOperation {
   private val equality =
      Equality(this, Operation::qualifiedName, Operation::returnType, Operation::parameters, Operation::metadata)

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int {
      return equality.hash()
   }

   fun parameter(name: String): Parameter? {
      return this.parameters.firstOrNull { it.name == name }
   }

   override val returnTypeName: QualifiedName = returnType.name
}

/**
 * The base interface that covers both traditional operations
 * ( Operation ), and query operations (QueryOperation)
 *
 */
interface RemoteOperation : MetadataTarget {
   val qualifiedName: QualifiedName
   val parameters: List<Parameter>
   val returnType: Type
   val contract: OperationContract

   val operationType: String?

   val name: String
      get() = OperationNames.operationName(qualifiedName)
}

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
   override val operationType: String? = null
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
}

data class TableOperation(
   override val qualifiedName: QualifiedName,
   @get:JsonSerialize(using = TypeAsNameJsonSerializer::class)
   override val returnType: Type,
   override val metadata: List<Metadata> = emptyList(),
   val typeDoc: String? = null
) : MetadataTarget, SchemaMember, RemoteOperation {
   override val parameters: List<Parameter> = emptyList()
   override val contract = OperationContract(returnType)
   override val operationType: String? = null
   val returnTypeName: QualifiedName = returnType.name

}

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
data class StreamOperation(
   override val qualifiedName: QualifiedName,
   @get:JsonSerialize(using = TypeAsNameJsonSerializer::class)
   override val returnType: Type,
   override val metadata: List<Metadata> = emptyList(),
   val typeDoc: String? = null
) : MetadataTarget, SchemaMember, RemoteOperation {
   override val parameters: List<Parameter> = emptyList()
   override val contract = OperationContract(returnType)
   override val operationType: String? = null

   val returnTypeName: QualifiedName = returnType.name
}

data class ConsumedOperation(val serviceName: ServiceName, val operationName: String) {
   val operationQualifiedName: QualifiedName = OperationNames.qualifiedName(serviceName, operationName)
}

data class ServiceLineage(
   val consumes: List<ConsumedOperation>,
   val stores: List<QualifiedName>,
   val metadata: List<Metadata>
) {
   fun consumesOperation(operationName: QualifiedName): Boolean {
      return consumes.any {
         it.operationQualifiedName == operationName
      }
   }

   fun getConsumerOf(operationName: QualifiedName): List<ConsumedOperation> {
      return this.consumes.filter { it.operationQualifiedName == operationName }
   }

   companion object {
      fun empty() = ServiceLineage(emptyList(), emptyList(), emptyList())
   }
}


enum class ServiceKind : Serializable {
   API,
   Database,
   Kafka;

   companion object {

      fun inferFromMetadata(
         serviceMetadata: List<Metadata>,
         operations: List<Operation> = emptyList(),
         streamOperations: List<StreamOperation>,
         tableOperations: List<TableOperation>
      ): ServiceKind? {
         val allOperationMetadata = operations.flatMap { it.metadata }
         val hasOperations = operations.isNotEmpty()
         val hasStreams = streamOperations.isNotEmpty()
         val hasTables = tableOperations.isNotEmpty()
         // We have to use string literals here, rather than constants, as we don't have
         // compile time dependencies on the connector libraries in core.
         return when {
            !hasOperations && !hasStreams && hasTables -> Database
            !hasOperations && hasStreams && !hasTables -> Kafka

            serviceMetadata.containsMetadata("io.vyne.kafka.KafkaService") -> Kafka
            serviceMetadata.containsMetadata("io.vyne.jdbc.DatabaseService") -> Database
            allOperationMetadata.containsMetadata("HttpOperation") -> API
            else -> null
         }
      }
   }
}


data class Service(
   override val name: QualifiedName,
   override val operations: List<Operation>,
   override val queryOperations: List<QueryOperation>,
   override val streamOperations: List<StreamOperation> = emptyList(),
   override val tableOperations: List<TableOperation> = emptyList(),
   override val metadata: List<Metadata> = emptyList(),
   val sourceCode: List<VersionedSource>,
   override val typeDoc: String? = null,
   val lineage: ServiceLineage? = null,
   val serviceKind: ServiceKind? = ServiceKind.inferFromMetadata(
      metadata,
      operations,
      streamOperations,
      tableOperations
   ),
) : MetadataTarget, SchemaMember, PartialService {

   private val equality = ImmutableEquality(
      this,
      Service::name,
      // 11-Aug-22: Added attributes and docs as needed for diffing.
      // However, if this trashes performance, we can revert,and we'll find another way.
      Service::operations,
      Service::queryOperations,
      Service::tableOperations,
      Service::streamOperations,
      Service::typeDoc,
      Service::metadata
   )

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()


   fun queryOperation(name: String): QueryOperation {
      return this.queryOperations.first { it.name == name }
   }

   fun operation(name: String): Operation {
      return this.operations.first { it.name == name }
   }

   val remoteOperations: List<RemoteOperation> = operations + queryOperations

   fun remoteOperation(name: String): RemoteOperation {
      return this.queryOperations.firstOrNull { it.name == name }
         ?: this.operations.first { it.name == name }
   }

   fun hasOperation(name: String): Boolean {
      return this.operations.any { it.name == name }
   }

   val qualifiedName = name.fullyQualifiedName
}


data class OperationContract(
   @JsonSerialize(using = TypeAsNameJsonSerializer::class)
   val returnType: Type, val constraints: List<OutputConstraint> = emptyList()
) {
   fun containsConstraint(clazz: Class<out OutputConstraint>): Boolean {
      return constraints.filterIsInstance(clazz)
         .isNotEmpty()
   }

   fun <T : OutputConstraint> containsConstraint(clazz: Class<T>, predicate: (T) -> Boolean): Boolean {
      return constraints.filterIsInstance(clazz).any(predicate)
   }

   fun <T : OutputConstraint> constraint(clazz: Class<T>, predicate: (T) -> Boolean): T {
      return constraints.filterIsInstance(clazz).first(predicate)
   }

   fun <T : OutputConstraint> constraint(clazz: Class<T>): T {
      return constraints.filterIsInstance(clazz).first()
   }
}


fun RemoteOperation.httpOperationMetadata(): VyneHttpOperation {
   val annotation = metadata("HttpOperation")
   val url = annotation.params["url"] as String
   val method = annotation.params["method"] as String
   return VyneHttpOperation(httpOperationMetadata = annotation, url = url, method = method)
}

data class VyneHttpOperation(val httpOperationMetadata: Metadata, val url: String, val method: String)

class OperationInvocationException(
   message: String,
   val httpStatus: Int,
   val remoteCall: RemoteCall,
   val parameters: List<Pair<Parameter, TypedInstance>>
) : RuntimeException(message)

private fun List<Metadata>.containsMetadata(name: String): Boolean {
   return this.any { it.name.fullyQualifiedName == name }
}
