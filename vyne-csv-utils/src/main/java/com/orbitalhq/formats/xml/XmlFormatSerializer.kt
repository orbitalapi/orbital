package com.orbitalhq.formats.xml

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedEnumValue
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.format.ModelFormatSerializer
import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import lang.taxi.types.ObjectType
import lang.taxi.xsd.XsdAnnotations
import org.codehaus.stax2.XMLOutputFactory2
import java.io.ByteArrayOutputStream
import javax.xml.stream.XMLStreamWriter

object XmlFormatSerializer : ModelFormatSerializer {

   override fun write(result: TypedInstance, metadata: Metadata, schema: Schema, index: Int): Any {
      val swFactory = XMLOutputFactory2.newFactory()
      val stream = ByteArrayOutputStream()
      val writer = swFactory.createXMLStreamWriter(stream)
      StaxXmlSerializer(schema, writer).write(result)
      return stream.toString()
   }

   override fun write(rawValue: Any?, metadata: Metadata, index: Int): Any? {
      error("This operation is not supported by XML Format")
   }

   override fun writeAsBytes(result: TypedInstance, metadata: Metadata, schema: Schema, index: Int): ByteArray {
      val xmlString = write(result, metadata, schema, index) as String
      return xmlString.toByteArray()
   }
}

private class StaxXmlSerializer(private val schema: Schema, private val writer: XMLStreamWriter) {
   private val namespacePrefixMap = mutableMapOf<String, String>()
   private var prefixCounter = 0

   fun write(value: TypedInstance) {
      // Pre-scan to discover all namespaces
      discoverNamespaces(value)
      writer.writeStartDocument()
      writeInstanceToXml(value, value.typeName.fqn().name, isRootElement = true)
      writer.writeEndDocument()
   }

   private fun discoverNamespaces(value: TypedInstance, field: Field? = null) {
      val namespace = getXmlNamespace(value, field)
      if (namespace != null) {
         getOrCreatePrefix(namespace) // Just to populate the map
      }

      when (value) {
         is TypedObject -> value.entries.forEach { (fieldName, fieldValue) ->
            discoverNamespaces(fieldValue, (value.type).attribute(fieldName))
         }

         is TypedCollection -> value.forEach { discoverNamespaces(it) }
         else -> {} // TypedValue, TypedNull don't have children
      }
   }

   /**
    * Returns the xml namespace.
    * If a field is being used, and is annotated, this takes precedence.
    */
   private fun getXmlNamespace(typedInstance: TypedInstance, field: Field? = null): String? {
      val namespaceAnnotation = XsdAnnotations.XML_NAMESPACE_TYPE.qualifiedName.fqn()
      val type = typedInstance.type
      val metadata = when {
         field?.hasMetadata(namespaceAnnotation) == true -> field.getMetadata(namespaceAnnotation)
         type.hasMetadata(namespaceAnnotation) -> type.firstMetadata(namespaceAnnotation.parameterizedName)
         else -> return null
      }
      val namespaceUri = metadata.params["uri"] as String?
         ?: error("Expected metadata XmlNamespace annotation to have attribute uri, but was not found")
      return namespaceUri
   }

   private fun getOrCreatePrefix(namespaceUri: String): String {
      return namespacePrefixMap.getOrPut(namespaceUri) {
         "ns${prefixCounter++}"
      }
   }

   private fun writeInstanceToXml(value: TypedInstance, elementName: String, isRootElement: Boolean = false) {
      when (value) {
         is TypedValue -> writeScalarToXml(value, elementName, isRootElement)
         is TypedObject -> writeObjectToXml(value, elementName, isRootElement, field = null)
         is TypedCollection -> writeCollectionToXml(value, elementName)
         else -> TODO()
      }
   }

   private fun writeScalarToXml(value: TypedValue, elementName: String, isRootElement: Boolean = false) {
      val namespace = getXmlNamespace(value)

      if (namespace != null) {
         val prefix = getOrCreatePrefix(namespace)
         writer.writeStartElement(prefix, elementName, namespace)
         if (isRootElement) {
            writer.writeNamespace(prefix, namespace)
         }
      } else {
         writer.writeStartElement(elementName)
      }

      writer.writeCharacters(value.toRawObject().toString())
      writer.writeEndElement()
   }

   private fun writeCollectionToXml(value: TypedCollection, elementName: String) {
      value.forEach { member ->
         writeInstanceToXml(member, elementName)
      }
   }

   private data class FieldToMap(val name: String, val value: TypedInstance, val isAttribute: Boolean)

   private fun writeObjectToXml(value: TypedObject, elementName: String, isRootElement: Boolean = false, field: Field?) {
      val type = schema.type(value.typeName)
      val namespace = getXmlNamespace(value, field)

      if (namespace != null) {
         val prefix = getOrCreatePrefix(namespace)
         writer.writeStartElement(prefix, elementName, namespace)
         if (isRootElement) {
            // Write all namespace declarations on root element
            namespacePrefixMap.forEach { (ns, pref) ->
               writer.writeNamespace(pref, ns)
            }
         }
      } else {
         writer.writeStartElement(elementName)
      }

      value.entries.map { (fieldName, fieldValue) ->
         val field = type.attribute(fieldName)
         val isAttribute = field.hasMetadata(XsdAnnotations.XML_ATTRIBUTE_TYPE.toVyneQualifiedName())
         FieldToMap(fieldName, fieldValue, isAttribute)
      }
         .sortedWith { field1, field2 ->
            when {
               field1.isAttribute && field2.isAttribute -> 0
               field1.isAttribute -> -1
               field2.isAttribute -> 1
               else -> 0
            }
         }
         .forEach { (fieldName, fieldValue, isAttribute) ->
            val field = type.attribute(fieldName)
            when (fieldValue) {
               is TypedValue -> {
                  writeValue(isAttribute, fieldName, fieldValue.value.toString(), getXmlNamespace(fieldValue, field))
               }

               is TypedCollection -> writeCollectionToXml(fieldValue, fieldName)
               is TypedObject -> writeObjectToXml(fieldValue, fieldName, field = field)
               is TypedNull -> {} // just skip it
               is TypedEnumValue -> writeValue(
                  isAttribute,
                  fieldName,
                  fieldValue.value.toString(),
                  getXmlNamespace(fieldValue, field = field)
               )

               else -> {
                  TODO("Xml serialization not implemented for typed instance with kind ${fieldValue::class.simpleName}")
               }
            }
         }
      writer.writeEndElement()
   }

   private fun writeValue(
      isAttribute: Boolean,
      fieldName: String,
      fieldValue: String,
      childNamespace: String?
   ) {
      if (isAttribute) {
         writer.writeAttribute(fieldName, fieldValue)
      } else {
         // Check if child element has a different namespace
         if (childNamespace != null) {
            val prefix = getOrCreatePrefix(childNamespace)
            writer.writeStartElement(prefix, fieldName, childNamespace)
         } else {
            writer.writeStartElement(fieldName)
         }
         writer.writeCharacters(fieldValue)
         writer.writeEndElement()
      }
   }
}
