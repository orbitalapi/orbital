package io.vyne.models

import lang.taxi.annotations.DataType
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class ValueReader {
   fun read(source: Any, attribute: String): Any? {
      return when (source) {
         is Map<*, *> -> readFromMap(source, attribute)
         else -> readFromObject(source, attribute)
      }
   }

   private fun readFromObject(source: Any, attribute: String): Any? {
      val srcClass = source::class
      val member = srcClass.memberProperties.filter {
         it.name == attribute || hasDataTypeAnnotation(it, attribute)
      }
         .firstOrNull() ?: throw IllegalArgumentException("Source of type ${srcClass.simpleName} does not declare a field $attribute")
      val value = member.call(source)
      return value
   }

   private fun hasDataTypeAnnotation(property: KProperty1<out Any, Any?>, attribute: String): Boolean {
      property.javaField?.apply {
         this.getAnnotation(DataType::class.java)?.apply { if (this.value == attribute) return true }
      }
      val dataType = property.findAnnotation<DataType>() ?: return false
      return (dataType.value == attribute)
   }

   private fun readFromMap(source: Map<*, *>, attribute: String): Any {
      return source[attribute] ?: IllegalArgumentException("Source map does not contain an attribute $attribute")
   }
}
