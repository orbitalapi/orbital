package com.orbitalhq.cockpit.core.schemas.editor.generator

import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.PartialOperation
import com.orbitalhq.schemas.PartialParameter
import com.orbitalhq.schemas.PartialQueryOperation
import com.orbitalhq.schemas.PartialSchema
import com.orbitalhq.schemas.PartialService
import com.orbitalhq.schemas.PartialType
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.TaxiDocument
import lang.taxi.generators.Logger
import lang.taxi.services.Operation
import lang.taxi.services.Parameter
import lang.taxi.services.QueryOperation
import lang.taxi.services.Service
import lang.taxi.services.Table
import lang.taxi.types.Annotation
import lang.taxi.types.AnnotationType
import lang.taxi.types.Arrays
import lang.taxi.types.CompilationUnit
import lang.taxi.types.EnumDefinition
import lang.taxi.types.EnumType
import lang.taxi.types.EnumValue
import lang.taxi.types.Field
import lang.taxi.types.FieldModifier
import lang.taxi.types.Modifier
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import lang.taxi.types.PrimitiveType
import lang.taxi.types.StreamType
import lang.taxi.types.Type
import lang.taxi.types.TypeKind

/**
 * This class is only used where we're sendng edits from the UI
 * to convert and write somehwere.
 * While we have a couple of edit-action specific endpoints, it starts
 * getting complex to have an endpoint for every edit operation, and we
 * end up generating taxi all over the place.
 * Given an edit process often requires parsing one schema (swagger / sql)
 * to taxi, editing it along the way, it becomes easier to edit the model
 * directly
 */
