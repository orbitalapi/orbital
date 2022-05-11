package io.vyne.schema.api

import io.vyne.schemas.*

/**
 * Iterates a schema, and given an entrypoint type, collects all the type references required to
 * generate a valid sub-set schema.
 *
 * Considering deprecating and removing this, purely because it's not used.
 */
@Deprecated("Is this really used?")
internal class MemberCollector(val schema: Schema, val includePrimitives: Boolean) {
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
