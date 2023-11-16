package com.orbitalhq.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import com.orbitalhq.*
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.schemas.TaxiTypeMapper.fromTaxiType
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.utils.assertingThat
import lang.taxi.TaxiDocument
import lang.taxi.packages.SourcesType
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.ArrayType


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
   val packages: List<SourcePackage>

   @get:JsonIgnore
   val sources: List<VersionedSource>
   val types: Set<Type>
   val services: Set<Service>

   val policies: Set<Policy>

   val queries: Set<SavedQuery>

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

   @get:JsonIgnore
   val additionalSourcePaths: List<Pair<String, PathGlob>>
      get() {
         return emptyList()
      }

   @get:JsonIgnore
   val additionalSources: Map<SourcesType, List<SourcePackage>>
      get() {
         // Grabs all the additional sources in the sourcePackages,
         // and flattens / combines them into a map, based on the source type
         val loadedSources = this.packages.flatMap { sourcePackage ->
            sourcePackage.additionalSources.map { (sourcesType, sources) ->
               sourcesType to SourcePackage(sourcePackage.packageMetadata, sources, emptyMap())
            }
         }.groupBy({ it.first }, { it.second })
         return loadedSources
      }


   val operations: Set<Operation>
      get() = services.flatMap { it.operations }.toSet()


   val queryAndTableOperations:Set<RemoteOperation>
      get() = queryOperations + tableOperations

   val queryOperations: Set<QueryOperation>
      get() = services.flatMap { it.queryOperations }.toSet()

   val tableOperations: Set<TableOperation>
      get() = services.flatMap { it.tableOperations }.toSet()
   val streamOperations: Set<StreamOperation>
      get() = services.flatMap { it.streamOperations }.toSet()

   @get:JsonIgnore
   val remoteOperations: Set<RemoteOperation>
      get() = services.flatMap { it.remoteOperations }.toSet()

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

   fun operationsWithNoArgument(): Set<Pair<Service, RemoteOperation>> {
      return servicesAndRemoteOperations()
         .filter { (service, remoteOperation) -> remoteOperation.parameters.isEmpty() }
         .toSet()
//      return services.flatMap { service ->
//         service.operations.filter { operation -> operation.parameters.size == 0 }
//            .map { service to it }
//      }.toSet()
   }

   fun servicesAndRemoteOperations(): Set<Pair<Service, RemoteOperation>> {
      return services.flatMap { service -> service.remoteOperations.map { operation -> service to operation } }
         .toSet()
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
      return this.services.any { it.fullyQualifiedName == serviceName }
   }

   fun service(serviceName: String): Service {
      return this.services.firstOrNull { it.fullyQualifiedName == serviceName }
         ?: throw IllegalArgumentException("Service $serviceName was not found within this schema")
   }

   fun serviceOrNull(serviceName: QualifiedName): Service? {
      return if (hasService(serviceName.fullyQualifiedName)) service(serviceName.fullyQualifiedName) else null
   }

   fun typeOrNull(typeName: String): Type? {
      return if (hasType(typeName)) {
         type(typeName)
      } else null
   }

   fun typeOrNull(typeName: QualifiedName): Type? {
      return typeOrNull(typeName.fullyQualifiedName)
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
      return type(typeRef.typeName)
   }

   fun type(nestedTypeRef: TypeReference): Type {
      return type(nestedTypeRef.name)
   }

   fun toTaxiType(versionedType: VersionedType) = type(versionedType.fullyQualifiedName.fqn()).taxiType

   fun getSourcePackageOrNull(rawPackageIdentifier: String): SourcePackage? {
      val packageIdentifier = PackageIdentifier.fromId(rawPackageIdentifier)

      // This is a brute-force approach, since we don't currently store a reference of schema members to the
      // sources they came from.
      return this.packages.firstOrNull { it.identifier == packageIdentifier }
   }

   fun getPartialSchemaForPackage(rawPackageIdentifier: String): PartialSchema {
      val sourcePackageOrNull = this.getSourcePackageOrNull(rawPackageIdentifier)
      val types = sourcePackageOrNull?.let { sourcePackage ->
         this.types
            .filter { it.sources.any { source -> source.packageIdentifier == sourcePackage.identifier } }
      } ?: emptySet()
      val services = sourcePackageOrNull?.let { sourcePackage ->
         this.services
            .filter { it.sourceCode.any { source -> source.packageIdentifier == sourcePackage.identifier } }
      } ?: emptySet()
      return DefaultPartialSchema(
         types.toSet(),
         services.toSet()
      )
   }

   fun getMember(name: QualifiedName): SchemaMember {
      return if (OperationNames.isName(name)) {
         val (serviceName, operation) = OperationNames.serviceAndOperation(name)
         val service = this.service(serviceName)
         service.remoteOperation(operation)
      } else {
         this.serviceOrNull(name) ?: this.typeOrNull(name)
         ?: error("No schema member named ${name.fullyQualifiedName} found")
      }

   }

   fun parseQuery(vyneQlQuery: TaxiQLQueryString): Pair<TaxiQlQuery, QueryOptions>

   /**
    * Looks up the type, and will construct a new type from the provided taxi type if not present.
    * This should only be called in edge cases where we aren't sure we've already converted the type.
    * Currently, the only known edge case is nested anonymous types inside expressions, where
    * we need to find a better solution, but haven't yet.
    * If you're calling this from somewhere else, it's worth investigating why.
    */
   fun typeCreateIfRequired(taxiType: lang.taxi.types.Type): Type {
      val name = taxiType.toVyneQualifiedName().parameterizedName

      return when {
         this.hasType(name) -> this.type(name)
         taxiType is ArrayType -> {
            val vyneMemberType = typeCreateIfRequired(taxiType.memberType)
            vyneMemberType.asArrayType()
         }

         else -> fromTaxiType(taxiType, this)
      }
   }

}


