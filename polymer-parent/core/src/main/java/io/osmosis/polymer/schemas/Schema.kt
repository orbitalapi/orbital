package io.osmosis.polymer.schemas

import io.osmosis.polymer.utils.assertingThat
import java.io.Serializable

fun String.fqn(): QualifiedName {
   return QualifiedName(this)
}

data class Metadata(val name: QualifiedName, val params: Map<String, Any?> = emptyMap())
data class TypeReference(val name: QualifiedName, val isCollection: Boolean = false)
data class QualifiedName(val fullyQualifiedName: String) : Serializable {
   val name: String
      get() = fullyQualifiedName.split(".").last()

   override fun toString(): String = fullyQualifiedName
}

typealias AttributeName = String
typealias AttributeType = QualifiedName
data class Type(val name: QualifiedName, val attributes: Map<AttributeName, TypeReference> = emptyMap()) {
   constructor(name: String, attributes: Map<AttributeName, TypeReference> = emptyMap()) : this(name.fqn(), attributes)

   val isScalar = attributes.isEmpty()
}


interface Schema {
   val types: Set<Type>
   val services: Set<Service>
   // TODO : Are these still required / meaningful?
   val attributes: Set<QualifiedName>
   val links: Set<Link>
   fun type(name: String): Type {
      return this.types.firstOrNull { it.name.fullyQualifiedName == name } ?:
         throw IllegalArgumentException("Type $name was not found within this schema")
   }

   fun type(name: QualifiedName): Type {
      return type(name.fullyQualifiedName)
   }

   fun hasType(name: String): Boolean {
      return this.types.any { it.name.fullyQualifiedName == name }
   }

   fun service(serviceName: String): Service {
      return this.services.firstOrNull { it.qualifiedName == serviceName } ?:
         throw IllegalArgumentException("Service $serviceName was not found within this schema")
   }

   fun operation(operationName: QualifiedName): Pair<Service, Operation> {
      val parts = operationName.fullyQualifiedName.split("@@").assertingThat({ it.size == 2 })
      val serviceName = parts[0]
      val operationName = parts[1]
      val service = service(serviceName)
      return service to service.operation(operationName)
   }
}
