package io.osmosis.polymer.schemas

import io.osmosis.polymer.query.TypeMatchingStrategy
import io.osmosis.polymer.schemas.taxi.DeferredConstraintProvider
import io.osmosis.polymer.schemas.taxi.EmptyDeferredConstraintProvider
import io.osmosis.polymer.utils.assertingThat
import java.io.Serializable

fun String.fqn(): QualifiedName {
   return QualifiedName(this)
}

data class Metadata(val name: QualifiedName, val params: Map<String, Any?> = emptyMap())

// A pointer to a type.
// Useful when parsing, and the type that we're referring to may not have been parsed yet.
data class TypeReference(val name: QualifiedName, val isCollection: Boolean = false, private val constraintProvider: DeferredConstraintProvider = EmptyDeferredConstraintProvider()) {
   val constraints: List<Constraint>
      get() = constraintProvider.buildConstraints()
   val fullyQualifiedName: String
      get() = name.fullyQualifiedName

}

data class QualifiedName(val fullyQualifiedName: String) : Serializable {
   val name: String
      get() = fullyQualifiedName.split(".").last()

   override fun toString(): String = fullyQualifiedName
}

typealias AttributeName = String
typealias AttributeType = QualifiedName
typealias DeclaringType = QualifiedName

data class Type(
   val name: QualifiedName,
   val attributes: Map<AttributeName, TypeReference> = emptyMap(),
   val modifiers: List<Modifier> = emptyList(),
   val aliasForType: QualifiedName? = null,
   val inherits: List<Type> = emptyList(),
   val sources: List<SourceCode>
) {
   constructor(name: String, attributes: Map<AttributeName, TypeReference> = emptyMap(), modifiers: List<Modifier> = emptyList(), aliasForType: QualifiedName? = null, inherits: List<Type>, sources: List<SourceCode>) : this(name.fqn(), attributes, modifiers, aliasForType, inherits, sources)

   val isTypeAlias = aliasForType != null;
   val isScalar = attributes.isEmpty()
   val isParameterType: Boolean = this.modifiers.contains(Modifier.PARAMETER_TYPE)

   val fullyQualifiedName: String
      get() = name.fullyQualifiedName

   val inheritanceGraph = calculateInheritanceGraph()

   private fun calculateInheritanceGraph(typesToExclude: List<Type> = emptyList()): List<Type> {
      val allTypesToExclude = typesToExclude + listOf(this)
      return this.inherits.flatMap { inheritedType ->
         if (!allTypesToExclude.contains(inheritedType)) {
            setOf(inheritedType) + inheritedType.calculateInheritanceGraph(allTypesToExclude)
         } else emptySet()
      }
   }
}

enum class Modifier {
   PARAMETER_TYPE,
   // TODO : Is it right to treat these as modifiers?  They're not really,
   // but I'm trying to avoid a big collection of boolean flags
   ENUM,
   PRIMITIVE
}

data class SourceCode(
   val origin: String,
   val language: String,
   val content: String
) {
   companion object {
      fun undefined(language: String): SourceCode {
         return SourceCode("Unknown", language, "")
      }
   }
}

interface Schema {
   val types: Set<Type>
   val services: Set<Service>
   // TODO : Are these still required / meaningful?
   val attributes: Set<QualifiedName>
   val links: Set<Link>

   val operations: Set<Operation>
      get() = services.flatMap { it.operations }.toSet()

   fun operationsWithReturnType(requiredType: Type, typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES): Set<Pair<Service, Operation>> {
      return services.flatMap { service ->
         service.operations.filter { operation -> typeMatchingStrategy.matches(requiredType, operation.returnType) }
            .map { service to it }
      }.toSet()
   }

   fun type(name: String): Type {
      return this.types.firstOrNull { it.name.fullyQualifiedName == name }
         ?: throw IllegalArgumentException("Type $name was not found within this schema")
   }

   fun type(name: QualifiedName): Type {
      return type(name.fullyQualifiedName)
   }

   fun hasType(name: String): Boolean {
      return this.types.any { it.name.fullyQualifiedName == name }
   }

   fun service(serviceName: String): Service {
      return this.services.firstOrNull { it.qualifiedName == serviceName }
         ?: throw IllegalArgumentException("Service $serviceName was not found within this schema")
   }

   fun operation(operationName: QualifiedName): Pair<Service, Operation> {
      val parts = operationName.fullyQualifiedName.split("@@").assertingThat({ it.size == 2 })
      val serviceName = parts[0]
      val operationName = parts[1]
      val service = service(serviceName)
      return service to service.operation(operationName)
   }

   fun attribute(attributeName: String): Pair<Type, Type> {
      val parts = attributeName.split("/").assertingThat({ it.size == 2 })
      val declaringType = type(parts[0])
      val attributeType = type(declaringType.attributes[parts[1]]!!)

      return declaringType to attributeType

   }

   fun type(nestedTypeRef: TypeReference): Type {
      return type(nestedTypeRef.name)
   }
}
