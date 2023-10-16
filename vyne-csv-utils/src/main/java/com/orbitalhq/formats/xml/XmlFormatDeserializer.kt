package com.orbitalhq.formats.xml

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import mu.KotlinLogging
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

object XmlFormatDeserializer : ModelFormatDeserializer {
   private val deserializer = XmlDeserializer()
   override fun parseRequired(value: Any, metadata: Metadata): Boolean = true

   override fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema): Any =
      deserializer.parse(value, type, metadata, schema) ?: error("Parsing XML from root returned null")
}

// Note sure about how threadsafe the xml stuff is, so ensuring we can
// factor out to instances if required.
private class XmlDeserializer {
   private val factory = DocumentBuilderFactory.newInstance()
   private val builder = factory.newDocumentBuilder()
   private val xpathFactory = XPathFactory.newInstance()

   private val xmlMapper = XmlMapper()
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val xpathCache: LoadingCache<String, XPathExpression> = CacheBuilder.newBuilder()
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .build(object : CacheLoader<String, XPathExpression>() {
         override fun load(key: String): XPathExpression {
            val xpath = xpathFactory.newXPath()
            return xpath.compile(key)
         }
      })

   private fun isXmlCollectionWrapper(value: Any): Boolean {
      return value is Map<*,*> && value.keys.size == 1 && value[value.keys.single()] is List<*>
   }

   private fun parseWithJackson(value: String, type: Type, schema: Schema): TypedInstance {
      val raw:Any = xmlMapper.readValue(value)
      val typedInstance = when {
         type.isCollection && raw is Collection<*> -> {
            TypedInstance.from(type, raw, schema)
         }
         type.isCollection && isXmlCollectionWrapper(raw) -> {
            val collectionWrapper = raw as Map<*, *>
            val collection = collectionWrapper[collectionWrapper.keys.single()] as List<*>
            TypedInstance.from(type, collection, schema)
         }
         else -> {
            TypedInstance.from(type, raw, schema)
         }
      }
      return typedInstance
   }

   fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema): TypedInstance? {
      require(value is String) { "Expected Xml parsed from String" }
      return parseWithJackson(value,type, schema)
   }


}
