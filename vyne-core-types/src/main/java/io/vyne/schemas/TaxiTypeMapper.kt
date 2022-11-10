package io.vyne.schemas

import io.vyne.schemas.taxi.FunctionConstraintProvider
import io.vyne.schemas.taxi.TaxiConstraintConverter
import io.vyne.schemas.taxi.toVyneFieldModifiers
import io.vyne.schemas.taxi.toVyneQualifiedName
import io.vyne.schemas.taxi.toVyneSources
import lang.taxi.accessors.FieldSourceAccessor
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.EnumType
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.TypeAlias

object TaxiTypeMapper {
   fun fromTaxiType(taxiType: lang.taxi.types.Type, schema: Schema, typeCache: TypeCache = schema.typeCache): Type {
      return when (taxiType) {
         is ObjectType -> {
            val typeName = QualifiedName(taxiType.qualifiedName)
            val fields = taxiType.allFields.map { field ->
               when (field.type) {
                  is ArrayType -> field.name to Field(
                     field.type.toVyneQualifiedName(),
                     field.modifiers.toVyneFieldModifiers(),
                     accessor = field.accessor,
                     readCondition = field.readExpression,
                     typeDoc = field.typeDoc,
                     nullable = field.nullable,
                     metadata = parseAnnotationsToMetadata(field.annotations),
                     fieldProjection = field.projection,
                     format = field.formatAndZoneOffset
                  )

                  else -> field.name to Field(
                     field.type.qualifiedName.fqn(),
                     constraintProvider = buildDeferredConstraintProvider(
                        field.type.qualifiedName.fqn(),
                        field.constraints,
                        schema
                     ),
                     modifiers = field.modifiers.toVyneFieldModifiers(),
                     accessor = field.accessor,
                     readCondition = field.readExpression,
                     typeDoc = field.typeDoc,
                     defaultValue = field.defaultValue,
//                     formula = field.formula,
                     nullable = field.nullable,
                     metadata = parseAnnotationsToMetadata(field.annotations),
                     sourcedBy = if (field.accessor is FieldSourceAccessor)
                        FieldSource(
                           (field.accessor as FieldSourceAccessor).sourceAttributeName,
                           field.type.qualifiedName.fqn(),
                           (field.accessor as FieldSourceAccessor).sourceType.toVyneQualifiedName()
                        )
                     else null,
                     fieldProjection = field.projection,
                     format = field.formatAndZoneOffset
                  )
               }
            }.toMap()
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
               QualifiedName(taxiType.qualifiedName),
               metadata = parseAnnotationsToMetadata(taxiType.annotations),
               aliasForTypeName = taxiType.aliasType!!.toQualifiedName().toVyneQualifiedName(),
               sources = taxiType.compilationUnits.toVyneSources(),
               typeDoc = taxiType.typeDoc,
               taxiType = taxiType,
               typeCache = typeCache
            )
         }

         is EnumType -> {
            val enumValues = taxiType.values.map { EnumValue(it.name, it.value, it.synonyms, it.typeDoc) }
            Type(
               QualifiedName(taxiType.qualifiedName),
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

         else -> Type(
            QualifiedName(taxiType.qualifiedName),
            modifiers = parseModifiers(taxiType),
            sources = taxiType.compilationUnits.toVyneSources(),
            taxiType = taxiType,
            typeDoc = null,
            typeCache = typeCache
         )
      }
   }

   private fun parseAnnotationsToMetadata(annotations: List<Annotation>): List<Metadata> {
      return annotations.map { Metadata(it.name.fqn(), it.parameters) }
   }

   private fun buildDeferredConstraintProvider(
      fqn: QualifiedName,
      constraints: List<lang.taxi.services.operations.constraints.Constraint>,
      schema: Schema
   ): DeferredConstraintProvider {
      val constraintConverter = TaxiConstraintConverter(schema)
      return FunctionConstraintProvider {
         val type = schema.type(fqn)
         constraintConverter.buildConstraints(type, constraints)
      }
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
