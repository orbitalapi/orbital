package com.orbitalhq.formats.xml

import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.models.format.ModelFormatSerializer
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.fqn

object XmlAnnotationSpec {
   val NAME = "com.orbitalhq.formats.Xml".fqn()

   val XmlAttributeName = "XmlAttribute".fqn()

   val taxi = """
      namespace ${NAME.namespace} {
         annotation Xml {}
         annotation XmlAttribute
         annotation XPath {
            value : String
         }
      }
   """.trimIndent()
}

object XmlFormatSpec : ModelFormatSpec {
   override val serializer: ModelFormatSerializer
      get() = TODO("Not yet implemented")
   override val deserializer: ModelFormatDeserializer = XmlFormatDeserializer
   override val annotations: List<QualifiedName> = listOf(XmlAnnotationSpec.NAME)
}
