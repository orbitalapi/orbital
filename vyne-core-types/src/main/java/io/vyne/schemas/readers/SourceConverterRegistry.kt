package io.vyne.schemas.readers

/**
 * A service-locator singleton style registry
 * for SourceToTaxiConverter instances.
 *
 * Unfortunately, we can't use dependency injection in some cases,
 * as we currently compile a schema whenever the SchemaSet is deserialized.
 *
 * At that point, we don't have access to any kind of injection capabilities,
 * and have to use a lookup.
 */
object StaticSourceConverterRegistry {
   val registry = SourceConverterRegistry.withDefaults()
}

class SourceConverterRegistry(
   converters: Set<SourceToTaxiConverter> = emptySet(),
   /**
    * If true, also registers with the StaticSourceConverterRegistry.
    * Set this to true in app-level config.
    * Use false for tests.
    */
   private val registerWithStaticRegistry: Boolean = false
) {
   private val _converters = mutableSetOf<SourceToTaxiConverter>()

   init {
      converters.forEach { this.addConverter(it) }
   }

   fun addConverter(converter: SourceToTaxiConverter) {
      _converters.add(converter)
      if (registerWithStaticRegistry) {
         StaticSourceConverterRegistry.registry.addConverter(converter)
      }
   }

   val converters: List<SourceToTaxiConverter>
      get() = _converters.toList()

   companion object {
      fun withDefaults() = SourceConverterRegistry(setOf(TaxiSourceConverter))
   }
}
