package io.vyne.query

import io.vyne.schemas.Field
import io.vyne.schemas.FieldSource
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.vyneql.AnonymousTypeDefinition
import io.vyne.vyneql.ComplexFieldDefinition
import io.vyne.vyneql.ProjectedType
import io.vyne.vyneql.SelfReferencedFieldDefinition
import io.vyne.vyneql.SimpleAnonymousFieldDefinition
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.sources.SourceCode
import lang.taxi.types.ArrayType
import lang.taxi.types.CompilationUnit
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import java.security.SecureRandom
import java.util.*


object ProjectionAnonymousTypeProvider {
   fun projectedTo(projectedType: ProjectedType, schema: Schema): Type {
      val anonymousTypeName = anonymousTypeName()
      return when {
         // Case for:
         // findAll { foo[] } as bar[]
         projectedType.concreteTypeName != null && projectedType.anonymousTypeDefinition == null -> schema.type(projectedType.concreteTypeName!!.toVyneQualifiedName())
         // Case for:
         // findAll { foo[] } as {
         // field1
         // field2
         // field3
         // field4: somenamespace.AnotherType
         // }[]
         projectedType.concreteTypeName == null && projectedType.anonymousTypeDefinition != null -> {
            val anonymousTypeDefinition = projectedType.anonymousTypeDefinition!!
            validateAnonymousTypeDefinition(anonymousTypeDefinition)
            val fieldDefinitions =
               simpleAnonymousFieldDefinitions(anonymousTypeDefinition)
                  .plus(selfReferencedAnonymousFieldDefinitions(anonymousTypeDefinition))
                  .plus(complexAnonymousFieldDefinitions(anonymousTypeDefinition, schema))
            anonymousType(fieldDefinitions, anonymousTypeName, schema, anonymousTypeDefinition.isList)
         }

         // Case for:
         // findAll { foo[] }
         // as bar[] {
         //    field1
         //    field2: mynamespace.mytype
         //}[]
         projectedType.concreteTypeName != null && projectedType.anonymousTypeDefinition != null -> {
            val anonymousTypeDefinition = projectedType.anonymousTypeDefinition!!
            validateAnonymousTypeDefinition(anonymousTypeDefinition)
            val concreteProjectionType = schema.type(projectedType.concreteTypeName!!.toVyneQualifiedName())
            val fieldDefinitions = concreteProjectionType
               .attributes
               .plus(simpleAnonymousFieldDefinitions(anonymousTypeDefinition))
               .plus(selfReferencedAnonymousFieldDefinitions(anonymousTypeDefinition, anonymousTypeName.fqn()))
               .plus(complexAnonymousFieldDefinitions(anonymousTypeDefinition, schema, anonymousTypeName.fqn()))
            anonymousType(fieldDefinitions, anonymousTypeName, schema, anonymousTypeDefinition.isList)
         }

         else -> throw CompilationException(CompilationError(0, 0, "Invalid Anonymous Projection Type."))
      }
   }

   private fun anonymousType(fieldDefinitions: Map<String, Field>, anonymousTypeName: String, schema: Schema, isList: Boolean): Type {
      val type = Type(
         anonymousTypeName.fqn(),
         fieldDefinitions,
         sources = listOf(),
         typeDoc = null,
         typeCache = schema.typeCache,
         taxiType = ObjectType(anonymousTypeName,
            ObjectTypeDefinition(compilationUnit = CompilationUnit(null, SourceCode("", ""))))
      )

      schema.typeCache.registerAnonymousType(type)
      return if (isList) {
         val arrayType = schema.type(ArrayType.NAME)
         arrayType.copy(
            name = QualifiedName(ArrayType.NAME, listOf(anonymousTypeName.fqn())),
            typeParametersTypeNames = listOf(anonymousTypeName.fqn()))
      } else {
         type
      }
   }

