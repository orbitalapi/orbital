package com.orbitalhq.models.format

import com.orbitalhq.schemas.Type

/**
 * Provides a list of all the formats registered with the system
 */
interface FormatRegistry {
   val formats: List<ModelFormatSpec>
   fun forType(type: Type): ModelFormatSpec?
}

open class DefaultFormatRegistry(override val formats: List<ModelFormatSpec>) : FormatRegistry {

   companion object {
      fun empty() = DefaultFormatRegistry(emptyList())
   }

   private val formatDetector = FormatDetector(formats)
   override fun forType(type: Type): ModelFormatSpec? {
      return formatDetector.getFormatType(type)?.second
   }
}
