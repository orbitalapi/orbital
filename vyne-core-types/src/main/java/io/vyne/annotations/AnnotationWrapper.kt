package io.vyne.annotations

import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

/**
 * Represents a class that can return a Taxi Annotation.
 * Allows for strongly-typed annotations being passed around in Kotlin code
 */
interface AnnotationWrapper {
   fun asAnnotation(schema:TaxiDocument): Annotation
}
