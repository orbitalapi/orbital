package io.vyne.query

import io.vyne.schemas.Field
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.vyneql.AnonymousTypeDefinition
import io.vyne.vyneql.ProjectedType
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
            val fieldDefinitions = fieldDefinitions(anonymousTypeDefinition)
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
            val fieldDefinitions = concreteProjectionType.attributes.plus(fieldDefinitions(anonymousTypeDefinition))
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

   private fun fieldDefinitions(anonymousTypeDefinition: AnonymousTypeDefinition): Map<String, Field> {
      return anonymousTypeDefinition.fields.map {
         val simpleAnonymousFieldDefinition = it as SimpleAnonymousFieldDefinition
         simpleAnonymousFieldDefinition.fieldName to Field(
            simpleAnonymousFieldDefinition.fieldType.toVyneQualifiedName(),
            modifiers = listOf(),
            accessor = null,
            readCondition = null,
            typeDoc = null
         )
      }.toMap()
   }

   private fun validateAnonymousTypeDefinition(anonymousTypeDefinition: AnonymousTypeDefinition) {
      // Currently we only support:
      // Case for:
      // findAll { foo[] } as {
      // field1
      // field2
      // field3
      // field4: somenamespace.AnotherType
      // }[]
      //
      // or
      //
      // findAll { foo[] }
      // as bar[] {
      //    field1
      //    field2: mynamespace.mytype
      //}[]
      if (anonymousTypeDefinition.fields.any { it !is SimpleAnonymousFieldDefinition }) {
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
