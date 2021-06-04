package io.vyne.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import io.vyne.models.json.isJson
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.Equality
import lang.taxi.types.ArrayType


interface TypedInstance {
   @get:JsonIgnore
   val type: Type
   val value: Any?

   @get:JsonView(DataSourceIncludedView::class)
   val source: DataSource

   /**
    * Hash code which includes the datasource - normally excluded.
    * Used in determining rowIds for TypedInstances when persisting to the UI.
    */
   val hashCodeWithDataSource: Int
      get() {
         return Equality(this, TypedInstance::typeName, TypedInstance::value, TypedInstance::source).hash()
      }

   val typeName: String
      get() {
         return type.name.parameterizedName
      }

   // It's up to instances of this to reconstruct themselves with their type
   // set to the value of the typeAlias.
   fun withTypeAlias(typeAlias: Type): TypedInstance

   fun toRawObject(): Any? {
      return TypedInstanceConverter(RawObjectMapper).convert(this)
//      return convert { it.value }
   }

   fun toTypeNamedInstance(): Any? {
      return TypedInstanceConverter(TypeNamedInstanceMapper).convert(this)
   }

   fun valueEquals(valueToCompare: TypedInstance): Boolean

   companion object {
      fun fromNamedType(
         typeNamedInstance: TypeNamedInstance,
         schema: Schema,
         performTypeConversions: Boolean = true,
         source: DataSource
      ): TypedInstance {
         val (typeName, value) = typeNamedInstance
         val type = schema.type(typeName)
         return when {
            value == null -> TypedNull.create(type)
            value is Collection<*> -> {
               val collectionMemberType = getCollectionType(type)
               val members = value.map { member ->
                  if (member == null) {
                     TypedNull.create(collectionMemberType)
                  } else {
                     fromNamedType(member as TypeNamedInstance, schema, performTypeConversions, source)
                  }
               }
               TypedCollection(collectionMemberType, members)
            }
            type.isEnum -> type.enumTypedInstance(value, source)
            type.isScalar -> TypedValue.from(type, value, performTypeConversions, source)
            else -> createTypedObject(typeNamedInstance, schema, performTypeConversions, source)
         }
      }

      private fun createTypedObject(
         typeNamedInstance: TypeNamedInstance,
         schema: Schema,
         performTypeConversions: Boolean,
         source: DataSource
      ): TypedObject {
         val type = schema.type(typeNamedInstance.typeName)
         val attributes = typeNamedInstance.value!! as Map<String, Any>
         val typedAttributes = attributes.map { (attributeName, typedInstance) ->
            when (typedInstance) {
               is TypeNamedInstance -> attributeName to fromNamedType(
                  typedInstance,
                  schema,
                  performTypeConversions,
                  source
               )
               is Collection<*> -> {
                  val collectionTypeRef = type.attributes[attributeName]?.type
                     ?: error("Cannot look up collection type for attribute $attributeName as it is not a defined attribute on type ${type.name}")
                  val collectionType = schema.type(collectionTypeRef)
                  attributeName to TypedCollection(
                     collectionType,
                     typedInstance.map {
                        fromNamedType(
                           it as TypeNamedInstance,
                           schema,
                           performTypeConversions,
                           source
                        )
                     })
               }
               else -> error("Unhandled scenario creating typedObject from TypeNamedInstance -> ${typedInstance::class.simpleName}")
            }
         }.toMap()
         return TypedObject(type, typedAttributes, source)
      }

      /**
       * Parses a TypedInstance
       *
       * @param evaluateAccessors Determines if accessors defined in the schema should be evaluated.  Normally
       * this should be true.  However, for content served from a cask, the content is already preparsed, and so
       * does not need accessors to be evaluated.
       */
      fun from(
         type: Type,
         value: Any?,
         schema: Schema,
         performTypeConversions: Boolean = true,
         nullValues: Set<String> = emptySet(),
         source: DataSource = UndefinedSource,
         evaluateAccessors: Boolean = true
      ): TypedInstance {
         return when {
            value is TypedInstance -> value
            value == null -> TypedNull.create(type)
            value is Collection<*> -> {
               val collectionMemberType = getCollectionType(type)
               TypedCollection.arrayOf(
                  collectionMemberType,
                  value.filterNotNull().map {
                     from(
                        collectionMemberType,
                        it,
                        schema,
                        performTypeConversions,
                        source = source,
                        evaluateAccessors = evaluateAccessors
                     )
                  })
            }
            type.isEnum -> {
               type.enumTypedInstance(value, source)
            }
            type.isScalar -> {
               TypedValue.from(type, value, performTypeConversions, source)
            }
            // This is here primarily for readability.  We could just let this fall through to below.
            isJson(value) -> TypedObjectFactory(
               type,
               value,
               schema,
               nullValues,
               source,
               evaluateAccessors = evaluateAccessors
            ).build()

            // This is a bit special...value isn't a collection, but the type is.  Oooo!
            // Must be a CSV ish type value.
            type.isCollection -> readCollectionTypeFromNonCollectionValue(type, value, schema, source)
            else -> TypedObject.fromValue(
               type,
               value,
               schema,
               nullValues,
               source = source,
               evaluateAccessors = evaluateAccessors
            )
         }
      }

      private fun readCollectionTypeFromNonCollectionValue(
         type: Type,
         value: Any,
         schema: Schema,
         source: DataSource
      ): TypedInstance {
         return CollectionReader.readCollectionFromNonTypedCollectionValue(type, value, schema, source)
      }

      private fun getCollectionType(type: Type): Type {
         return if (type.isCollection) {
            type.collectionType ?: error("Type should return a collection when isCollection is true.")
         } else {
            log().warn("Collection type could not be determined - expected to find ${ArrayType.qualifiedName}, but found ${type.fullyQualifiedName}")
            type
         }
      }

      /**
       * Indciates if the instance, or it's value, is null or a TypedNull
       */
      fun isNull(instance: TypedInstance?): Boolean {
         return instance?.value == null || instance.value is TypedNull
      }
   }
}
