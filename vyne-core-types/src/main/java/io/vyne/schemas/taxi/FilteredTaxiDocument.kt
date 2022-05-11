package io.vyne.schemas.taxi

import lang.taxi.TaxiDocument
import lang.taxi.functions.Function
import lang.taxi.services.Service
import lang.taxi.types.Annotation
import lang.taxi.types.Type

/**
 * A TaxiDocument which allows filtering of the types
 * and services within it.
 *
 * Used when editing schemas, and we want to take a slice of the original
 * taxi.
 */
fun TaxiDocument.filtered(
   typeFilter: (Type) -> Boolean = { true },
   serviceFilter: (Service) -> Boolean = { true },
   functionFilter: (Function) -> Boolean = { true },
   annotationFilter: (Annotation) -> Boolean = { true }
): TaxiDocument {
   return TaxiDocument(
      types = this.types.filter(typeFilter).toSet(),
      services = this.services.filter(serviceFilter).toSet(),
      policies = this.policies,
      functions = this.functions.filter(functionFilter).toSet(),
      annotations = this.annotations.filter(annotationFilter).toSet(),
      views = this.views
   )
}
