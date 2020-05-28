@file:Suppress("UNCHECKED_CAST")

package io.vyne.models

import io.vyne.schemas.Type
import lang.taxi.jvm.common.PrimitiveTypes
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.lang.Nullable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

interface ConversionService {
   fun <T> convert(@Nullable source: Any?, targetType: Class<T>): T

   companion object {
      fun default(): ConversionService {
         return StringToIntegerConverter(
            VyneDefaultConversionService
         )
      }
   }
}

/**
 * Used when you don't want to perform any conversions
 */
object NoOpConversionService : ConversionService {
   override fun <T> convert(source: Any?, targetType: Class<T>): T {
      return source!! as T
   }
}

object VyneDefaultConversionService : ConversionService {
   private val innerConversionService by lazy {
      val service = DefaultConversionService()
      // TODO :  we need to be much richer about date handling.
      service.addConverter(String::class.java, LocalDate::class.java) { s -> LocalDate.parse(s) }
      service.addConverter(String::class.java, Instant::class.java) { s -> Instant.parse(s) }
      service
   }

   override fun <T> convert(source: Any?, targetType: Class<T>): T {
      return innerConversionService.convert(source, targetType)!!
   }
}

interface ForwardingConversionService : ConversionService {
   val next: ConversionService
}

class StringToIntegerConverter(override val next: ConversionService = NoOpConversionService) : ForwardingConversionService {
   override fun <T> convert(source: Any?, targetType: Class<T>): T {
      return if (source is String && targetType == Int::class.java) {
         BigDecimal(source).intValueExact() as T
      } else {
         next.convert(source, targetType)
      }
   }
}

data class TypedValue private constructor(override val type: Type, override val value: Any) : TypedInstance {
   companion object {
      private val conversionService by lazy {
         ConversionService.default()
      }

      fun from(type: Type, value: Any, converter: ConversionService): TypedValue {
         if (!type.taxiType.inheritsFromPrimitive) {
            error("Type ${type.fullyQualifiedName} is not a primitive, cannot be converted")
         } else {
            val valueToUse = converter.convert(value, PrimitiveTypes.getJavaType(type.taxiType.basePrimitive!!))
            return TypedValue(type, valueToUse)
         }

      }

      @Deprecated("Use conversionService approach")
      fun from(type: Type, value: Any, performTypeConversions: Boolean = true): TypedValue {
         val conversionServiceToUse = if (performTypeConversions) {
            conversionService
         } else {
            NoOpConversionService
         }
         return from(type, value, conversionServiceToUse)
      }
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedValue(typeAlias, value)
   }

   /**
    * Returns true if the two are equal, where the values are the same, and the underlying
    * types resolve to the same type, considering type aliases.
    */
   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedValue) {
         return false
      }
      if (!(this.type.resolvesSameAs(valueToCompare.type) || valueToCompare.type.inheritsFrom(this.type))) return false;
      return this.value == valueToCompare.value
   }

}
