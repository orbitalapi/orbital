package com.orbitalhq.cockpit.core.schemas.importing.map

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.cockpit.core.schemas.importing.SchemaConversionRequest
import com.orbitalhq.cockpit.core.schemas.importing.SchemaConverter
import com.orbitalhq.cockpit.core.schemas.importing.SourcePackageWithMessages
import com.orbitalhq.cockpit.core.schemas.importing.toSourcePackageWithMessages
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.NamingUtils.replaceIllegalCharacters
import lang.taxi.generators.SchemaWriter
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.Arrays
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

@Component
class MapToTypeConverter(
   private val schemaWriter: SchemaWriter = SchemaWriter(),
   private val schemaProvider: SchemaProvider,
) : SchemaConverter<MapConverterOptions> {

   companion object {
      const val FORMAT = "map"
   }

   override val supportedFormats: List<String> = listOf(FORMAT)
   override val conversionParamsType: KClass<MapConverterOptions> = MapConverterOptions::class

   override fun convert(
      request: SchemaConversionRequest,
      options: MapConverterOptions
   ): Mono<SourcePackageWithMessages> {
      val generatedTypes =
         MapToTypeBuilder(schemaProvider.schema).convertAndGetTypes(
            options.map, options.createdTypeName,
            options.inheritedTypeName,
            options.fieldsToInclude
         )
      val taxiDoc = TaxiDocument(generatedTypes, emptySet())
      val taxi = schemaWriter.generateSchemas(listOf(taxiDoc))
      val filename = options.createdTypeName
      val generatedTaxiCode = GeneratedTaxiCode(taxi, messages = emptyList(), suggestedFileName = filename)
      return Mono.just(generatedTaxiCode.toSourcePackageWithMessages(request.packageIdentifier, filename))
   }


}


data class MapConverterOptions(
   val map: Map<String, Any>,
   val createdTypeName: String,
   val inheritedTypeName: String? = null,
   /**
    * Allows for including a subset of fields in the
    * created type.
    * If empty, all fields are included
    */
   val fieldsToInclude: Set<String> = emptySet()
)

@VisibleForTesting
internal class MapToTypeBuilder(private val schema: Schema) {
   private val generatedTypes = mutableSetOf<Type>()
   fun convertAndGetTypes(
      map: Map<String, Any>,
      targetTypeName: String,
      inheritsFromTypeName: String? = null,
      fieldsToInclude: Set<String> = emptySet()
   ): Set<Type> {
      convert(map, targetTypeName, inheritsFromTypeName, fieldsToInclude)
      return generatedTypes
   }

   fun convert(
      map: Map<String, Any>,
      targetTypeName: String,
      inheritsFromTypeName: String? = null,
      fieldsToInclude: Set<String> = emptySet()
   ): ObjectType {
      val fields = map
         .filter { (key, _) ->
            if (fieldsToInclude.isEmpty()) {
               true
            } else {
               fieldsToInclude.contains(key)
            }
         }
         .map { (key, value) ->
            generateField(key, value)
         }
      val inheritsFrom = setOfNotNull(inheritsFromTypeName?.let { typeName ->
         schema.type(typeName.fqn()).taxiType
      })
      val type = ObjectType(
         targetTypeName,
         ObjectTypeDefinition(
            fields.toSet(),
            inheritsFrom = inheritsFrom,
            compilationUnit = CompilationUnit.unspecified()
         )
      )
      generatedTypes.add(type)
      return type
   }

   private fun generateField(key: String, value: Any): Field {
      val type = getTypeForValue(key, value)
      return Field(
         name = key,
         type = type,
         compilationUnit = CompilationUnit.unspecified()
      )
   }

   private fun getTypeForValue(key: String, value: Any?): Type {
      val semanticTypeName = key.replaceFirstChar { it.uppercaseChar() }
         .replaceIllegalCharacters()
      val type = when (value) {
         is List<*> -> {
            val memberType = if (value.isEmpty()) {
               PrimitiveType.ANY
            } else {
               val firstItem = value.first()
               getTypeForValue(key, firstItem)
            }
            Arrays.arrayOf(memberType)
         }

         is Map<*, *> -> {
            convert(value as Map<String, Any>, semanticTypeName)
         }

         null -> {
            // Not enough info to infer type from null
            PrimitiveType.ANY
         }

         else -> {
            val baseType = PrimitiveTypes.getTaxiPrimitive(value::class.java)

            ObjectType(
               semanticTypeName,
               ObjectTypeDefinition(
                  inheritsFrom = setOf(baseType),
                  compilationUnit = CompilationUnit.unspecified()
               )
            )
         }
      }
      generatedTypes.add(type)
      return type
   }
}
