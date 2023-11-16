package com.orbitalhq.formats.xml

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.models.xml.XmlParsedList
import com.orbitalhq.models.xml.XmlParsedMap
import com.orbitalhq.models.xml.XmlTypedInstanceParser
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.w3c.dom.Document
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

object XmlFormatDeserializer : ModelFormatDeserializer {
   private val deserializer = XmlDeserializer()
   override fun canParse(value: Any, metadata: Metadata): Boolean = true

   override fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema, source: DataSource): Any =
      deserializer.parse(value, type, metadata, schema, source) ?: error("Parsing XML from root returned null")
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

   private fun parseWithJackson(value: String, type: Type, schema: Schema, source: DataSource): TypedInstance {
      // This is expensive, and needs rethinking.
      // We use jackson parsing because it's simple, and maps nicely to a Map<String,Any>,
      // so the user doesn't have to litter with by xpath(..) on all attributes.
      // However, when the use has used by xpath() for some reason, we need access to the XML Doc
      // The obvious resolution is not to use Jackson, but that needs rework.
      val raw:Any = xmlMapper.readValue(value)
      val xmlDocument = builder.parse(IOUtils.toInputStream(value, Charset.defaultCharset()))

      val typedInstance = when {
         type.isCollection && raw is Collection<*> -> {
            val xmlParsedStructure = XmlParsedList(raw as List<Map<String,Any>>, xmlDocument)
            TypedInstance.from(type, xmlParsedStructure, schema, source = source)
         }
         type.isCollection && isXmlCollectionWrapper(raw) -> {
            val collectionWrapper = raw as Map<*, *>
            val collection = collectionWrapper[collectionWrapper.keys.single()] as List<*>
            val xmlParsedStructure = XmlParsedList(collection as List<Map<String,Any>>, xmlDocument)
            TypedInstance.from(type, xmlParsedStructure, schema, source = source)
         }
         else -> {
            val xmlParsedStructure = XmlParsedMap(raw as Map<String,Any>, xmlDocument)
            TypedInstance.from(type, xmlParsedStructure, schema, source = source)
         }
      }
      return typedInstance
   }

   fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema, source: DataSource = UndefinedSource): TypedInstance {
      require(value is String) { "Expected Xml parsed from String, but received ${value::class.simpleName}" }
      return parseWithJackson(value,type, schema, source = source)
   }


}

