package io.vyne.protobuf

import io.vyne.models.format.ModelFormatDeserializer
import io.vyne.models.format.ModelFormatSerializer
import io.vyne.models.format.ModelFormatSpec
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import lang.taxi.generators.protobuf.ProtobufMessageAnnotation

object ProtobufAnnotationSpec {
   val NAME = "io.vyne.formats.Protobuf".fqn()
   val taxi = """
      annotation io.vyne.formats.Protobuf {
      }
   """.trimIndent()
}

object ProtobufFormatSpec : ModelFormatSpec {
   override val serializer: ModelFormatSerializer
      get() = TODO("Not yet implemented")
   override val deserializer: ModelFormatDeserializer = ProtobufFormatDeserializer()
   override val annotations: List<QualifiedName> = listOf(ProtobufMessageAnnotation.NAME.fqn())
}
