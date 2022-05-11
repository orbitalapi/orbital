@file:OptIn(ExperimentalSerializationApi::class)

package io.vyne.spring.projection.serde

import io.vyne.models.*
import io.vyne.schemas.Schema
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal

/**
 * This class exists to aid with serialization and deserialization of TypedInstances
 * when passing across the wire - including writing to Bytes, and then converting the
 * serialzied / deserialzied value back into a TypedInstance (via the .toTypedInstance(schema) method)
 *
 * Types are not serialized, only their name.  It is expected that both serializer and
 * deserializer are working against the same schema, or differences will result in the deserialized value.
 *
 * The value from this serialization is more descriptive than a TypeNamedInstance, as
 * we include the Taxi type data all the way down the chain.   However, in order to support
 * deserialzation, we need to handle polymorphic type references.
 * Specific challenges are ensuring the following references are resolved in a compile constant way:
 *  - The T of List<T>
 *  - The T of Map<String,T>
 *  - TypedInstance and TypedCollection
 *  - the Value of TypedInstance needs
 *
 * One option explored was converting to a Json TypeNamedInstance, then encoding that.
 * However, the performance overhead was significant, as much of the deserialization inference needed
 * reflection.
 *
 * Instead, this approach uses Kotlin serialization to convert to a cbor representation, using
 * statically defined wrapper classes for all the T references.
 *
 * Note that to keep objects lightweight, the DataSource is not serialized, only a reference to it.
 * It is expected that callers retain a reference to the full DataSource (indexed by Id) in order to look up later.
 *
 * Update 07-Apr-2022:
 * After upgrading to Kotlin 1.6.10 and Kotlin Serialization 1.3.x,
 * this SerializableTypedInstance is no longer usable if called from
 * a different package - it results in NoSuchMethod exceptions.
 *
 * However, if the serialization code and caller live in the same package,
 * it seems to work.
 *
 * Have raised a topic for discussion in the Kotlin Serialization Slack
 * channel, and will investigate.
 *
 * For now, moving the code into the package where we handle
 * Hazelcast work distribution resolves the issue.
 */
@Serializable
data class SerializableTypedInstance(
   val typeName: String,
   val value: SerializableTypedValue,
   val dataSourceId: String
) : SerializableTypedValue() {
   fun toTypedInstance(schema: Schema): TypedInstance {
      val dataSource = DataSourceReference.staticSourceOrReference(dataSourceId)
      val converted = when (this.value) {
         is MapWrapper -> {
            val typedInstances = this.value.value.mapValues { (_, mapValue) -> mapValue.toTypedInstance(schema) }
            TypedObject(schema.type(this.typeName), typedInstances, dataSource)
         }
         is ListWrapper -> {
            val typedInstances = this.value.value.map { it.toTypedInstance(schema) }
            TypedCollection(schema.type(this.typeName), typedInstances, dataSource)
         }
         is SerializableTypedValueWrapper<*> -> TypedInstance.from(
            schema.type(this.typeName),
            value.value,
            schema,
            source = dataSource,
            evaluateAccessors = false
         )
         is SerializedNull -> TypedNull.create(schema.type(this.typeName), dataSource)
         is SerializableTypedInstance -> error("Unhandled type of SerializableTypedInstance: ${this.value::class.simpleName}")
      }
      return converted
   }

   fun toBytes(): ByteArray {
      return CborSerializer.serializer.encodeToByteArray(this)
   }

   companion object {
      fun fromBytes(byteArray: ByteArray): SerializableTypedInstance {
         return CborSerializer.serializer.decodeFromByteArray<SerializableTypedInstance>(byteArray)
      }
   }
}

/**
 * Represents a reference to the original data source
 */
data class DataSourceReference(override val id: String) : DataSource {
   override val name: String = "DataSourceReference"
   override val failedAttempts: List<DataSource> = emptyList()

   companion object {
      /**
       * Returns the actual data source (if it's static), or an id-bound reference
       */
      fun staticSourceOrReference(id: String): DataSource {
         return if (StaticDataSources.isStatic(id)) {
            StaticDataSources.forId(id)
         } else {
            DataSourceReference(id)
         }
      }
   }
}

internal object CborSerializer {
   val module = SerializersModule {
      polymorphic(Temporal::class) {
         subclass(Instant::class, InstantSerializer)
         subclass(LocalDate::class, LocalDateSerializer)
         subclass(LocalTime::class, LocalTimeSerializer)
         subclass(LocalDateTime::class, LocalDateTimeSerializer)
         subclass(ZonedDateTime::class, ZonedDateTimeTimeSerializer)
      }
   }
   val serializer = Cbor { serializersModule = module }
}

