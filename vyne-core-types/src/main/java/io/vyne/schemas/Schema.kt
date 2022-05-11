package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.VersionedSource
import io.vyne.VersionedTypeReference
import io.vyne.models.functions.FunctionRegistry
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.schemas.taxi.toVyneQualifiedName
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

   fun asTaxiSchema(): TaxiSchema

   @get:JsonIgnore
   val sources: List<VersionedSource>
   val types: Set<Type>
   val services: Set<Service>

   val policies: Set<Policy>

   @get:JsonIgnore
   val typeCache: TypeCache

   /**
    * Lists the names of types which have declared annotation types.
    * Note: We should replace this with references to an actual AnnotationType in the future
    */
   val metadataTypes: List<QualifiedName>

   /**
    * Returns the names of annotations present in the schema which do not have formal
    * types declared.
    */
   val dynamicMetadata: List<QualifiedName>

   @get:JsonIgnore
   val functionRegistry: FunctionRegistry
      get() = FunctionRegistry.default

   val operations: Set<Operation>
      get() = services.flatMap { it.operations }.toSet()

   val queryOperations: Set<QueryOperation>
      get() = services.flatMap { it.queryOperations }.toSet()

   @get:JsonIgnore
   val remoteOperations: Set<RemoteOperation>
      get() = (operations + queryOperations).toSet()

   fun operationsWithReturnType(
      requiredType: Type,
      typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES
   ): Set<Pair<Service, Operation>> {
      return services.flatMap { service ->
         service.operations.filter { operation -> typeMatchingStrategy.matches(requiredType, operation.returnType) }
            .map { service to it }
      }.toSet()
   }

   fun operationsWithReturnTypeAndWithSingleArgument(
      requiredReturnType: Type,
      requiredParameterType: Type,
      typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES
   ): Set<Pair<Service, Operation>> {
      return services.flatMap { service ->
         service.operations.filter { operation ->
            typeMatchingStrategy.matches(requiredReturnType, operation.returnType)
               && operation.parameters.size == 1 &&
               typeMatchingStrategy.matches(requiredParameterType, operation.parameters.first().type)
         }
            .map { service to it }
      }.toSet()
   }

   // Find All operations that takes a single argument
   // and the return type has a field with @Id annotation and with argument type.
   // Example:
   // model Instrument {
   //     @Id
   //     instrumentId: InstrumentId
   //     isin: Isin
   // }
   //
   // following operation should be returned by this function:
   // operation(InstrumentId): Instrument
   // whereas operation(Isin): Instrument should be filtered by this function.
   fun operationsWithSingleIdArgumentForReturnType(): Set<Operation> {
      return operationsWithSingleArgument().filter { (_, operation) ->
         val argument = operation.parameters.first()
         operation.returnType.attributes.values.firstOrNull { field ->
            argument.type.name == field.type &&
               field.metadata.firstOrNull { metadata -> metadata.name == VyneAnnotations.Id.annotation.fqn() } != null
         } != null
      }.map { it.second }.toSet()
   }

   /**
    * When enriching / projecting we are careful about which operations to include.
    * If a model declares an @Id attribute, then operations that return that model will be restricted
    * to those that accept the @Id attribute (and only the @Id attribute) as an input.
    * This is to avoid things like accidentally calling a findLastTradeByTrader(Trader):Trade
    * as an operation to enrich data about a completely unrelated entity.
    */
   fun excludedOperationsForEnrichment(): Set<Operation> {
      return operationsWithSingleArgument().filterNot { (_, operation) ->
         val argument = operation.parameters.first()
         operation.returnType.isScalar ||
            operation.returnType.attributes.values.all { field ->
               field.metadata.firstOrNull { metadata -> metadata.name == VyneAnnotations.Id.annotation.fqn() } == null
            } ||
            operation.returnType.attributes.values.firstOrNull { field ->
               argument.type.name == field.type &&
                  field.metadata.firstOrNull { metadata -> metadata.name == VyneAnnotations.Id.annotation.fqn() } != null
            } != null
      }.map { it.second }.toSet()
   }

   fun operationsWithSingleArgument(): Set<Pair<Service, Operation>> {
      return services.flatMap { service ->
         service.operations.filter { operation -> operation.parameters.size == 1 }
            .map { service to it }
      }.toSet()
   }

   fun operationsWithNoArgument(): Set<Pair<Service, Operation>> {
      return services.flatMap { service ->
         service.operations.filter { operation -> operation.parameters.size == 0 }
            .map { service to it }
      }.toSet()
   }

   fun servicesAndOperations(): Set<Pair<Service, Operation>> {
      return services.flatMap { service ->
         service.operations.map { service to it }
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

   fun versionedType(versionedTypeReference: VersionedTypeReference) =
      VersionedType(this.sources, type(versionedTypeReference.typeName), taxiType(versionedTypeReference.typeName))

   fun taxiType(name: QualifiedName): lang.taxi.types.Type

   fun type(taxiType: lang.taxi.types.Type): Type = type(taxiType.toVyneQualifiedName().parameterizedName.fqn())
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


   fun remoteOperation(operationName: QualifiedName): Pair<Service, RemoteOperation> {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(operationName)
      val service = service(serviceName)
      return service to service.remoteOperation(operationName)
   }

   fun operation(operationName: QualifiedName): Pair<Service, Operation> {
      return remoteOperation(operationName) as Pair<Service, Operation>
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

