@file:Suppress("UNCHECKED_CAST")

package io.vyne.models

import io.vyne.schemas.Type
import io.vyne.utils.Ids
import lang.taxi.ImmutableEquality
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.FormatsAndZoneOffset
import mu.KotlinLogging
import org.apache.commons.lang3.ClassUtils

interface ConversionService {
   fun <T> convert(source: Any?, targetType: Class<T>, format: FormatsAndZoneOffset?): T

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
   override fun <T> convert(source: Any?, targetType: Class<T>, format: FormatsAndZoneOffset?): T {
      return source!! as T
   }
}


data class TypedValue private constructor(
   override val type: Type,
   override val value: Any,
   override val source: DataSource,
   val format: FormatsAndZoneOffset? = null
) : TypedInstance {
   private val equality = ImmutableEquality(this, TypedValue::type, TypedValue::value)
   private val hash: Int by lazy { equality.hash() }
   override fun toString(): String {
      return "TypedValue(type=${type.qualifiedName.longDisplayName}, value=$value)"
   }
   override val nodeId: String = Ids.fastUuid()

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
         parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException,
         format: FormatsAndZoneOffset? = null
      ): TypedInstance {
         if (!type.taxiType.inheritsFromPrimitive) {
            error("Type ${type.fullyQualifiedName} is not a primitive, cannot be converted")
         } else {
            return try {
               val valueToUse =
                  converter.convert(value, PrimitiveTypes.getJavaType(type.taxiType.basePrimitive!!), format)
               TypedValue(type, valueToUse, source, format)
            } catch (exception: Exception) {
               val messageFormatPart = if (format != null) {
                  "with formats ${format.patterns.joinToString(" , ")}"
               } else {
                  "(no formats were supplied)"
               }
               val error =
                  "Failed to parse value $value to type ${type.longDisplayName} $messageFormatPart - ${exception.message}"
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
         parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException,
         format: FormatsAndZoneOffset? = null
      ): TypedInstance {
         val conversionServiceToUse = if (performTypeConversions) {
            ConversionService.DEFAULT_CONVERTER
         } else {
            NoOpConversionService
         }
         return from(type, value, conversionServiceToUse, source, parsingErrorBehaviour, format)
      }
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedValue(typeAlias, value, source)
   }

   override fun equals(other: Any?): Boolean {
      // Don't call equality.equals() here, as it's too slow.
      // We need a fast, non-reflection based implementation.
      if (this === other) return true
      if (other == null) return false
      if (this.javaClass !== other.javaClass) return false
      val otherTypedValue = other as TypedValue
      // Type uses a fast interned check, so should be fine.
      // value could be slow, but not much we can do here.
      return this.type == otherTypedValue.type && this.value == other.value

   }
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
