package com.orbitalhq.schemas

import com.orbitalhq.models.DefinedInSchema
import com.orbitalhq.models.TypedEnumValue
import com.orbitalhq.schemas.taxi.toVyneFieldModifiers
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.schemas.taxi.toVyneSources
import lang.taxi.accessors.FieldSourceAccessor
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.EnumType
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.TypeAlias
import lang.taxi.types.UnionType

object TaxiTypeMapper {
   private fun convertIfAnonymous(taxiType: lang.taxi.types.Type, schema: Schema, typeCache: TypeCache): Type? {
      return when {
         taxiType is ArrayType && (taxiType as ArrayType).memberType.anonymous -> {
            val memberType = fromTaxiType(taxiType.memberType, schema, typeCache)

            memberType.asArrayType()
         }

         taxiType.anonymous -> fromTaxiType(taxiType, schema, typeCache)
         else -> null
      }
   }

   private fun convertFields(fields: Collection<lang.taxi.types.Field>, schema: Schema, typeCache: TypeCache):Map<String,Field> {
      return fields.map { field ->
         val declaredAnonymousType = convertIfAnonymous(field.type, schema, typeCache)
         val accessorReturnType = field.accessor?.returnType?.let { convertIfAnonymous(it, schema, typeCache) }
         // HACK: This is a workaround.
         // Thins like this shouldn't be permitted, but currently are:
         //  as {
         //          //   starring : Person = first(Person[]) as { // <--- This is wrong, as the actual anonymous type is NOT a person.
         //          //     starsName : PersonName
         //          //  }
         // So, if there's an accessor return type, use that.
         val fieldAnonymousType = accessorReturnType ?: declaredAnonymousType
         val fieldTypeName = fieldAnonymousType?.qualifiedName ?: field.type.toVyneQualifiedName()
         when (field.type) {
            is ArrayType -> field.name to Field(
               type = fieldTypeName,
               modifiers = field.modifiers.toVyneFieldModifiers(),
               accessor = field.accessor,
               readCondition = field.readExpression,
               typeDoc = field.typeDoc,
               nullable = field.nullable,
               metadata = parseAnnotationsToMetadata(field.annotations),
               fieldProjection = field.projection,
               format = field.formatAndZoneOffset,
               anonymousType = fieldAnonymousType,
               constraints = field.constraints,
            )

            else -> field.name to Field(
               fieldTypeName,
               modifiers = field.modifiers.toVyneFieldModifiers(),
               accessor = field.accessor,
               readCondition = field.readExpression,
               typeDoc = field.typeDoc,
//                     formula = field.formula,
               nullable = field.nullable,
               metadata = parseAnnotationsToMetadata(field.annotations),
               sourcedBy = if (field.accessor is FieldSourceAccessor)
                  FieldSource(
                     (field.accessor as FieldSourceAccessor).sourceAttributeName,
                     field.type.qualifiedName.fqn(),
                     (field.accessor as FieldSourceAccessor).sourceType.toVyneQualifiedName(),
                     fieldAnonymousType
                  )
               else null,
               fieldProjection = field.projection,
               format = field.formatAndZoneOffset,
               anonymousType = fieldAnonymousType,
               constraints = field.constraints
            )
         }
      }.toMap()
   }

   fun fromTaxiType(taxiType: lang.taxi.types.Type, schema: Schema, typeCache: TypeCache = schema.typeCache): Type {
      return when (taxiType) {
         is ObjectType -> {
            val typeName = QualifiedName.from(taxiType.qualifiedName)
            val fields = convertFields(taxiType.allFields, schema, typeCache)
            val modifiers = parseModifiers(taxiType)
            Type(
               typeName,
               fields,
               modifiers,
               inheritsFromTypeNames = taxiType.inheritsFromNames.map { it.fqn() },
               metadata = parseAnnotationsToMetadata(taxiType.annotations),
               sources = taxiType.compilationUnits.toVyneSources(),
               typeDoc = taxiType.typeDoc,
               taxiType = taxiType,
               typeCache = typeCache
            )
         }

         is TypeAlias -> {
            Type(
               QualifiedName.from(taxiType.qualifiedName),
               metadata = parseAnnotationsToMetadata(taxiType.annotations),
               aliasForTypeName = taxiType.aliasType!!.toQualifiedName().toVyneQualifiedName(),
               sources = taxiType.compilationUnits.toVyneSources(),
               typeDoc = taxiType.typeDoc,
               taxiType = taxiType,
               typeCache = typeCache
            )
         }

         is EnumType -> {
            val enumValues = taxiType.values.map { enumValue ->

               val typedEnumValueSupplier = if (enumValue.value is lang.taxi.types.TypedValue) {
                  require(taxiType.valueType != null) { "Expected type ${taxiType.qualifiedName} to declare a value type, as it has non-scalar values, but valueType was null"}
                  ObjectEnumValueSupplier(schema)
               } else {
                  ScalarEnumValueSupplier
               }
               EnumValue(enumValue.name, enumValue.value, enumValue.synonyms, enumValue.typeDoc, typedEnumValueSupplier)


            }
            Type(
               QualifiedName.from(taxiType.qualifiedName),
               modifiers = parseModifiers(taxiType),
               metadata = parseAnnotationsToMetadata(taxiType.annotations),
               enumValues = enumValues,
               sources = taxiType.compilationUnits.toVyneSources(),
               typeDoc = taxiType.typeDoc,
               taxiType = taxiType,
               typeCache = typeCache
            )
         }

         is ArrayType -> {
            val collectionType = typeCache.type(taxiType.parameters[0].qualifiedName)

            collectionType.asArrayType()
         }
         is UnionType -> {
            Type(
               taxiType.toVyneQualifiedName(),
               modifiers = parseModifiers(taxiType),
               sources = taxiType.compilationUnits.toVyneSources(),
               taxiType = taxiType,
               typeDoc = null,
               typeCache = typeCache,
               attributes = convertFields(taxiType.fields, schema, typeCache),
               typeParametersTypeNames = taxiType.types.map { it.toVyneQualifiedName() }
            )
         }

         else -> Type(
            taxiType.toVyneQualifiedName(),
            modifiers = parseModifiers(taxiType),
            sources = taxiType.compilationUnits.toVyneSources(),
            taxiType = taxiType,
            typeDoc = null,
            typeCache = typeCache,
            typeParametersTypeNames = taxiType.toVyneQualifiedName().parameters
         )
      }
   }

   private fun parseAnnotationsToMetadata(annotations: List<Annotation>): List<Metadata> {
      return annotations.map { Metadata(it.name.fqn(), it.parameters) }
   }

   fun parseModifiers(type: lang.taxi.types.Type): List<Modifier> {
      return when (type) {
         is EnumType -> listOf(Modifier.ENUM)
         is PrimitiveType -> listOf(Modifier.PRIMITIVE)
         is ObjectType -> type.modifiers.map {
            when (it) {
               lang.taxi.types.Modifier.CLOSED -> Modifier.CLOSED
               lang.taxi.types.Modifier.PARAMETER_TYPE -> Modifier.PARAMETER_TYPE
            }
         }

         else -> emptyList()
      }
   }
}
