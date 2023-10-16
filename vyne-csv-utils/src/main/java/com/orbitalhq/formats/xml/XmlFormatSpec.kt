package com.orbitalhq.formats.xml

import com.google.common.net.MediaType
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.models.format.ModelFormatSerializer
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.fqn
import lang.taxi.xsd.XsdAnnotations

object XmlAnnotationSpec {
   val NAME = "com.orbitalhq.formats.Xml".fqn()

   val XmlAttributeName = XsdAnnotations.XML_ATTRIBUTE_TYPE.qualifiedName.fqn()

   val taxi = """
      namespace ${NAME.namespace} {
         annotation Xml {}
      }
   """.trimIndent()
}

object XmlFormatSpec : ModelFormatSpec {
   override val serializer: ModelFormatSerializer = XmlFormatSerializer
   override val deserializer: ModelFormatDeserializer = XmlFormatDeserializer
   override val annotations: List<QualifiedName> = listOf(XmlAnnotationSpec.NAME)
   override val mediaType: String = MediaType.APPLICATION_XML_UTF_8.toString()
}
