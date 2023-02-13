package io.vyne.cockpit.core.schemas.editor.generator

import io.vyne.PackageIdentifier
import io.vyne.cockpit.core.schemas.editor.EditedSchema
import io.vyne.schemas.PartialSchema
import io.vyne.schemas.PartialService
import io.vyne.schemas.PartialType
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.schemas.taxi.toVyneQualifiedName
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.Logger
import lang.taxi.generators.SchemaWriter
import lang.taxi.types.Type

class VyneSchemaToTaxiGenerator(
) {

   fun generate(schemaToGenerate: EditedSchema, referenceSchema: TaxiSchema = TaxiSchema.empty()): GeneratedTaxiCode {
      val schemaWriter: SchemaWriter = SchemaWriter(typeFilter = { type: Type ->

         if (schemaToGenerate.typeNames.contains(type.toVyneQualifiedName())) {
            // Always generate the types explicity in the set of types edited.
            true
         } else {
            // Don't include types that are present in the reference schema.  We'll
            // resolve those through imports / dependencies.
            !referenceSchema.taxi.containsType(type.toQualifiedName().parameterizedName)
         }
      })
      val logger: Logger = Logger()

      val taxiSchema = VyneSchemaToTaxiSchemaMapper(
         schemaToGenerate,
         referenceSchema,
         logger
      ).generate()
      return GeneratedTaxiCode(
         schemaWriter.generateSchemas(taxiSchema),
         logger.messages
      )

   }

   fun generateWithPackageUpsertDelete(
      packageIdentifier: PackageIdentifier,
      edits: EditedSchema,
      existingPartialSchemaForThePackage: PartialSchema,
      referenceSchemaProvider: (types: Set<PartialType>, services: Set<PartialService>) -> TaxiSchema): GeneratedTaxiCode {
      val aggregatedTypes = mutableSetOf<PartialType>()
      aggregatedTypes.addAll(edits.types)
      val requestedEditTypeNames = edits.types.map { it.fullyQualifiedName }
      existingPartialSchemaForThePackage.types.forEach{ existingType ->
         if (!requestedEditTypeNames.contains(existingType.fullyQualifiedName)) {
            aggregatedTypes.add(existingType)
         }
      }

      val aggregatedServices = mutableSetOf<PartialService>()
      aggregatedServices.addAll(edits.services)
      val requestedEditServiceNames = edits.services.map { it.name.fullyQualifiedName }
      existingPartialSchemaForThePackage.services.forEach { existingService ->
         if(!requestedEditServiceNames.contains(existingService.name.fullyQualifiedName)) {
            aggregatedServices.add(existingService)
         }
      }

      edits.removedTypes.forEach { removedType ->
         aggregatedTypes.removeIf { filter -> filter.fullyQualifiedName == removedType.fullyQualifiedName }
      }
      edits.removedServices.forEach { removedService ->
         aggregatedServices.removeIf { filter -> filter.name == removedService }
      }

      val aggregatedEditedSchema = EditedSchema(aggregatedTypes, aggregatedServices)
      val referenceSchema = referenceSchemaProvider(aggregatedTypes, aggregatedServices)
      return generate(aggregatedEditedSchema, referenceSchema)
   }
}
