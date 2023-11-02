package com.orbitalhq.protobuf

import com.google.common.net.MediaType
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.models.format.ModelFormatSerializer
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.fqn
import lang.taxi.generators.protobuf.ProtobufMessageAnnotation

object ProtobufAnnotationSpec {
   val NAME = "com.orbitalhq.formats.Protobuf".fqn()
   val taxi = """
      annotation com.orbitalhq.formats.Protobuf {
      }
   """.trimIndent()
}

object ProtobufFormatSpec : ModelFormatSpec {
   override val serializer: ModelFormatSerializer
      get() = TODO("Not yet implemented")
   override val deserializer: ModelFormatDeserializer = ProtobufFormatDeserializer()
   override val annotations: List<QualifiedName> = listOf(ProtobufMessageAnnotation.NAME.fqn())
   override val mediaType: String = MediaType.PROTOBUF.toString()
}
