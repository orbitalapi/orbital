@file:Suppress("UNCHECKED_CAST")

package io.vyne.models

import io.vyne.schemas.Type
import lang.taxi.Equality
import lang.taxi.jvm.common.PrimitiveTypes
import mu.KotlinLogging
import org.apache.commons.lang3.ClassUtils

interface ConversionService {
   fun <T> convert(source: Any?, targetType: Class<T>, format: List<String>?): T

   companion object {
      var DEFAULT_CONVERTER: ConversionService = NoOpConversionService
         private set

      init {
         DEFAULT_CONVERTER = try {
            val preferredConversionService = ClassUtils.getClass("io.vyne.models.conversion.VyneConversionService")
            val instance = preferredConversionService.kotlin.objectInstance as ConversionService
            instance
         } catch (e: Throwable) {
            NoOpConversionService
         }
      }

//      /**
//       * Creates a default converter.
//       * Use this if you wish to have a new instance that you further customize.
//       * If you're not planning on customizing, use DEFAULT_CONVERTER
//       */
//      fun newDefaultConverter(): ConversionService {
//         return StringToNumberConverter(
//            FormattedInstantConverter(
//               VyneDefaultConversionService
//            )
//         )
//      }
   }
}

/**
 * Used when you don't want to perform any conversions
 */
object NoOpConversionService : ConversionService {
   override fun <T> convert(source: Any?, targetType: Class<T>, format: List<String>?): T {
      return source!! as T
   }
}


data class TypedValue private constructor(
   override val type: Type,
   override val value: Any,
   override val source: DataSource
) : TypedInstance {
   private val equality = Equality(this, TypedValue::type, TypedValue::value)
   private val hash: Int by lazy { equality.hash() }
   override fun toString(): String {
      return "TypedValue(type=${type.qualifiedName.longDisplayName}, value=$value)"
   }

   init {
      if (type.isEnum) {
         // Explode if we're using old code.
         error("Don't used TypedValue with enums, use a TypedEnumValue")
      }
   }

   companion object {
      private val logger = KotlinLogging.logger {}
//      private val conversionService by lazy {
//         ConversionService.newDefaultConverter()
//      }

      fun from(
         type: Type,
         value: Any,
         converter: ConversionService,
         source: DataSource,
         parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException
      ): TypedInstance {
         if (!type.taxiType.inheritsFromPrimitive) {
            error("Type ${type.fullyQualifiedName} is not a primitive, cannot be converted")
         } else {
            return try {
               val valueToUse =
                  converter.convert(value, PrimitiveTypes.getJavaType(type.taxiType.basePrimitive!!), type.format)
               TypedValue(type, valueToUse, source)
            } catch (exception: Exception) {
               val error =
                  "Failed to parse value $value to type ${type.longDisplayName} - ${exception.message}"
               logger.warn { error }
               return when (parsingErrorBehaviour) {
                  ParsingFailureBehaviour.ThrowException -> throw DataParsingException(error, exception)
                  ParsingFailureBehaviour.ReturnTypedNull -> TypedNull.create(type, FailedParsingSource(value, error))
               }
            }
         }

      }

      @Deprecated("Use conversionService approach")
      fun from(
         type: Type,
         value: Any,
         performTypeConversions: Boolean = true,
         source: DataSource,
         parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException
      ): TypedInstance {
         val conversionServiceToUse = if (performTypeConversions) {
            ConversionService.DEFAULT_CONVERTER
         } else {
            NoOpConversionService
         }
         return from(type, value, conversionServiceToUse, source, parsingErrorBehaviour)
      }
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedValue(typeAlias, value, source)
   }

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = hash

   /**
    * Returns true if the two are equal, where the values are the same, and the underlying
    * types resolve to the same type, considering type aliases.
    */
   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedValue) {
         return false
      }
      if (!(this.type.resolvesSameAs(valueToCompare.type) || valueToCompare.type.inheritsFrom(this.type))) return false
      return this.value == valueToCompare.value
   }
}

class DataParsingException(message: String, exception: Exception) : RuntimeException(message, exception)


/**
 * We couldn't figure out what the right behaviour universally should be, when there's
 * a parsing error.
 *
 * Sometimes you want a null, so things can continue.
 * Sometimes you want an exception, so everything stops.
 *
 * Since we couldn't decide, we made it your responsibility to choose, dear reader.
 * Haha.
 */
enum class ParsingFailureBehaviour {
   /**
    * Return a TypedNull instance, with the reason for
    * the failure captured in the lineage for the value.
    */
   ReturnTypedNull,

   /**
    * Throw a DataParsingException
    */
   ThrowException;
}
