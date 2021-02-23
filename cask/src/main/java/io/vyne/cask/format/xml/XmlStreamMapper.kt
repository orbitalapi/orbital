package io.vyne.cask.format.xml

import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.xtimed
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import org.w3c.dom.Document
import java.util.concurrent.TimeUnit


class XmlStreamMapper(private val versionedType: VersionedType, private val schema: Schema) {
   fun map(document: Document, messagedId:String): InstanceAttributeSet {
      val instance = xtimed("XmlStreamMapper.map", true, timeUnit = TimeUnit.MILLISECONDS) {
         TypedInstance.from(versionedType.type, document, schema, source = Provided)
      }

      return InstanceAttributeSet(
         versionedType,
         instance.value as Map<String, TypedInstance>,
         messagedId
      )
   }

}
