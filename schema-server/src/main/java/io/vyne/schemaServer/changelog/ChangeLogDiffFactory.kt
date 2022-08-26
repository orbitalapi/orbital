package io.vyne.schemaServer.changelog

import com.google.common.collect.Collections2
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import io.vyne.schemas.*

class ChangeLogDiffFactory(
   private val namespacesToExclude: List<String> = DEFAULT_EXCLUDED_NAMESPACES
) {

   companion object {
      val DEFAULT_EXCLUDED_NAMESPACES = listOf("lang.taxi", "io.vyne")
   }

   fun buildDiffs(oldSchema: Schema, schema: Schema): List<ChangeLogDiffEntry> {
      val typeDiffs = diffSchemaMembers(
         oldSchema.types,
         schema.types,
         this::createTypeAddedEntry,
         this::createTypeRemovedEntry,
         this::createTypeChanged
      )
      val serviceDiffs = diffSchemaMembers(
         oldMembers = oldSchema.services, newMembers = schema.services,
         addedFactory = this::createServiceAdded,
         removedFactory = this::createServiceRemoved,
         changedFactory = this::createServiceChanged
      )

      return typeDiffs + serviceDiffs
   }


   private fun <T : SchemaMember> diffSchemaMembers(
      oldMembers: Set<T>,
      newMembers: Set<T>,
      addedFactory: (T) -> List<ChangeLogDiffEntry>,
      removedFactory: (T) -> List<ChangeLogDiffEntry>,
      changedFactory: (T, T) -> List<ChangeLogDiffEntry>
   ): List<ChangeLogDiffEntry> {
      if (oldMembers == newMembers) {
         return emptyList()
      }
      val typesAdded = newMembers.membersNotPresentByNameIn(oldMembers)
         .excludingInternalMembers()
         .flatMap { addedFactory(it) }
      val typesRemoved = oldMembers.membersNotPresentByNameIn(newMembers)
         .excludingInternalMembers()
         .flatMap { removedFactory(it) }
      val typesChanged = newMembers
         .excludingInternalMembers()
         .mapNotNull { newType ->
            val oldType = oldMembers.findByName(newType)
            if (oldType != null && oldType != newType) {
               oldType to newType
            } else {
               null
            }
         }.flatMap { (oldType, newType) -> changedFactory(oldType, newType) }

      return typesAdded + typesRemoved + typesChanged
   }


   /**
    * Currently, this only evaluates fields added / removed.
    * We could be richer.
    * Possible enhancements:
    *  * Docs changed
    *  * Metadata added / removed to type
    *  * Metadata added / removed to fields
    */
   private fun createTypeChanged(oldType: Type, newType: Type): List<ChangeLogDiffEntry> {
      val fieldDifferences = Maps.difference(oldType.attributes, newType.attributes)
      fun createDiffEntries(entries: Map<AttributeName, Field>, diffKind: DiffKind): List<ChangeLogDiffEntry> {
         return entries.map { (fieldName, field) ->
            ChangeLogDiffEntry(
               fieldName,
               diffKind,
               newType.name,
               emptyList()
            )
         }
      }

      val fieldsAdded = createDiffEntries(fieldDifferences.entriesOnlyOnRight(), DiffKind.FieldAddedToModel)
      val fieldsRemoved = createDiffEntries(fieldDifferences.entriesOnlyOnLeft(), DiffKind.FieldRemovedFromModel)
      return listOf(
         ChangeLogDiffEntry(
            newType.name.shortDisplayName,
            DiffKind.ModelChanged,
            newType.name,
            fieldsAdded + fieldsRemoved
         )
      )
   }


   private fun createTypeRemovedEntry(type: Type): List<ChangeLogDiffEntry> {
      val diffKind = if (type.attributes.isEmpty()) {
         DiffKind.TypeRemoved
      } else {
         DiffKind.ModelRemoved
      }
      return listOf(
         ChangeLogDiffEntry(
            type.name.shortDisplayName,
            diffKind,
            type.name,
            emptyList()
         )
      )
   }

   private fun createTypeAddedEntry(type: Type): List<ChangeLogDiffEntry> {
      val members = type.attributes.map { (fieldName, field) ->
         ChangeLogDiffEntry(
            displayName = fieldName,
            kind = DiffKind.FieldAddedToModel,
            schemaMember = type.name, // Should we use the fields type name here?
            // We *could* also iterate into the fields of the type (for nested models).
            // That would require then filtering the types out to ensure they don't show twice.
            // Not sure what the best UX here will be.
            children = emptyList()
         )
      }
      val typeEntry = ChangeLogDiffEntry(
         displayName = type.name.shortDisplayName,
         kind = if (members.isEmpty()) {
            DiffKind.TypeAdded
         } else {
            DiffKind.ModelAdded
         },
         schemaMember = type.name,
         children = members
      )
      return listOf(typeEntry)

   }

   private fun createServiceAdded(service: Service): List<ChangeLogDiffEntry> {
      val operations = service.remoteOperations.map { remoteOperation ->
         ChangeLogDiffEntry(
            remoteOperation.qualifiedName.shortDisplayName,
            DiffKind.OperationAdded,
            service.name,
         )
      }
      return listOf(
         ChangeLogDiffEntry(
            service.name.shortDisplayName,
            DiffKind.ServiceAdded,
            service.name,
            operations
         )
      )
   }

   private fun createServiceRemoved(service: Service): List<ChangeLogDiffEntry> {
      return listOf(
         ChangeLogDiffEntry(
            service.name.shortDisplayName,
            DiffKind.ServiceRemoved,
            service.name
         )
      )
   }

   private fun createServiceChanged(oldService: Service, newService: Service): List<ChangeLogDiffEntry> {
      // Google's Maps.difference() is pretty rich, but the Sets.difference() isn't as easy to use., so create a map
      // Also, compare operations for existance only on names.  We'll do equality checks on actual operations elsewhere.
      // Otherwise an operation that is changed shows as added+removed
      val oldOperations = oldService.remoteOperations.map { it.qualifiedName }.associateWith { it }
      val newOperations = newService.remoteOperations.map { it.qualifiedName }.associateWith { it }
      val operationDifferences = Maps.difference(oldOperations, newOperations)
      fun createDiffEntries(entries: Collection<RemoteOperation>, diffKind: DiffKind): List<ChangeLogDiffEntry> {
         return entries.map { remoteOperation ->
            ChangeLogDiffEntry(
               remoteOperation.qualifiedName.shortDisplayName,
               diffKind,
               remoteOperation.qualifiedName,
               emptyList()
            )
         }
      }

      val operationsAdded = operationDifferences.entriesOnlyOnRight()
         .keys
         .map { newService.remoteOperation(OperationNames.operationName(it)) }
      val operationsAddedDiffs = createDiffEntries(operationsAdded, DiffKind.OperationAdded)

      val operationsRemoved = operationDifferences.entriesOnlyOnLeft()
         .keys
         .map { oldService.remoteOperation(OperationNames.operationName(it)) }
      val operationsRemovedDiffs = createDiffEntries(operationsRemoved, DiffKind.OperationRemoved)

      val operationsChangedDiffs = compareOperationsPresentOnBoth(
         operationDifferences.entriesInCommon().keys,
         oldService.remoteOperations,
         newService.remoteOperations
      )

      return listOf(
         ChangeLogDiffEntry(
            newService.name.shortDisplayName,
            DiffKind.ServiceChanged,
            newService.name,
            operationsAddedDiffs + operationsRemovedDiffs + operationsChangedDiffs
         )
      )
   }

   private fun compareOperationsPresentOnBoth(
      keys: MutableSet<QualifiedName>,
      oldOperations: List<RemoteOperation>,
      newOperations: List<RemoteOperation>
   ): List<ChangeLogDiffEntry> {
      if (keys.isEmpty()) {
         return emptyList()
      }
      return keys.flatMap { name ->
         val oldOperation = oldOperations.single { it.qualifiedName == name }
         val newOperation = newOperations.single { it.qualifiedName == name }
         compareOperation(oldOperation, newOperation)
      }
   }

   private fun compareOperation(
      oldOperation: RemoteOperation,
      newOperation: RemoteOperation
   ): List<ChangeLogDiffEntry> {
      return listOfNotNull(
         compareOperationReturnTypes(oldOperation, newOperation),
         compareOperationParameters(oldOperation, newOperation),
         compareOperationMetadata(oldOperation, newOperation)
      )
   }

   private fun compareOperationMetadata(
      oldOperation: RemoteOperation,
      newOperation: RemoteOperation
   ): ChangeLogDiffEntry? {
      return if (oldOperation.metadata != newOperation.metadata) {
         ChangeLogDiffEntry(
            newOperation.qualifiedName.shortDisplayName,
            DiffKind.OperationMetadataChanged,
            newOperation.qualifiedName,
            emptyList(),
            oldOperation.metadata,
            newOperation.metadata
         )
      } else null
   }

   data class ParameterDiff(val name: String?, val type: QualifiedName)

   private fun compareOperationParameters(
      oldOperation: RemoteOperation,
      newOperation: RemoteOperation
   ): ChangeLogDiffEntry? {

      return if (oldOperation.parameters != newOperation.parameters) {
         ChangeLogDiffEntry(
            newOperation.qualifiedName.shortDisplayName,
            DiffKind.OperationParametersChanged,
            newOperation.qualifiedName,
            emptyList(),
            oldDetails = oldOperation.parameters.map { ParameterDiff(it.name, it.typeName) },
            newDetails = newOperation.parameters.map { ParameterDiff(it.name, it.typeName) },
         )
      } else null
   }

   private fun compareOperationReturnTypes(
      oldOperation: RemoteOperation,
      newOperation: RemoteOperation
   ): ChangeLogDiffEntry? {
      return if (oldOperation.returnType != newOperation.returnType) {
         ChangeLogDiffEntry(
            newOperation.qualifiedName.shortDisplayName,
            DiffKind.OperationReturnValueChanged,
            newOperation.qualifiedName,
            emptyList(),
            oldDetails = oldOperation.returnType.name,
            newDetails = newOperation.returnType.name,
         )
      } else null
   }


   private fun <T : SchemaMember> Iterable<T>.findByName(other: T): T? {
      return this.firstOrNull { it.memberQualifiedName == other.memberQualifiedName }
   }


   private fun <T : SchemaMember> Set<T>.membersNotPresentByNameIn(other: Set<T>): Set<T> {
      val otherNames = other.map { it.memberQualifiedName }.toSet()
      return this.filter { thisMember -> !otherNames.contains(thisMember.memberQualifiedName) }.toSet()
   }

   private fun <T : SchemaMember> Set<T>.excludingInternalMembers(): Set<T> {
      return this.filter {
         !namespacesToExclude.contains(it.memberQualifiedName.namespace)
      }.toSet()
   }
}
