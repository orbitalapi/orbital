package com.orbitalhq.avro

import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.generators.avro.AvroFieldAnnotation
import lang.taxi.generators.avro.AvroMessageAnnotation
import lang.taxi.generators.avro.AvroTypeMapper
import lang.taxi.types.EnumType
import lang.taxi.types.MapType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.isMapType

class AvroSchemaGenerator(private val schema: Schema) {
   fun generateAvroSchema(type: Type): org.apache.avro.Schema {
      return if (type.isCollection) {
         val memberSchema = generateAvroSchema(type.collectionType!!)
         org.apache.avro.Schema.createArray(memberSchema)
      } else {
         require(type.hasMetadata(AvroMessageAnnotation.NAME.fqn())) { "Type ${type.name.shortDisplayName} does not declare a ${AvroMessageAnnotation.NAME} annotation" }
         getSchemaForType(type, mutableMapOf())
      }
   }

   private fun getSchemaForType(
      type: Type,
      typeCache: MutableMap<Type, org.apache.avro.Schema>
   ): org.apache.avro.Schema {
      return when {
         type.isCollection -> getSchemaForArrayType(type, typeCache)
         type.isEnum -> getSchemaForEnumType(type, typeCache)
         type.taxiType.isMapType() -> getSchemaForMapType(type, typeCache)
         !type.isScalar -> getSchemaForObjectType(type, typeCache)
         else -> getTypeForScalarType(type, typeCache)
      }
   }

   private fun getTypeForScalarType(
      type: Type,
      typeCache: MutableMap<Type, org.apache.avro.Schema>
   ): org.apache.avro.Schema {
      return typeCache.getOrPut(type) {
         val primitiveType = PrimitiveType.getPrimitive(
            type.basePrimitiveTypeName?.parameterizedName
               ?: error("Type ${type.name.shortDisplayName} does not have a primitive base type")
         )
         val typeMapping = AvroTypeMapper.primitives.entries.firstOrNull {
            it.value == primitiveType
         } ?: error("Could not find a corresponding Avro type for primitive type ${primitiveType.name}")

         org.apache.avro.Schema.create(typeMapping.key)
      }

   }

   private fun getSchemaForMapType(
      type: Type,
      typeCache: MutableMap<Type, org.apache.avro.Schema>
   ): org.apache.avro.Schema {
      val valueType = schema.type((type.taxiType as MapType).valueType)
      val valueTypeSchema = getSchemaForType(valueType, typeCache)
      return org.apache.avro.Schema.createMap(valueTypeSchema)
   }

   private fun getSchemaForEnumType(
      type: Type,
      typeCache: MutableMap<Type, org.apache.avro.Schema>
   ): org.apache.avro.Schema {
      require(type.enumValues.all { it.value is Int }) { "Invalid Avro Enum type - ${type.name.shortDisplayName}. All enum values are expected to be Int ordinals, but found: ${type.enumValues.joinToString { it.value.toString() }}" }
      return typeCache.getOrPut(type) {
         val values = type.enumValues
            .sortedBy { it.value as Int }
            .map { it.name }
         val defaultValue = (type.taxiType as EnumType).defaultValue?.value?.toString()
         org.apache.avro.Schema.createEnum(type.name.name, type.typeDoc, type.name.namespace, values, defaultValue)
      }


   }

   private fun getSchemaForArrayType(
      type: Type,
      typeCache: MutableMap<Type, org.apache.avro.Schema>
   ): org.apache.avro.Schema {
      val memberSchema = getSchemaForType(type.collectionType!!, typeCache)
      return org.apache.avro.Schema.createArray(memberSchema)
   }

   private fun getSchemaForObjectType(
      type: Type,
      typeCache: MutableMap<Type, org.apache.avro.Schema>
   ): org.apache.avro.Schema {
      return typeCache.getOrPut(type) {
         val avroType = org.apache.avro.Schema.createRecord(
            type.name.name,
            type.typeDoc,
            type.name.namespace,
            false
         )

         typeCache.put(type, avroType)

         val fields = type.attributes
            .entries
            .sortedBy { (_, field) ->
               val fieldAnnotation = field.getMetadata(AvroFieldAnnotation.NAME.fqn())
               fieldAnnotation.params["ordinal"]!! as Int
            }
            .map { (fieldName, field) ->

               val declaredTypeSchema = getSchemaForType(schema.type(field.type), typeCache)
               val fieldSchema = if (field.nullable) {
                  org.apache.avro.Schema.createUnion(
                     listOf(
                        declaredTypeSchema,
                        org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL)
                     )
                  )
               } else {
                  declaredTypeSchema
               }

               org.apache.avro.Schema.Field(
                  fieldName,
                  fieldSchema,
                  field.typeDoc
//                TODO : Default Value
               )
            }
         avroType.fields = fields

         avroType
      }
   }

}