class VyneSchemaToTaxiSchemaMapper(
   private val schema: PartialSchema,
   /**
    * A schema of types that will not be generated, but will result in imports.
    */
   private val referenceSchema: TaxiSchema = TaxiSchema.empty(),
   private val logger: Logger
) {
   private val _generatedTypes = mutableMapOf<QualifiedName, Type>()

   fun generate(): List<TaxiDocument> {
      val taxiTypes = schema.types
         .filter { type -> !type.isPrimitive && type.name.namespace != "lang.taxi" }
         .map { type -> getOrCreateType(type.name) }
      val services = schema.services
         .map { generateService(it) }

      val typesByNameSpace =  taxiTypes.groupBy { it.toQualifiedName().namespace }
      val servicesByNameSpace = services.groupBy { it.toQualifiedName().namespace }
      val uniqueNameSpaces = typesByNameSpace.keys + servicesByNameSpace.keys
      return  uniqueNameSpaces.map { namespace ->
         val typesForNamespace = typesByNameSpace[namespace]?.toSet() ?: setOf()
         val servicesForNamespace = servicesByNameSpace[namespace]?.toSet() ?: setOf()
         TaxiDocument(typesForNamespace, servicesForNamespace)
      }
   }

   private fun generateService(source: PartialService): Service {
      val members = source.operations.map { generateOperation(it) } +
         source.queryOperations.map { generateQueryOperation(it) } +
         source.tableOperations.map { generateTableOperation(it) }
      return Service(
         source.name.fullyQualifiedName,
         members,
         convertAnnotations(source.metadata),
         listOf(CompilationUnit.Companion.generatedFor(source.name.fullyQualifiedName)),
         source.typeDoc
      )
   }

   private fun generateTableOperation(source: PartialOperation): Table {
      return Table(
         name = OperationNames.operationName(source.qualifiedName),
         annotations = convertAnnotations(source.metadata),
         returnType = getOrCreateType(source.returnTypeName),
         compilationUnits = listOf(CompilationUnit.Companion.generatedFor(source.qualifiedName.longDisplayName)),
         typeDoc = source.typeDoc
      )
   }

   private fun generateOperation(source: PartialOperation): Operation {
      return Operation(
         OperationNames.operationName(source.qualifiedName),
         source.operationType,
         convertAnnotations(source.metadata),
         source.parameters.map { sourceParam -> convertParameter(sourceParam) },
         getOrCreateType(source.returnTypeName),
         listOf(CompilationUnit.Companion.generatedFor(source.qualifiedName.longDisplayName)),
         null,
         source.typeDoc
      )
   }

   private fun convertParameter(
      sourceParam: PartialParameter
   ) = Parameter(
      convertAnnotations(sourceParam.metadata),
      getOrCreateType(sourceParam.typeName),
      sourceParam.name,
      emptyList(), // TODO : Contraints
   )

   private fun generateQueryOperation(source: PartialQueryOperation): QueryOperation {
      return QueryOperation(
         OperationNames.operationName(source.qualifiedName),
         convertAnnotations(source.metadata),
         source.parameters.map { convertParameter(it) },
         source.grammar,
         getOrCreateType(source.returnTypeName),
         listOf(CompilationUnit.Companion.generatedFor(source.qualifiedName.longDisplayName)),
         source.capabilities,
         source.typeDoc
      )
   }

   private fun getOrCreateType(name: QualifiedName): Type {
      if (Arrays.isArray(name.parameterizedName)) {
         val arrayMemberType = getOrCreateType(name.parameters[0])
         return Arrays.arrayOf(arrayMemberType)
      }
      if (StreamType.isStream(name.parameterizedName)) {
         val streamMemberType = getOrCreateType(name.parameters[0])
         return StreamType.of(streamMemberType)
      }
      if (referenceSchema.hasType(name.parameterizedName)) {
         return referenceSchema.taxiType(name)
      }
      return _generatedTypes.getOrPut(name) {
         // This allows us to support recursion - the undefined type will prevent us getting into an endless loop
         _generatedTypes[name] = ObjectType.undefined(name.fullyQualifiedName)
         createTaxiType(schema.type(name))
      }
   }

   private fun createTaxiType(type: PartialType): Type {
      return when {
         type.isPrimitive -> getPrimitiveType(type.fullyQualifiedName)
         type.isEnum -> createEnumType(type)
         type.isCollection -> createCollectionType(type)
         type.isScalar -> createTypeOrModel(type)
         type.attributes.isNotEmpty() -> createTypeOrModel(type)
         else -> error("Unhandled taxi type creation encountered for type ${type.fullyQualifiedName}")
      }
   }

   private fun getPrimitiveType(name: String) = PrimitiveType.fromDeclaration(name)

   private fun createEnumType(type: PartialType): Type {
      return EnumType(
         type.fullyQualifiedName,
         EnumDefinition(
            type.enumValues.map { sourceEnumValue ->
               EnumValue(
                  sourceEnumValue.name,
                  sourceEnumValue.value,
                  EnumValue.enumValueQualifiedName(
                     lang.taxi.types.QualifiedName.from(type.name.fullyQualifiedName),
                     sourceEnumValue.name
                  ),
                  emptyList(), // todo : annotations (not present on vyne type)
                  sourceEnumValue.synonyms,
                  sourceEnumValue.typeDoc,
                  // todo : isDefault (not present on vyne type)
               )
            },
            convertAnnotations(type.metadata),
            CompilationUnit.generatedFor(type.fullyQualifiedName),
            type.inheritsFromTypeNames.map { getOrCreateType(it) }.toSet(),
            getPrimitiveType(type.basePrimitiveTypeName!!.fullyQualifiedName),
            false, // todo - isLenient
            type.typeDoc
         )
      )
   }

   private fun createCollectionType(type: PartialType): Type {
      TODO("Building partial types not yet implemented")

   }

   private fun createTypeOrModel(type: PartialType): Type {
      val fields = type.attributes.map { (name, field) ->
         Field(
            name,
            getOrCreateType(field.type),
            field.nullable,
            convertModifiers(field.modifiers),
            convertAnnotations(field.metadata),
            if (field.constraints.isNotEmpty()) {
               error("Constraints on fields are not yet implemented. (Field $name on type ${type.fullyQualifiedName})")
            } else {
               emptyList()
            },
            if (field.accessor != null) {
               error("Fields with accessors are not yet supported  (Field $name on type ${type.fullyQualifiedName})")
            } else {
               null
            },
            if (field.readCondition != null) {
               error("Field with read conditions are not supported  (Field $name on type ${type.fullyQualifiedName})")
            } else {
               null
            },
            field.typeDoc,
//            field.defaultValue,
            null,
            null,
            field.format,
            CompilationUnit.generatedFor(type.fullyQualifiedName)
         )
      }
      return ObjectType(
         type.fullyQualifiedName,
         ObjectTypeDefinition(
            fields.toSet(),
            convertAnnotations(type.metadata).toSet(),
            type.modifiers
               .mapNotNull {
                  try {
                     Modifier.valueOf(it.name)
                  } catch (e: Exception) {
                     null
                  }
               },
            type.inheritsFromTypeNames.map { getOrCreateType(it) }.toSet(),
            type.formatAndZoneOffset,
            false,
            if (fields.isEmpty()) TypeKind.Type else TypeKind.Model,
            type.expression,
            type.typeDoc,
            CompilationUnit.Companion.generatedFor(type.fullyQualifiedName)
         )
      )
   }

   private fun convertModifiers(modifiers: List<com.orbitalhq.schemas.FieldModifier>): List<FieldModifier> {
      return modifiers.map { FieldModifier.valueOf(it.name) }
   }

   private fun convertAnnotations(metadatas: List<Metadata>): List<Annotation> {

      return metadatas.map { metadata ->
         // An annotation can be a real type, or just a name.
         // If the real type exists in either the schema we're generating, or the reference schema,
         // then use that.
         val annotationType = referenceSchema.taxi?.let { taxi ->
            // This isn
            if (taxi.containsType(metadata.name.fullyQualifiedName) || referenceSchema.hasType(metadata.name.fullyQualifiedName)) {
               getOrCreateType(metadata.name) as AnnotationType
            } else {
               null
            }
         }
         if (annotationType != null) {
            Annotation(annotationType, metadata.params)
         } else {
            Annotation(metadata.name.fullyQualifiedName, metadata.params)
         }
      }
   }
}

enum class GenerationAction {
   GENERATE,
   GENERATE_IMPORT_ONLY,
   IGNORE
}