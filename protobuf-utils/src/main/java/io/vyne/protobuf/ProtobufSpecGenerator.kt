package io.vyne.protobuf

import com.squareup.wire.schema.Schema
import io.vyne.protobuf.wire.RepoBuilder
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import lang.taxi.generators.protobuf.ProtobufFieldAnnotation
import lang.taxi.types.Arrays
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Responsible for introspecting the protobuf annotations on a taxi type,
 * and recreating a representative protobuf schema for serialization / deserialization operations.
 *
 * A non-goal of this class is to recreate the original spec.
 * We ONLY try to include enough information to support reading and writing.
 *
 * Not sure if this is a good idea - alternatives considered:
 *
 *  * When the protobuf is imported, store a reference to it somewhere, and "attach" that somehow into the schema.
 *    Dismissed as this requires a signficant rework of our schema infrastructure.  Probably the better approach long-term
 *
 *  * Skip this intermediary step, and just use the metadata directly.
 *    Dismissed because the proto libraries seem to have limited support for reading without a spec, especially where
 *    nested objects are present.
 */
class ProtobufSpecGenerator(private val vyneSchema: io.vyne.schemas.Schema) {

   private val generatedProto = mutableMapOf<Type, ProtoMessageSpec>()

   fun generateProtobufSrc(type: Type): Map<Path, String> {
      getOrGenerateProtoSpec(type)
      return generatedProto.values.map { it.path to it.proto }
         .toMap()
   }

   private fun getOrGenerateProtoSpec(type: Type): ProtoMessageSpec {
      if (type.isCollection) {
         return getOrGenerateProtoSpec(type.collectionType!!)
      }
      val messageSpec = generatedProto.getOrPut(type) {

         // Create and store an empty type early.
         // This prevents stack overflow when creating self-referencing types
         val requiredImports = mutableListOf<String>()
         val specUnderConstruction = ProtoMessageSpec(type.qualifiedName.namespace, type.name.name, emptyList(), requiredImports = requiredImports)
         generatedProto[type] = specUnderConstruction


         specUnderConstruction.fields = type.attributes.map { (name, attribute) ->
            val metadata = attribute.getMetadata(ProtobufFieldAnnotation.NAME.fqn())
            val fieldType = vyneSchema.type(attribute.type)
            if (!fieldType.isPrimitive) {
               // Add the type to set of generated fields
               val fieldTypeMessageSpec = getOrGenerateProtoSpec(fieldType)
               requiredImports.add(fieldTypeMessageSpec.path.toString())
            }
            ProtoFieldSpec(
               fieldName = name,
               typeName = metadata.params["protoType"] as String?
                  ?: error("Field $name is missing protoType in ${ProtobufFieldAnnotation.NAME} annotation"),
               tagNumber = metadata.params["tag"] as Int?
                  ?: error("Field $name is missing protoType in ${ProtobufFieldAnnotation.NAME} annotation"),
               repeated = Arrays.isArray(attribute.type.parameterizedName),
               optional = !attribute.nullable
            )
         }
         if (type.isEnum) {
            specUnderConstruction.enumMembers = type.enumValues.associate { enumValue ->
               enumValue.name to enumValue.value as Int
            }
         }

         specUnderConstruction
      }
      return messageSpec
   }

   fun generateProtobufSchema(type: Type): Schema {
      val sources = generateProtobufSrc(type)
      // Here we use util classes from the wire project to generate a
      // fake file system, with fake source files.
      // Add them, then create the schema
      val repoBuilder = RepoBuilder()
      sources.forEach { (path, source) ->
         repoBuilder.add(path.toString(), source)
      }
      return repoBuilder.schema()
   }

   // We use vars here because we need to pre-populate
   // with "empty" references before we build the types.
   // This is to avoid circular references when types self-reference
   private data class ProtoMessageSpec(
      val packageName: String,
      val messageName: String,
      var fields: List<ProtoFieldSpec>,
      var enumMembers: Map<String, Int> = emptyMap(),
      var requiredImports: List<String> = emptyList()
   ) {
      val isEnum: Boolean
         get() = enumMembers.isNotEmpty()
      val path: Path = Paths.get(packageName.replace(".", "/"), "$messageName.proto")


      /**
       * Generates the protobuf required to describe this message type.
       * Expects that a single message per file.
       */
      val proto: String
         get() {
            val packageDeclaration = if (packageName.isNotEmpty()) {
               "package $packageName;"
            } else ""
            val imports = requiredImports.joinToString("\n") { """import "$it";""" }
            val prelude: String = """syntax = "proto3";
               |$packageDeclaration
               |
               |$imports
               |
            """.trimMargin()
            val body = if (isEnum) {
               generateEnumProto()
            } else {
               generateMessageProto()
            }

            return """$prelude
               |$body
            """.trimMargin()
         }

      private fun generateEnumProto(): String {
         val enumValues = this.enumMembers.map { (name, value) ->
            "  $name = $value;"
         }.joinToString("\n")
         return """enum $messageName {
            |$enumValues
            |}
         """.trimMargin()
      }

      private fun generateMessageProto(): String {
         val fieldList = fields.joinToString(separator = "\n") { "  " + it.proto }
         val proto = """message $messageName {
            |$fieldList
            |}
         """.trimMargin()
         return proto
      }
   }

   private data class ProtoFieldSpec(
      val fieldName: String,
      val typeName: String,
      val optional: Boolean,
      val repeated: Boolean,
      val tagNumber: Int
   ) {
      val proto: String
         get() {
            val qualifiers = listOfNotNull(
               if (optional) "optional" else null,
               if (repeated) "repeated" else null
            ).joinToString(" ")
            return "$qualifiers $typeName $fieldName = $tagNumber;".trim()
         }
   }
}

