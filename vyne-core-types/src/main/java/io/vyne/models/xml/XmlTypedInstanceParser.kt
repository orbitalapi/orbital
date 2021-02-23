package io.vyne.models.xml

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import io.vyne.models.DataSource
import io.vyne.models.PrimitiveParser
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.xbatchTimed
import lang.taxi.types.XpathAccessor
import org.apache.commons.io.IOUtils
import org.w3c.dom.Document
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

class XmlTypedInstanceParser(private val primitiveParser: PrimitiveParser = PrimitiveParser()) {
   private val factory = DocumentBuilderFactory.newInstance()
   private val builder = factory.newDocumentBuilder()
   private val xpathFactory = XPathFactory.newInstance()
   private val documentCache: LoadingCache<String, Document> = CacheBuilder.newBuilder()
      .expireAfterAccess(2, TimeUnit.SECONDS)
      .build(object : CacheLoader<String, Document>() {
         override fun load(key: String): Document {
            return builder.parse(IOUtils.toInputStream(key))
         }
      })
   private val xpathCache: LoadingCache<String, XPathExpression> = CacheBuilder.newBuilder()
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .build(object : CacheLoader<String, XPathExpression>() {
         override fun load(key: String): XPathExpression {
            val xpath = xpathFactory.newXPath()
            return xpath.compile(key)
         }
      })


   fun parse(
      xml: Document,
      type: Type,
      accessor: XpathAccessor,
      schema: Schema,
      source: DataSource,
      nullable: Boolean
   ): TypedInstance {
      val result = xbatchTimed("XmlTypeInstanceParser:parse") {
         val xpath = xpathCache.get(accessor.expression)
         val result = xbatchTimed("Evaluate xpath ${accessor.expression}") {
            xpath.evaluate(xml)
         }

         if (result.isEmpty()) {
            //xpath evaluate returns empty string if there is no match.
            TypedNull.create(type, source)
//            val matchingNodes =
//               batchTimed("xpath evaluate on empty string") { xpath.evaluate(xml, XPathConstants.NODESET) as NodeList? }
//            if ((matchingNodes == null || matchingNodes.length == 0) && nullable) {
//               TypedNull.create(type, source)
//            } else {
//               primitiveParser.parse(result, type, source)
//            }
         } else {
            primitiveParser.parse(result, type, source)
         }

      }
      return result
   }


   fun parse(
      xml: String,
      type: Type,
      accessor: XpathAccessor,
      schema: Schema,
      source: DataSource,
      nullable: Boolean
   ): TypedInstance {
      val document = documentCache.get(xml)
      return parse(document, type, accessor, schema, source, nullable)
   }
}
