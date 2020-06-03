package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.VersionedSource
import io.vyne.VersionedTypeReference
import io.vyne.utils.assertingThat
import io.vyne.utils.log
import lang.taxi.TaxiDocument


/**
 * Note - implementors must provide valid
 * Hashcode and Equals implementations, as Schemas
 * are used heavily in caching.
 */
interface Schema {
   // I've pretty much given up on avoiding the Taxi vs Schema abstraction at this point..
   @get:JsonIgnore
   val taxi: TaxiDocument

   @get:JsonIgnore
   val sources: List<VersionedSource>
   val types: Set<Type>
   val services: Set<Service>

   val policies: Set<Policy>

   @get:JsonIgnore
   val typeCache: TypeCache


   val operations: Set<Operation>
      get() = services.flatMap { it.operations }.toSet()

   fun operationsWithReturnType(requiredType: Type, typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES): Set<Pair<Service, Operation>> {
      return services.flatMap { service ->
         service.operations.filter { operation -> typeMatchingStrategy.matches(requiredType, operation.returnType) }
            .map { service to it }
      }.toSet()
   }

   fun type(name: String): Type {
      val type = typeCache.type(name)
      return type
   }

   // Note - in the future, we may wish to be smart, and only include the
   // sources that contributed to the types definition.
   // That's a bit too much work for now.
   fun versionedType(name: QualifiedName) = VersionedType(this.sources, type(name), taxiType(name))

   fun versionedType(versionedTypeReference: VersionedTypeReference) = VersionedType(this.sources, type(versionedTypeReference.typeName), taxiType(versionedTypeReference.typeName))

   fun taxiType(name: QualifiedName): lang.taxi.types.Type

   fun type(taxiType: lang.taxi.types.Type): Type = type(taxiType.qualifiedName.fqn())
   fun type(name: QualifiedName) = typeCache.type(name)

   fun hasType(name: String) = typeCache.hasType(name)

   fun hasService(serviceName: String): Boolean {
      return this.services.any { it.qualifiedName == serviceName }
   }

   fun service(serviceName: String): Service {
      return this.services.firstOrNull { it.qualifiedName == serviceName }
         ?: throw IllegalArgumentException("Service $serviceName was not found within this schema")
   }

   fun policy(type: Type): Policy? {
      return this.policies.firstOrNull { it.targetType.fullyQualifiedName == type.fullyQualifiedName }
   }

   fun hasOperation(operationName: QualifiedName): Boolean {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(operationName)
      if (!hasService(serviceName)) return false

      val service = service(serviceName)
      return service.hasOperation(operationName)
   }

   fun operation(operationName: QualifiedName): Pair<Service, Operation> {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(operationName)
      val service = service(serviceName)
      return service to service.operation(operationName)
   }

   fun attribute(attributeName: String): Pair<Type, Type> {
      val parts = attributeName.split("/").assertingThat({ it.size == 2 })
      val declaringType = type(parts[0])
      val attributeType = type(declaringType.attributes.getValue(parts[1]).type)

      return declaringType to attributeType

   }

   fun type(typeRef: VersionedTypeReference): Type {
      // TODO
      log().warn("Not validating type versions")
      return type(typeRef.typeName)
   }

   fun type(nestedTypeRef: TypeReference): Type {
      return type(nestedTypeRef.name)
   }

   fun toTaxiType(versionedType: VersionedType) = type(versionedType.fullyQualifiedName.fqn()).taxiType
}

