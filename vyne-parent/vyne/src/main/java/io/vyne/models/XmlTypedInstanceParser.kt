package io.vyne.models

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.XpathAccessor
import org.apache.commons.io.IOUtils
import org.w3c.dom.Document
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

class XmlTypedInstanceParser(val primitiveParser: PrimitiveParser = PrimitiveParser()) {
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


   fun parse(xml: Document, type: Type, schema: Schema, accessor: XpathAccessor): TypedInstance {
      val xpath = xpathCache.get(accessor.expression)
      val result = xpath.evaluate(xml)
      return primitiveParser.parse(result, type)
   }


   fun parse(xml: String, type: Type, schema: Schema, accessor: XpathAccessor): TypedInstance {
      val document = documentCache.get(xml)
      return parse(document, type, schema, accessor)
   }
}
