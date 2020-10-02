package io.vyne.cask.format.xml

import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.utils.xml.XmlDocumentProvider
import reactor.core.publisher.Flux
import java.io.InputStream

class XmlStreamSource(private val input: Flux<InputStream>,
                      versionedType: VersionedType,
                      schema: Schema,
                      override val messageId: String,
                      elementSelector: String? = null) : StreamSource {
   private val xmlDocumentProvider = XmlDocumentProvider(elementSelector)
   private val mapper = XmlStreamMapper(versionedType, schema)
   override val stream: Flux<InstanceAttributeSet>
      get() {
         return xmlDocumentProvider.parseXmlStream(input).map { mapper.map(it, messageId) }
      }
}
