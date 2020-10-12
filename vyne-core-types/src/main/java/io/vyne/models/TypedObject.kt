package io.vyne.models

import io.vyne.schemas.AttributeName
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.toVyneQualifiedName
import lang.taxi.Equality
import lang.taxi.services.operations.constraints.PropertyFieldNameIdentifier
import lang.taxi.services.operations.constraints.PropertyIdentifier
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.AttributePath


data class TypedObject(
   override val type: Type,
   private val suppliedValue: Map<String, TypedInstance>,
   override val source: DataSource) : TypedInstance, Map<String, TypedInstance>  {

   private val combinedValues: Map<String, TypedInstance> = type.defaultValues?.plus(suppliedValue) ?: suppliedValue

   override val value: Map<String, TypedInstance>
      get() = combinedValues

   private val equality = Equality(this, TypedObject::type, TypedObject::value)
   companion object {
      fun fromValue(typeName: String, value: Any, schema: Schema, source:DataSource): TypedInstance {
         return fromValue(schema.type(typeName), value, schema, source = source)
      }

      fun fromAttributes(typeName: String, attributes: Map<String, Any>, schema: Schema, performTypeConversions: Boolean = true, source:DataSource): TypedObject {
         return fromAttributes(schema.type(typeName), attributes, schema, performTypeConversions, source)
      }

      fun fromAttributes(type: Type, attributes: Map<String, Any>, schema: Schema, performTypeConversions: Boolean = true, source:DataSource): TypedObject {
         val typedAttributes: Map<String, TypedInstance> = attributes
            .filterKeys { type.hasAttribute(it) }
            .map { (attributeName, value) ->
            val attributeType = schema.type(type.attributes.getValue(attributeName).type)
            attributeName to TypedInstance.from(attributeType, value, schema, performTypeConversions, source = source)
         }.toMap()
         return TypedObject(type, typedAttributes, source)
      }

      fun fromValue(type: Type, value: Any, schema: Schema, nullValues: Set<String> = emptySet(), source:DataSource): TypedInstance {
         return TypedObjectFactory(type, value, schema, nullValues, source).build()
      }
   }

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()

   fun hasAttribute(name: String): Boolean {
      return this.combinedValues.containsKey(name)
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedObject(typeAlias, combinedValues, source)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedObject) {
         return false
      }
      if (!this.type.resolvesSameAs(valueToCompare.type)) {
         return false
      }
      return this.combinedValues.all { (attributeName, value) ->
         valueToCompare.hasAttribute(attributeName) && valueToCompare.get(attributeName).valueEquals(value)
      }
   }

   /**
    * Returns the attribute value identified by the propertyIdentifer
    */
   fun getAttribute(propertyIdentifier: PropertyIdentifier, schema: Schema): TypedInstance {
      return when (propertyIdentifier) {
         is PropertyFieldNameIdentifier -> get(propertyIdentifier.name)
         is PropertyTypeIdentifier -> getAttributeIdentifiedByType(propertyIdentifier.type.toVyneQualifiedName(), schema)
      }
   }

   private fun getAttributeIdentifiedByType(typeName: QualifiedName, schema: Schema): TypedInstance {
      return getAttributeIdentifiedByType(schema.type(typeName))
   }

   fun getAttributeIdentifiedByType(type: Type, returnNull: Boolean = false): TypedInstance {
      val candidates = this.value.filter { (_, value) -> value.type.isAssignableTo(type) }
      return when {
         candidates.isEmpty() && returnNull -> TypedNull(type) // sometimes i want to allow null values
         candidates.isEmpty() -> error("No properties on type ${this.type.name.parameterizedName} have type ${type.name.parameterizedName}")
         candidates.size > 1 -> TypedInstanceCandidateFilter.resolve(candidates.values, type)
         else -> candidates.values.first()
      }
   }

   operator fun get(path: AttributePath): TypedInstance {
      return get(path.path)
   }

   // TODO : Needs a test
   override operator fun get(key: String): TypedInstance {
      val parts = key.split(".").toMutableList()
      val thisFieldName = parts.removeAt(0)
      val attributeValue = this.value[thisFieldName]
         ?: error("No attribute named $thisFieldName found on this type (${type.name})")

      return if (parts.isEmpty()) {
         attributeValue
      } else {
         val remainingAccessor = parts.joinToString(".")
         if (attributeValue is TypedObject) {
            attributeValue[remainingAccessor]
         } else {
            throw IllegalArgumentException("Cannot evaluate an accessor ($remainingAccessor) as value is not an object with fields (${attributeValue.type.name})")
         }
      }
   }

   fun copy(replacingArgs: Map<AttributeName, TypedInstance>): TypedObject {
      return TypedObject(this.type, this.value + replacingArgs, source)
   }

   override val entries: Set<Map.Entry<String, TypedInstance>>
      get() = combinedValues.entries
   override val keys: Set<String>
      get() = combinedValues.keys
   override val size: Int
      get() = combinedValues.size
   override val values: Collection<TypedInstance>
      get() = combinedValues.values

   override fun containsKey(key: String): Boolean {
      return combinedValues.containsKey(key)
   }

   override fun containsValue(value: TypedInstance): Boolean {
      return combinedValues.containsValue(value)
   }

   override fun isEmpty(): Boolean {
      return combinedValues.isEmpty()
   }
}
