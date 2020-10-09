package io.vyne.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.models.conditional.ConditionalFieldSetEvaluator
import io.vyne.models.conditional.ValueExpressionEvaluator
import io.vyne.models.functions.FunctionRegistry
import io.vyne.models.json.Jackson
import io.vyne.models.json.isJson
import io.vyne.schemas.*
import io.vyne.utils.log
import lang.taxi.types.Accessor
import lang.taxi.types.ColumnAccessor
import org.apache.commons.csv.CSVRecord


class TypedObjectFactory(private val type: Type, private val value: Any, internal val schema: Schema, val nullValues: Set<String> = emptySet(), val source: DataSource, private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper, private val functionRegistry: FunctionRegistry = FunctionRegistry.default) {
   private val valueReader = ValueReader()
   private val accessorReader: AccessorReader by lazy { AccessorReader(this, this.functionRegistry) }
   private val conditionalFieldSetEvaluator = ConditionalFieldSetEvaluator(this)
   private val valueExpressionEvaluator = ValueExpressionEvaluator(this)
   private val mappedAttributes: MutableMap<AttributeName, TypedInstance> = mutableMapOf()

   fun build(): TypedInstance {
      if (isJson(value)) {
         val map = objectMapper.readValue<Any>(value as String)
         return TypedInstance.from(type, map, schema, nullValues = nullValues, source = source)
      }

      if (type.isAbstract) {
         val baseTypeDiscriminatorField = type.discriminatorField
            ?: error("Type ${type.qualifiedName} is abstract, but no discriminator field is defined - this shouldn't happen")
         if (baseTypeDiscriminatorField !is VyneBaseTypeDiscriminatorField) error("Type ${type.qualifiedName} is abstract, so expected to find a VyneBaseTypeDiscriminatorField but found ${baseTypeDiscriminatorField::class.simpleName}")
         val discriminatorValue = getOrBuild(baseTypeDiscriminatorField.field.name!!, baseTypeDiscriminatorField.field)
         val matchingSubtype = schema.findSubtypesOf(type).firstOrNull { subType ->
            val subTypeDiscriminatorField = subType.discriminatorField as? VyneSubTypeDiscriminatorField
               ?: return@firstOrNull false
            valueExpressionEvaluator.expressionEvaluatesEqualTo(subTypeDiscriminatorField.expression, schema.type(subTypeDiscriminatorField.field.type), discriminatorValue)
         }
            ?: error("Unable to find a subtype of ${type.qualifiedName} which matches discriminator value of ${discriminatorValue.value}")
         return TypedInstance.from(matchingSubtype, value, schema, source = source)
      }

      // TODO : Naieve first pass.
      // This approach won't work for nested objects.
      // I think i need to build a hierachy of object factories, and allow nested access
      // via the get() method
      type.attributes.filter { it.value.formula == null }.forEach { (attributeName, field) ->

         // The value may have already been populated on-demand from a conditional
         // field set evaluation block, prior to the iterator hitting the field
         getOrBuild(attributeName, field)
      }
      return TypedObject(type, mappedAttributes, source)
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
      return accessorReader.read(value, type, accessor, schema, source = source)
   }

   internal fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean): TypedInstance {
      return accessorReader.read(value, type, accessor, schema, nullValues, source = source, nullable = nullable)
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
         // Cheaper readers first
         value is CSVRecord && field.accessor is ColumnAccessor -> {
            readAccessor(field.type, field.accessor, field.nullable)
         }

         // ValueReader can be expensive if the value is an object,
         // so only use the valueReader early if the value is a map
         value is Map<*, *> && valueReader.contains(value, attributeName) -> readWithValueReader(attributeName, field)
         field.accessor != null -> {
            readAccessor(field.type, field.accessor, field.nullable)
         }
         field.readCondition != null -> {
            conditionalFieldSetEvaluator.evaluate(field.readCondition, attributeName, schema.type(field.type))
         }
         // Not a map, so could be an object, try the value reader - but this is an expensive
         // call, so we defer to last-ish
         valueReader.contains(value, attributeName) -> readWithValueReader(attributeName, field)

         else -> {
            log().error("The supplied value did not contain an attribute of $attributeName and no accessors or strategies were found to read.  Will return null")
            TypedNull(schema.type(field.type))
         }
      }

   }

   private fun readWithValueReader(attributeName: AttributeName, field: Field): TypedInstance {
      val attributeValue = valueReader.read(value, attributeName)
      return if (attributeValue == null) {
         TypedNull(schema.type(field.type), source)
      } else {
         TypedInstance.from(schema.type(field.type.fullyQualifiedName), attributeValue, schema, true, source = source)
      }
   }

}
