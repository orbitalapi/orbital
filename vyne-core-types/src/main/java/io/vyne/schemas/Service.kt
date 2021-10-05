package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.VersionedSource
import lang.taxi.Equality
import lang.taxi.services.FilterCapability
import lang.taxi.services.QueryOperationCapability


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

   fun serviceAndOperation(qualifiedOperationName: QualifiedName): Pair<ServiceName, OperationName> {
      val parts = qualifiedOperationName.fullyQualifiedName.split(DELIMITER)
      require(parts.size == 2) { "${qualifiedOperationName.fullyQualifiedName} is not a valid operation name." }
      return parts[0] to parts[1]
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
   val name: String? = null,
   override val metadata: List<Metadata> = emptyList(),
   val constraints: List<InputConstraint> = emptyList()) : MetadataTarget {
   fun isNamed(name: String): Boolean {
      return this.name != null && this.name == name
   }
}

data class Operation(override val qualifiedName: QualifiedName,
                     override val parameters: List<Parameter>,
                     @get:JsonSerialize(using = TypeAsNameJsonSerializer::class)
                     override val returnType: Type,
   // similar to scope in taxi - ie., read / write
                     override val operationType: String? = null,
                     override val metadata: List<Metadata> = emptyList(),
                     override val contract: OperationContract = OperationContract(returnType),
                     @get:JsonIgnore
                     val sources: List<VersionedSource>,
                     val typeDoc: String? = null) : MetadataTarget, SchemaMember, RemoteOperation {
   private val equality = Equality(this, Operation::qualifiedName, Operation::returnType)
   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int  {
     return equality.hash()
   }

   fun parameter(name: String): Parameter? {
      return this.parameters.firstOrNull { it.name == name }
   }
}

interface RemoteOperation : MetadataTarget {
   val qualifiedName: QualifiedName
   val parameters: List<Parameter>
   val returnType: Type
   val contract: OperationContract

   val operationType: String?

   val name: String
      get() = OperationNames.operationName(qualifiedName)
}

data class QueryOperation(override val qualifiedName: QualifiedName,
                          override val parameters: List<Parameter>,
                          @get:JsonSerialize(using = TypeAsNameJsonSerializer::class)
                          override val returnType: Type,
                          override val metadata: List<Metadata> = emptyList(),
                          val grammar: String,
                          val capabilities: List<QueryOperationCapability>,
                          val typeDoc: String? = null
) : MetadataTarget, SchemaMember, RemoteOperation {
   override val contract = OperationContract(returnType)
   override val operationType: String? = null
   private val filterCapability: FilterCapability? = capabilities
      .filterIsInstance<FilterCapability>()
      .firstOrNull()

   val hasFilterCapability = this.filterCapability != null
   val supportedFilterOperations = filterCapability?.supportedOperations ?: emptyList()


}
data class ConsumedOperation(val serviceName: ServiceName, val operationName: String)
data class ServiceLineage(val consumes: List<ConsumedOperation>,
                          val stores: List<QualifiedName>,
                          val metadata: List<Metadata>) {
   companion object {
      fun empty() = ServiceLineage(emptyList(), emptyList(), emptyList())
   }
}

data class Service(val name: QualifiedName,
                   val operations: List<Operation>,
                   val queryOperations: List<QueryOperation>,
                   override val metadata: List<Metadata> = emptyList(),
                   val sourceCode: List<VersionedSource>,
                   val typeDoc: String? = null,
                   val lineage: ServiceLineage? = null) : MetadataTarget, SchemaMember {
   fun queryOperation(name: String): QueryOperation {
      return this.queryOperations.first { it.name == name }
   }

   fun operation(name: String): Operation {
      return this.operations.first { it.name == name }
   }

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
   val returnType: Type, val constraints: List<OutputConstraint> = emptyList()) {
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
