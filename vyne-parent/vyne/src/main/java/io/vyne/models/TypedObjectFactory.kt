package io.vyne.models

import io.vyne.models.conditional.ConditionalFieldSetEvaluator
import io.vyne.schemas.*
import lang.taxi.types.Accessor

class TypedObjectFactory(private val type: Type, private val value: Any, internal val schema: Schema) {
   private val valueReader = ValueReader()
   private val accessorReader = AccessorReader()
   private val conditionalFieldSetEvaluator = ConditionalFieldSetEvaluator(this)

   private val mappedAttributes: MutableMap<AttributeName, TypedInstance> = mutableMapOf()

   fun build(): TypedObject {
      // TODO : Naieve first pass.
      // This approach won't work for nested objects.
      // I think i need to build a hierachy of object factories, and allow nested access
      // via the get() method
      type.attributes.forEach { (attributeName, field) ->

         // The value may have already been populated on-demand from a conditional
         // field set evaluation block, prior to the iterator hitting the field
         getOrBuild(attributeName, field)
      }
      return TypedObject(type, mappedAttributes)
   }

   private fun getOrBuild(attributeName: AttributeName, field: Field): TypedInstance {
      return mappedAttributes.computeIfAbsent(attributeName) {
         buildField(field, attributeName)
      }
   }

   internal fun getValue(attributeName: AttributeName): TypedInstance {
      return getOrBuild(attributeName, type.attribute(attributeName))
   }

   internal fun readAccessor(type: Type, accessor: Accessor): TypedInstance {
      return accessorReader.read(value, type, accessor, schema)
   }

   internal fun readAccessor(type: TypeReference, accessor: Accessor): TypedInstance {
      return accessorReader.read(value, type, accessor, schema)
   }


   private fun buildField(field: Field, attributeName: AttributeName): TypedInstance {
      // Questionable design choice: Favour directly supplied values over accessors and conditions.
      // The idea here is that when we're reading from a file or non parsed source, we need
      // to know how to construct the instance.
      // However, if that work has already been done, and we're trying to rebuild the instance
      // from a parsing result, we need to be able to.
      // Therefore, if we've been directly supplied the value, use it.
      // Otherwise, look to leverage conditions.
      // Note - revisit if this proves to be problematic.
      return when {
         valueReader.contains(value, attributeName) -> {
            val attributeValue = valueReader.read(value, attributeName)
            if (attributeValue == null) {
               TypedNull(schema.type(field.type))
            } else {
               TypedInstance.from(schema.type(field.type.name), attributeValue, schema)
            }
         }
         field.accessor != null -> {
            readAccessor(field.type, field.accessor)

         }
         field.readCondition != null -> {
            conditionalFieldSetEvaluator.evaluate(field.readCondition, attributeName, schema.type(field.type))
         }
         else -> error("The supplied value did not contain an attribute of $attributeName and no accessors or strategies were found to read")
      }

   }

}
