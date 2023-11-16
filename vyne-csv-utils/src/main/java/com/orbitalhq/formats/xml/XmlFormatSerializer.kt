package com.orbitalhq.formats.xml

import com.orbitalhq.models.TypeNamedInstance
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.format.ModelFormatSerializer
import com.orbitalhq.models.format.TypedInstanceInfo
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import lang.taxi.xsd.XsdAnnotations
import org.codehaus.stax2.XMLOutputFactory2
import java.io.ByteArrayOutputStream
import javax.xml.stream.XMLStreamWriter

object XmlFormatSerializer : ModelFormatSerializer {


   override fun write(
      result: TypedInstance,
      metadata: Metadata,
      schema: Schema,
      typedInstanceInfo: TypedInstanceInfo
   ): Any {
      return write(result, schema, typedInstanceInfo)
   }

   override fun write(
      result: TypeNamedInstance,
      attributes: Set<AttributeName>,
      metadata: Metadata,
      typedInstanceInfo: TypedInstanceInfo
   ): Any? {
      TODO("Not yet implemented")
   }

   override fun write(
      result: TypeNamedInstance,
      type: Type,
      metadata: Metadata,
      typedInstanceInfo: TypedInstanceInfo
   ): Any? {
      TODO("Not yet implemented")
   }

   override fun write(result: TypedInstance, schema: Schema, typedInstanceInfo: TypedInstanceInfo): Any {
      val swFactory = XMLOutputFactory2.newFactory()
      val stream = ByteArrayOutputStream()
      val writer = swFactory.createXMLStreamWriter(stream)
      StaxXmlSerializer(schema, writer).write(result)
      return stream.toString()
   }

   override fun writeAsBytes(result: TypedInstance, schema: Schema, typedInstanceInfo: TypedInstanceInfo): ByteArray {
      val xmlString = write(result, schema, typedInstanceInfo) as String
      return xmlString.toByteArray()
   }
}

private class StaxXmlSerializer(private val schema: Schema, private val writer: XMLStreamWriter) {
   fun write(value: TypedInstance) {
      writer.writeStartDocument()
      writeInstanceToXml(value, value.typeName.fqn().name)
      writer.writeEndDocument()
   }

   private fun writeInstanceToXml(value: TypedInstance, elementName: String) {
      when (value) {
         is TypedObject -> writeObjectToXml(value, elementName)
         is TypedCollection -> writeCollectionToXml(value, elementName)
         else -> TODO()
      }
   }

   private fun writeCollectionToXml(value: TypedCollection, elementName: String) {
      value.forEach { member ->
         writeInstanceToXml(member, elementName)
      }
   }

   private data class FieldToMap(val name: String, val value: TypedInstance, val isAttribute: Boolean)

   private fun writeObjectToXml(value: TypedObject, elementName: String) {
      val type = schema.type(value.typeName)
      writer.writeStartElement(elementName)

      value.entries.map { (fieldName, value) ->
         val field = type.attribute(fieldName)
         val isAttribute = field.hasMetadata(XsdAnnotations.XML_ATTRIBUTE_TYPE.toVyneQualifiedName())
         FieldToMap(fieldName, value, isAttribute)
      }
         // Process attributes first, as we need to set them on the root node
         .sortedWith { field1, field2 ->
            when {
               field1.isAttribute && field2.isAttribute -> 0
               field1.isAttribute -> -1
               field2.isAttribute -> 1
               else -> 0
            }
         }
         .forEach { (fieldName, fieldValue, isAttribute) ->
            when (fieldValue) {
               is TypedValue -> {
                  if (isAttribute) {
                     writer.writeAttribute(fieldName, fieldValue.value.toString())
                  } else {
                     writer.writeStartElement(fieldName)
                     writer.writeCharacters(fieldValue.value.toString())
                     writer.writeEndElement()
                  }

               }

               is TypedCollection -> writeCollectionToXml(fieldValue, fieldName)
               is TypedObject -> writeObjectToXml(fieldValue, fieldName)
               is TypedNull -> {}// just skip it.
               else -> {
                  TODO()
               }
            }
         }
      writer.writeEndElement()
   }

}