@Serializable
sealed class SerializableTypedValue

@Serializable
object SerializedNull : SerializableTypedValue()

@Serializable
sealed class SerializableTypedValueWrapper<T>() : SerializableTypedValue() {
   abstract val value: T
}

@Serializable
class StringWrapper(override val value: String) : SerializableTypedValueWrapper<String>()

@Serializable
class IntWrapper(override val value: Int) : SerializableTypedValueWrapper<Int>()

@Serializable
class TemporalWrapper(override val value: Temporal) : SerializableTypedValueWrapper<Temporal>()

@Serializable(with = BigDecimalSerializer::class)
class BigDecimalWrapper(override val value: BigDecimal) : SerializableTypedValueWrapper<BigDecimal>()

@Serializable
class BooleanWrapper(override val value: Boolean) : SerializableTypedValueWrapper<Boolean>()

@Serializable
class MapWrapper(override val value: Map<String, SerializableTypedInstance>) :
   SerializableTypedValueWrapper<Map<String, SerializableTypedInstance>>()

@Serializable
class ListWrapper(override val value: List<SerializableTypedInstance>) :
   SerializableTypedValueWrapper<List<SerializableTypedInstance>>()

class BigDecimalSerializer : KSerializer<BigDecimalWrapper> {
   override fun deserialize(decoder: Decoder): BigDecimalWrapper {
      return BigDecimalWrapper(BigDecimal(decoder.decodeString()))
   }

   override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

   override fun serialize(encoder: Encoder, wrapper: BigDecimalWrapper) {
      return encoder.encodeString(wrapper.value.toString())
   }
}

object SerializableTypeConverter {
   val converter = TypedInstanceConverter(SerializableTypeMapper)
}

/**
 * Wraps the value of a TypedInstance in a concrete class which makes the
 * type of the value known at compilation time
 */
object SerializableTypeMapper : TypedInstanceMapper {
   override fun handleUnwrapped(original: TypedInstance, value: Any?): Any? {
      val wrappedMap = MapWrapper(value as Map<String, SerializableTypedInstance>)
      return SerializableTypedInstance(
         original.typeName,
         wrappedMap,
         original.source.id
      )
   }

   override fun handleUnwrappedCollection(original: TypedInstance, value: Any?): Any? {
      return SerializableTypedInstance(
         original.typeName,
         ListWrapper(value as List<SerializableTypedInstance>),
         original.source.id
      )
   }

   override fun map(typedInstance: TypedInstance): SerializableTypedValue? {
//      val formattedValue = TypeNamedInstanceMapper.formatValue(typedInstance)
      val serializableValue = when (val formattedValue = typedInstance.value) {
         null -> SerializedNull
         is String -> StringWrapper(formattedValue)
         is Int -> IntWrapper(formattedValue)
         is BigDecimal -> BigDecimalWrapper(formattedValue)
         is Boolean -> BooleanWrapper(formattedValue)
         is Temporal -> TemporalWrapper(formattedValue)
         else -> error("No serializer support provided for type ${formattedValue::class.simpleName}")
      }
      return wrapValueInSerializable(typedInstance, serializableValue)
   }

   private fun wrapValueInSerializable(
      instance: TypedInstance,
      value: SerializableTypedValue
   ): SerializableTypedInstance {
      return SerializableTypedInstance(
         instance.typeName,
         value,
         instance.source.id
      )
   }
}

fun TypedInstance.toSerializable(): SerializableTypedInstance {
   val value = SerializableTypeConverter.converter.convert(this)
      ?: error("TODO : Handle nulls in  TypedInstance.toSerializable()")

   return value as SerializableTypedInstance
}


@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {
   override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
   override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
}

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : KSerializer<LocalDate> {
   override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
   override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
}

@Serializer(forClass = LocalTime::class)
object LocalTimeSerializer : KSerializer<LocalTime> {
   override fun deserialize(decoder: Decoder): LocalTime = LocalTime.parse(decoder.decodeString())
   override fun serialize(encoder: Encoder, value: LocalTime) = encoder.encodeString(value.toString())
}

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
   override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
   override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
}

@Serializer(forClass = ZonedDateTime::class)
object ZonedDateTimeTimeSerializer : KSerializer<ZonedDateTime> {
   override fun deserialize(decoder: Decoder): ZonedDateTime = ZonedDateTime.parse(decoder.decodeString())
   override fun serialize(encoder: Encoder, value: ZonedDateTime) = encoder.encodeString(value.toString())
}
