package io.vyne.schemaStore

import io.vyne.CompositeSchemaBuilder
import io.vyne.VersionedSource
import io.vyne.schemas.*
import io.vyne.schemas.taxi.TaxiSchema

interface SchemaSource {
   fun schemaStrings(): List<String>
   fun schemaString(): String = schemaStrings().joinToString("\n")

}

interface VersionedSourceProvider {
   val versionedSources: List<VersionedSource>
}

interface SchemaProvider {
   fun schemas(): List<Schema>
   // TODO : May want to deprecate this approach, and the whole concept of schema aggregators.
   // See SchemaAggregator for an explanation.
   fun schema(): Schema {
      if (this.schemas().any { it !is TaxiSchema }) {
         // Use of non-taxi schemas is no longer supported, for the reasons outlined in
         // SchemaAggregator.
         // AS we move more aggressively towards type extensions, we need to simplify the
         // schema support.
         error("No longer supporting non TaxiSchema's.")
      }
      val taxiSchemas = schemas().map { it as TaxiSchema }
      return TaxiSchema.from(taxiSchemas.flatMap { it.sources })
   }
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

interface SchemaSourceProvider : SchemaProvider, SchemaSource

private class MemberCollector(val schema: Schema, val includePrimitives: Boolean) {
   fun collect(memberNames: List<QualifiedName>, collectedMembers: MutableMap<QualifiedName, SchemaMember>): Schema {
      append(memberNames, collectedMembers)

      val types = collectedMembers.values.filterIsInstance<Type>().toSet()
      val services = collectedMembers.values.filterIsInstance<Service>().toSet()
      return SimpleSchema(types, services)
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
         type.attributes.values.map { it.type.name } +
         listOfNotNull(type.aliasForType)
   }
}
