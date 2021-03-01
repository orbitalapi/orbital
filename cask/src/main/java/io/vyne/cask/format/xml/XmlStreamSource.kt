package io.vyne.cask.format.xml

import com.ximpleware.VTDGen
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.utils.xml.XmlDocumentProvider
import org.apache.commons.io.IOUtils
import java.io.InputStream

class XmlStreamSource(
   private val input: InputStream,
   versionedType: VersionedType,
   schema: Schema,
   override val messageId: String,
   elementSelector: String? = null
) : StreamSource {
   private val xmlDocumentProvider = XmlDocumentProvider(elementSelector)
   private val mapper = XmlStreamMapper(versionedType, schema)
   private var sequenceCreated: Boolean = false

//   override fun sequence(): Sequence<InstanceAttributeSet> {
//      require(!sequenceCreated) { "This sequence has already been consumed " }
//      sequenceCreated = true
//      return xmlDocumentProvider.parseXmlStream(input)
//         .map { mapper.map(it, messageId) }
//         .asSequence()
//   }

   override fun sequence(): Sequence<InstanceAttributeSet> {
      require(!sequenceCreated) { "This sequence has already been consumed " }
      sequenceCreated = true
      return xmlDocumentProvider
         .parseXmlStreamToVTDNav(input)
         .map { mapper.map(it, messageId) }
         .asSequence()
   }
}