   // Processes anonymous field definitions like:
   // inputId: InputId
   // or
   // orderId
   private fun simpleAnonymousFieldDefinitions(anonymousTypeDefinition: AnonymousTypeDefinition): Map<String, Field> {
      return anonymousTypeDefinition
         .fields
         .filterIsInstance<SimpleAnonymousFieldDefinition>()
         .map { simpleAnonymousFieldDefinition -> simpleAnonymousFieldDefinitionToField(simpleAnonymousFieldDefinition) }
         .toMap()
   }

   // Processes anonymous field definitions like:
   // traderName: TraderName (from this.traderId)
   private fun selfReferencedAnonymousFieldDefinitions(anonymousTypeDefinition: AnonymousTypeDefinition, sourceTypeName: QualifiedName? = null): Map<String, Field> {
      return anonymousTypeDefinition
         .fields
         .filterIsInstance<SelfReferencedFieldDefinition>()
         .map { selfReferencedFieldDefinition ->
            selfReferencedFieldDefinition.fieldName to Field(
               selfReferencedFieldDefinition.fieldType.toVyneQualifiedName(),
               modifiers = listOf(),
               accessor = null,
               readCondition = null,
               typeDoc = null,
               sourcedBy = FieldSource(
                  selfReferencedFieldDefinition.referenceFieldName,
                  selfReferencedFieldDefinition.fieldType.toVyneQualifiedName(),
                  sourceTypeName ?: selfReferencedFieldDefinition.referenceFieldContainingType.toVyneQualifiedName()
               )
            )
         }.toMap()
   }

   // Processes anonymous field definitions like:
   // salesPerson {
   //       firstName : FirstName
   //       lastName : LastName
   //   }(from this.salesUtCode)
   private fun complexAnonymousFieldDefinitions(
      anonymousTypeDefinition: AnonymousTypeDefinition,
      schema: Schema,
      sourceTypeName: QualifiedName? = null): Map<String, Field> {
      return anonymousTypeDefinition
         .fields
         .filterIsInstance<ComplexFieldDefinition>()
         .map { complexFieldDefinition ->
            val anonymousTypeNameForComplexField = anonymousTypeName()
            val attributeMap = complexFieldDefinition.fieldDefinitions.map { simpleAnonymousFieldDefinitionToField(it) }.toMap()
            val anonymousTypeForComplexField = anonymousType(attributeMap, anonymousTypeNameForComplexField,  schema, false)
            complexFieldDefinition.fieldName to Field(
               anonymousTypeForComplexField.qualifiedName,
               modifiers = listOf(),
               accessor = null,
               readCondition = null,
               typeDoc = null,
               sourcedBy = FieldSource(
                  complexFieldDefinition.referenceFieldName,
                  anonymousTypeForComplexField.qualifiedName,
                  sourceTypeName ?: complexFieldDefinition.referenceFieldContainingType.toVyneQualifiedName()
               )
            )
         }.toMap()

   }

   private fun simpleAnonymousFieldDefinitionToField(simpleAnonymousFieldDefinition: SimpleAnonymousFieldDefinition): Pair<String, Field> {
      return simpleAnonymousFieldDefinition.fieldName to Field(
         simpleAnonymousFieldDefinition.fieldType.toVyneQualifiedName(),
         modifiers = listOf(),
         accessor = null,
         readCondition = null,
         typeDoc = null
      )
   }

   private fun validateAnonymousTypeDefinition(anonymousTypeDefinition: AnonymousTypeDefinition) {
      if (anonymousTypeDefinition.fields.any { it !is SimpleAnonymousFieldDefinition && it !is SelfReferencedFieldDefinition && it !is ComplexFieldDefinition }) {
         throw CompilationException(CompilationError(0, 0, "only simple anonymous field definitions supported currently!"))
      }
   }

   private fun anonymousTypeName() = AnonymousTypeNameGenerator.generate()
}

object AnonymousTypeNameGenerator {
   private val random: SecureRandom = SecureRandom()
   private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

   // This is both shorter than a UUID (e.g. Xl3S2itovd5CDS7cKSNvml4_ODA)  and also more secure having 160 bits of entropy.
   fun generate(): String {
      val buffer = ByteArray(20)
      random.nextBytes(buffer)
      return "vyne.AnonymousProjectedType${encoder.encodeToString(buffer)}"
   }
}
