package io.vyne

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.module.SimpleModule

fun <T : Any> T.getPrivateProperty(variableName: String): Any? {
   return javaClass.getDeclaredField(variableName).let { field ->
      field.isAccessible = true
      return@let field.get(this)
   }
}

class VyneEnumDeserialisationModuleBeanDeserializerModifier: BeanDeserializerModifier() {

   override fun modifyEnumDeserializer(config: DeserializationConfig?, type: JavaType, beanDesc: BeanDescription?, deserializer: JsonDeserializer<*>?): JsonDeserializer<*> {
      return VyneEnumDeserialier(type)
   }
}

class VyneEnumDeserialier(private val type: JavaType): JsonDeserializer<Enum<*>>() {
   override fun deserialize(jsonParser: JsonParser, ctx: DeserializationContext): Enum<*>? {
      val enumClass = type.rawClass as Class<Enum<*>>
      val enumValues = enumClass.enumConstants as Array<Enum<*>>

      val serialisedValue = jsonParser.valueAsString
      val firstAttempt = enumValues.firstOrNull { it.name == serialisedValue }
      if (firstAttempt == null) {
         enumClass.declaredFields.find { field -> field.name == "value" }?.let { valueField ->
           val fromValue =  enumValues.firstOrNull { enumValue ->
              enumValue.getPrivateProperty("value").toString() == serialisedValue
            }

            if (fromValue != null) {
               return fromValue
            }
         }
      }

      return firstAttempt
   }
}
fun vyneEnumDeserialisationModule(): SimpleModule {
   val module = SimpleModule()
   module.setDeserializerModifier(VyneEnumDeserialisationModuleBeanDeserializerModifier())
   return module
}

