package io.vyne.models

import com.fasterxml.jackson.databind.node.ObjectNode
import io.vyne.utils.log
import lang.taxi.annotations.DataType
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

// MP (16-Nov): Looks like this class is growing to multiple concerns, and we may wish
// to split this out to MapReader / PojoReader / JsonReader.
// I suspect the reason we've ended up with multiple different reader strategies in here
// is because of the various different ways we parse / handle Json content (sometimes it's a Map, sometimes
// it's a JsonNode).
class ValueReader {
   fun contains(source: Any, attribute: String): Boolean {
      return when (source) {
         is Map<*, *> -> mapContains(source, attribute)
         is ObjectNode -> jsonObjectContains(source, attribute)
         else -> objectContains(source, attribute)
      }
   }

   private fun jsonObjectContains(source: ObjectNode, attribute: String): Boolean {
      return source.has(attribute)
   }

   private fun objectContains(source: Any, attribute: String): Boolean {
      return getObjectMember(source, attribute) != null
   }

   private fun mapContains(source: Map<*, *>, attribute: String): Boolean {
      return source.containsKey(attribute)
   }

   fun read(source: Any, attribute: String): Any? {
      return when (source) {
         is Map<*, *> -> readFromMap(source, attribute)
         is ObjectNode -> readFromJsonObject(source, attribute)
         else -> readFromObject(source, attribute)
      }
   }

   private fun readFromJsonObject(source: ObjectNode, attribute: String): Any? {
      val valueNode = source[attribute]
      return when {
         valueNode.isBoolean -> valueNode.asBoolean()
         valueNode.isBigDecimal -> valueNode.decimalValue()
         valueNode.isDouble -> valueNode.decimalValue()
         valueNode.isInt -> valueNode.asInt()
         valueNode.isLong -> valueNode.asLong()
         valueNode.isNull -> null
         valueNode.isTextual -> valueNode.asText()
         else -> {
            log().warn("No valueNode parsing strategy found for attribute $attribute with node type ${valueNode::class.simpleName} and node type ${valueNode.nodeType}.  Returning null")
            null
         }
      }
   }

   private fun readFromObject(source: Any, attribute: String): Any? {
      val member = getObjectMember(source, attribute)
         ?: throw IllegalArgumentException("Source of type ${source::class.simpleName} does not declare a field $attribute")
      return member.call(source)
   }

   private fun getObjectMember(source: Any, attribute: String): KProperty1<out Any, Any?>? {
      val srcClass = source::class
      return srcClass.memberProperties.firstOrNull {
         it.name == attribute || hasDataTypeAnnotation(it, attribute)
      }
   }

   private fun hasDataTypeAnnotation(property: KProperty1<out Any, Any?>, attribute: String): Boolean {
      property.javaField?.apply {
         this.getAnnotation(DataType::class.java)?.apply { if (this.value == attribute) return true }
      }
      val dataType = property.findAnnotation<DataType>() ?: return false
      return (dataType.value == attribute)
   }

   private fun readFromMap(source: Map<*, *>, attribute: String): Any? {
      return source[attribute]
   }
}
