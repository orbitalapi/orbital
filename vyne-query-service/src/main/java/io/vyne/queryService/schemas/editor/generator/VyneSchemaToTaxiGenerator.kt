package io.vyne.queryService.schemas.editor.generator

import io.vyne.queryService.schemas.editor.EditedSchema
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.Logger
import lang.taxi.generators.SchemaWriter

class VyneSchemaToTaxiGenerator(
) {

   fun generate(schemaToGenerate: EditedSchema, referenceSchema: TaxiSchema = TaxiSchema.empty()): GeneratedTaxiCode {
      val schemaWriter: SchemaWriter = SchemaWriter(typeFilter = { type ->
         // Don't include types that are present in the reference schema.  We'll
         // resolve those through imports / dependencies.
         !referenceSchema.taxi.containsType(type.toQualifiedName().parameterizedName)
      })
      val logger: Logger = Logger()

      val taxiSchema = VyneSchemaToTaxiSchemaMapper(
         schemaToGenerate,
         referenceSchema,
         logger
      ).generate()
      return GeneratedTaxiCode(
         schemaWriter.generateSchemas(listOf(taxiSchema)),
         logger.messages
      )

   }
}
