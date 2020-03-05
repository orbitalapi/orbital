package io.vyne.models

import lang.taxi.annotations.DataType
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class ValueReader {
   fun contains(source: Any, attribute: String): Boolean {
      return when (source) {
         is Map<*, *> -> mapContains(source, attribute)
         else -> objectContains(source, attribute)
      }
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
         else -> readFromObject(source, attribute)
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

   private fun readFromMap(source: Map<*, *>, attribute: String): Any {
      return source[attribute] ?: IllegalArgumentException("Source map does not contain an attribute $attribute")
   }
}
