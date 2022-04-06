package io.vyne.schema.api

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.schemas.Modifier
import io.vyne.schemas.Operation
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaMember
import io.vyne.schemas.Service
import io.vyne.schemas.SimpleSchema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema

/**
 * Exposes individual strings of schemas from somewhere (loaded from disk, generated from code).
 */
interface SchemaSource {
   fun schemaStrings(): List<String>
   fun schemaString(): String = schemaStrings().joinToString("\n")

}

interface VersionedSourceProvider {
   val versionedSources: List<VersionedSource>
   val parsedSources: List<ParsedSource>
}

/**
 * Responsible for exposing a Schema, based on multiple sources.
 *
 * There's tech debt here, as we used to think that we'd support multiple
 * independent schemas.
 *
 * However, we now handle combination during the Taxi compilation phase,
 * so there's only ever a single schema.  This idea has not been tidied up throughout
 * the code, so the List<Schema> vs Schema methods are still a mess.
 */
interface SchemaProvider {
   @Deprecated("there can be only one.")
   fun schemas(): List<Schema>
   fun schema(): Schema {
      return TaxiSchema.from(this.sources())
   }

   fun sources(): List<VersionedSource> {
      if (this.schemas().any { it !is TaxiSchema }) {
         // Use of non-taxi schemas is no longer supported, for the reasons outlined in
         // SchemaAggregator.
         // AS we move more aggressively towards type extensions, we need to simplify the
         // schema support.
         error("No longer supporting non TaxiSchema's.")
      }
      return schemas().map { it as TaxiSchema }.flatMap { it.sources }
   }

   /**
    * Returns a smaller schema only containing the requested members,
    * and their dependencies
    */
   fun schema(memberNames: List<String>, includePrimitives: Boolean = false): Schema {
      val qualifiedNames = memberNames.map { it.fqn() }
      return MemberCollector(schema(), includePrimitives).collect(qualifiedNames, mutableMapOf())
   }

   companion object {
      fun from(schema: Schema): SchemaProvider {
         return object : SchemaProvider {
            override fun schemas(): List<Schema> = listOf(schema)
         }
      }
   }
}

/**
 * Combines the responsibilities of exposing indvidual taxi source code to the system,
 * along with providing a schema, compiled of multiple sources
 *
 * A SchemaStore will then hold the state of all the individual sources (published by SchemaSourceProviders)
 * and ultimately combining these into a Schema.
 *
 * Depending on configuration, individual services may have both a SchemaSourceProvider (to expose
 * sources), and a SchemaProvider (to compile and validate the sources).
 * However, it's equally valid to defer compilation
 */
interface SchemaSourceProvider : SchemaProvider, SchemaSource

private class MemberCollector(val schema: Schema, val includePrimitives: Boolean) {
   fun collect(memberNames: List<QualifiedName>, collectedMembers: MutableMap<QualifiedName, SchemaMember>): Schema {
      append(memberNames, collectedMembers)

      val types = collectedMembers.values.filterIsInstance<Type>().toSet()
      val services = collectedMembers.values.filterIsInstance<Service>().toSet()
      return SimpleSchema(types, services, schema.typeCache)
   }

   private fun append(memberNames: List<QualifiedName>, collectedMembers: MutableMap<QualifiedName, SchemaMember>) {
      memberNames.forEach { memberName ->
         if (!collectedMembers.containsKey(memberName)) {
            when {
               schema.hasType(memberName.fullyQualifiedName) -> appendType(memberName, collectedMembers)
               OperationNames.isName(memberName) && schema.hasOperation(memberName) -> appendOperation(memberName, collectedMembers)
               schema.hasService(memberName.fullyQualifiedName) -> appendService(memberName, collectedMembers)
            }
         }
      }
   }

   private fun appendService(memberName: QualifiedName, collectedMembers: MutableMap<QualifiedName, SchemaMember>) {
      val service = schema.service(memberName.fullyQualifiedName)
      service.operations.forEach { operation ->
         appendOperation(operation.qualifiedName, collectedMembers)
      }
   }

   private fun appendOperation(memberName: QualifiedName, collectedMembers: MutableMap<QualifiedName, SchemaMember>) {
      val (service, operation) = schema.operation(memberName)
      if (collectedMembers.containsKey(service.name)) {
         appendOperationToService(collectedMembers, service, operation)
      } else {
         collectedMembers.put(service.name, service.copy(operations = listOf(operation)))
      }

      val operationReferencedTypes = operation.parameters.map { it.type } + operation.returnType

      append(operationReferencedTypes.map { it.name }, collectedMembers)
   }

   fun appendOperationToService(collectedMembers: MutableMap<QualifiedName, SchemaMember>, service: Service, operation: Operation) {
      val collectedService = collectedMembers[service.name] as Service
      if (collectedService.hasOperation(operation.name)) {
         // Nothing to do, we've already collected it.
      } else {
         val updatedService = collectedService.copy(operations = collectedService.operations + operation)
         collectedMembers[service.name] = updatedService
      }
   }

   private fun appendType(memberName: QualifiedName, collectedMembers: MutableMap<QualifiedName, SchemaMember>) {
      val type = schema.type(memberName)

      if (shouldInclude(type)) {
         collectedMembers[memberName] = type
         val typeReferences = collectTypeReferences(type)
         append(typeReferences, collectedMembers)
      }
   }

   /**
    * Only includus primitive types if they've been explicitly asked for.
    * Note - type aliases of primitives are always included.
    */
   private fun shouldInclude(type: Type): Boolean {
      val isPrimitive = type.modifiers.contains(Modifier.PRIMITIVE) && !type.isTypeAlias
      return if (isPrimitive) includePrimitives else true
   }

   private fun collectTypeReferences(type: io.vyne.schemas.Type): List<QualifiedName> {
      return type.inheritanceGraph.map { it.name } +
         type.attributes.values.map { it.type } +
         listOfNotNull(type.aliasForTypeName)
   }
}

data class ControlSchemaPollEvent(val poll: Boolean)
