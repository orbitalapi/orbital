package com.orbitalhq.schemas

import lang.taxi.query.ServiceRestriction
import lang.taxi.query.ServiceRestrictions

fun ServiceRestrictions.applyTo(schema: Schema): Schema {
   return if (this.isEmpty) {
      schema
   } else {
      ServiceFilteredSchema(schema, this)
   }
}

/**
 * A specialized view of a schema where query restrictions are applied, filtering the set of services
 * and operations
 */
class ServiceFilteredSchema(private val schema: Schema, private val restrictions: ServiceRestrictions) :
   Schema by schema {
   override val services: Set<Service>
      get() {
         return schema.services.mapNotNull { service -> filter(service) }
            .toSet()
      }

   override val operations: Set<Operation>
      get() = super.operations

   override val remoteOperations: Set<RemoteOperation>
      get() = super.remoteOperations

   override val tableOperations: Set<TableOperation>
      get() = super.tableOperations

   override val streamOperations: Set<StreamOperation>
      get() = super.streamOperations

   // These are placeholders, but we don't want these
   // methods deferring to the wrapped schema, instead
   // using the default implementation from our Schema interface
   override fun service(serviceName: String): Service {
      return super.service(serviceName)
   }

   // These are placeholders, but we don't want these
   // methods deferring to the wrapped schema, instead
   // using the default implementation from our Schema interface
   override fun hasService(serviceName: String): Boolean {
      return super.hasService(serviceName)
   }

   // These are placeholders, but we don't want these
   // methods deferring to the wrapped schema, instead
   // using the default implementation from our Schema interface
   override fun serviceOrNull(serviceName: QualifiedName): Service? {
      return super.serviceOrNull(serviceName)
   }

   // These are placeholders, but we don't want these
   // methods deferring to the wrapped schema, instead
   // using the default implementation from our Schema interface
   override fun remoteOperation(operationName: QualifiedName): Pair<Service, RemoteOperation> {
      return super.remoteOperation(operationName)
   }

   // These are placeholders, but we don't want these
   // methods deferring to the wrapped schema, instead
   // using the default implementation from our Schema interface
   override fun remoteOperationOrNull(operationName: QualifiedName): RemoteOperation? {
      return super.remoteOperationOrNull(operationName)
   }

   // These are placeholders, but we don't want these
   // methods deferring to the wrapped schema, instead
   // using the default implementation from our Schema interface
   override fun servicesAndRemoteOperations(): Set<Pair<Service, RemoteOperation>> {
      return super.servicesAndRemoteOperations()
   }

   private fun filter(service: Service): Service? {
      return when {
         restrictions.hasInclusions -> filterIncluded(service)
         restrictions.hasExclusions -> filterExcluded(service)
         else -> service
      }
   }

   private fun filterIncluded(service: Service): Service? {
      val serviceRestriction = restrictions.inclusionFor(service.name.toTaxiQualifiedName())
         ?: return null
      return if (serviceRestriction.operations.isNotEmpty()) {
         Service(
            service.name,
            service.operations.filter { serviceRestriction.hasOperation(it.name) },
            service.queryOperations.filter { serviceRestriction.hasOperation(it.name) },
            service.streamOperations.filter { serviceRestriction.hasOperation(it.name) },
            service.tableOperations.filter { serviceRestriction.hasOperation(it.name) },
            service.metadata,
            service.sourceCode,
            service.typeDoc,
            service.lineage,
            service.serviceKind
         )
      } else {
         service
      }
   }

   private fun filterExcluded(service: Service): Service? {
      val serviceRestriction = restrictions.exclusionFor(service.name.toTaxiQualifiedName())
         ?: return service
      return if (serviceRestriction.operations.isNotEmpty()) {
         Service(
            service.name,
            service.operations.filterNot { serviceRestriction.hasOperation(it.name) },
            service.queryOperations.filterNot { serviceRestriction.hasOperation(it.name) },
            service.streamOperations.filterNot { serviceRestriction.hasOperation(it.name) },
            service.tableOperations.filterNot { serviceRestriction.hasOperation(it.name) },
            service.metadata,
            service.sourceCode,
            service.typeDoc,
            service.lineage,
            service.serviceKind
         )
      } else {
         // The entire service is excluded
         null
      }
   }

}

fun ServiceRestriction.hasOperation(operationName: String): Boolean {
   return this.operations.any { it.name == operationName }
}
