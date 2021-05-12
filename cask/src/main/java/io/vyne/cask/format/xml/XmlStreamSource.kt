package io.vyne.cask.format.xml

import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.utils.xml.XmlDocumentProvider
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.io.InputStream

class XmlStreamSource(private val input: Flux<InputStream>,
                      versionedType: VersionedType,
                      schema: Schema,
                      override val messageId: String,
                      elementSelector: String? = null) : StreamSource {
   private val xmlDocumentProvider = XmlDocumentProvider(elementSelector)
   private val mapper = XmlStreamMapper(versionedType, schema)

   private val observers = mutableListOf<Sinks.Many<InstanceAttributeSet>>()

   override fun withObserver(sink: Sinks.Many<InstanceAttributeSet>): StreamSource {
      observers.add(sink)
      return this
   }

   override val stream: Flux<InstanceAttributeSet>
      get() {
         return xmlDocumentProvider.parseXmlStream(input).map { mapper.map(it, messageId) }
            .doOnEach { signal ->
               if (signal.isOnNext && signal.hasValue()) {
                  observers.forEach { it.tryEmitNext(signal.get()!!) }
               }
            }
      }
}
