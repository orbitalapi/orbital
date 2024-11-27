package com.orbitalhq.query.caching

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.types.SumType
import lang.taxi.types.UnionType
import mu.KotlinLogging

private typealias SourceType = Type
private typealias JoinFieldType = Type

class InvalidStateJoinTypesException(message: String) : RuntimeException(message)

/**
 * Calcualtes the id for a typed instance intended for storing in a state store.
 *
 * Ids are calculated based on:
 *  - Any fields that are present on both types, where one of the types has declared the field with an @Id annotation
 */
class StateStoreIdProvider(private val types: Set<Type>, private val schema: Schema) {
   constructor(sumType: SumType, schema: Schema) : this(sumType.types.map { schema.type(it) }.toSet(), schema)
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   val joinTypeReferences: Map<SourceType, Map<JoinFieldType, Pair<AttributeName, Field>>> =
      buildJoinTypeReferencesMap()

   private fun buildJoinTypeReferencesMap(): Map<SourceType, Map<JoinFieldType, Pair<AttributeName, Field>>> {
      // Start by finding fields with @Id annotations on each type
      val joinTypes = types.flatMap { type ->
         val fieldsWithId = type.getAttributesWithAnnotation("Id".fqn())
            .map { it.value.type }
            .distinct()
            .map { schema.type(it) }

         // These are the types of fields which are present on type being iterated,
         // and the field is also present the other types in the sum type.
         val idTypesPresentOnAllTypes = fieldsWithId
            .filter { idFieldType ->
               // This Id type can only be used as a join if all other types
               // in the sum type have a field of the same type
               types.all { it.hasAttributeWithType(idFieldType) }
            }

         idTypesPresentOnAllTypes
      }.distinct()

      val joinSourceTypesDescription = types.joinToString { it.qualifiedName.shortDisplayName }
      when {
         joinTypes.isEmpty() -> throw InvalidStateJoinTypesException("No valid joins available between the types of $joinSourceTypesDescription - No types contain an @Id annotated field that is also present on the other types")
         joinTypes.size > 1 -> throw InvalidStateJoinTypesException(
            "Ambiguous join found between types $joinSourceTypesDescription - The following fields could be used to to join on: ${
               joinTypes.joinToString { it.qualifiedName.shortDisplayName }
            }."
         )
      }

      val joinTypesDescription = joinTypes.joinToString { it.qualifiedName.shortDisplayName }

      // For each source type...
      val result = types.associateWith { type: SourceType ->
         //.. build a map that holds each join type, and it's associated field within the source type
         val joinTypesToFields: Map<JoinFieldType, Pair<AttributeName, Field>> = joinTypes.associateWith { joinType ->
            val attributes = type.findAttributeForType(joinType)
            val fieldAndName = when {
               attributes.isEmpty() -> throw InvalidStateJoinTypesException("Building a join between $joinSourceTypesDescription using types $joinTypesDescription expected to find type ${joinType.qualifiedName.shortDisplayName} on type ${type.qualifiedName.shortDisplayName} but it wasn't present")
               attributes.size > 1 -> throw InvalidStateJoinTypesException("Building a join between $joinSourceTypesDescription using types $joinTypesDescription expected to find exactly one field with type ${joinType.qualifiedName.shortDisplayName} on type ${type.qualifiedName.shortDisplayName} but found ${attributes.size}")
               else -> attributes.single()
            }
            fieldAndName
         }
         joinTypesToFields
      }
      logger.debug { "Join between $joinSourceTypesDescription will use type(s) $joinTypesDescription" }
      return result

   }

   fun getIdFields(type: Type): Map<JoinFieldType, Pair<AttributeName, Field>> {
      return joinTypeReferences[type] ?: error("Type ${type.name.shortDisplayName} is not present in this join")

   }

   fun getIdFieldNames(type: Type): Map<JoinFieldType, AttributeName> {
      return getIdFields(type)
         .mapValues { (_,fields) -> fields.first }
   }

   fun calculateStateStoreId(instance: TypedInstance): String? {
      if (instance !is TypedObject) {
         logger.warn { "Attempted to calculate the state store id of a non-object instance : ${instance.type.paramaterizedName}" }
         return null
      }
      val fieldsByJoinFields = getIdFields(instance.type)
      if (fieldsByJoinFields.isEmpty()) {
         return null
      }
      val idValues = fieldsByJoinFields
         .values
         .toList()
         .sortedBy { it.first }
         .map { (attributeName, field) ->
            val fieldType = field.resolveType(schema)
            val attributeValue = instance.getAttributeIdentifiedByType(fieldType)
            attributeValue.value?.toString()?.let { "${fieldType.qualifiedName}:$it" }
         }.distinct()

      return if (idValues.any { it == null }) {
         null
      } else {
         idValues.joinToString("|") { it!! }
      }
   }
}
