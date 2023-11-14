package com.orbitalhq.connections

import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.SchemaMemberReference

/**
 * Holds a list of places that a connection could be declared within metadata.
 * Instead of building a single uber-list here, it is expected that individual
 * annotation types register themselves
 */
object ConnectionUsageMetadataRegistry {
   private val registrations = mutableListOf<ConnectionUsageRegistration>()

   fun register(usage: ConnectionUsageRegistration) {
      registrations.add(usage)
   }

   /**
    * Searches a schema for all usages of a provided connectionName
    */
   fun findConnectionUsages(schema: Schema, connectionName: String): List<SchemaMemberReference> {
      val registeredMetadataNames = registrations.associateBy { it.annotationName }
      val schemaMembersAndMetadata = schema.types.map { it.schemaMemberReference to it.metadata } +
         schema.services.map { it.schemaMemberReference to it.metadata } +
         schema.operations.map { it.schemaMemberReference to it.metadata }

      return schemaMembersAndMetadata.filter { (schemaMemberReference,metadata) ->
         val metadataContainingConnectionNames = metadata.filter { registeredMetadataNames.containsKey(it.name) }
         val hasConnectionReference = metadataContainingConnectionNames.any { metadata: Metadata ->
            registrations.any { it.containsConnectionUsage(metadata, connectionName) }
         }
         hasConnectionReference
      }.map { it.first }
   }
}

data class ConnectionUsageRegistration(
   val annotationName: QualifiedName,
   val attributeName: String
) {
   fun containsConnectionUsage(annotation: Metadata, connectionName: String):Boolean {
      return annotation.params[attributeName] == connectionName
   }
}

