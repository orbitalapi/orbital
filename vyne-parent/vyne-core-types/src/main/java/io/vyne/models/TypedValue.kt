package io.vyne.models

import io.vyne.schemas.Type
import lang.taxi.jvm.common.PrimitiveTypes
import org.springframework.core.convert.support.DefaultConversionService
import java.time.LocalDate


data class TypedValue private constructor(override val type: Type, override val value: Any) : TypedInstance {
   companion object {
      private val conversionService by lazy {
         val service = DefaultConversionService()
         // TODO :  we need to be much richer about date handling.
         service.addConverter(String::class.java, LocalDate::class.java) { s -> LocalDate.parse(s) }
         service
      }

      fun from(type: Type, value: Any, performTypeConversions: Boolean = true): TypedValue {
         val valueToUse = if (performTypeConversions) {
            if (!type.taxiType.inheritsFromPrimitive) {
               error("Type is not a primitive, cannot be converted")
            } else {
               conversionService.convert(value, PrimitiveTypes.getJavaType(type.taxiType.basePrimitive!!))
            }
         } else {
            value
         }
         return TypedValue(type, valueToUse)
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
