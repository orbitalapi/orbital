package com.orbitalhq.formats.xml

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

   fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema): TypedInstance? {
      require(value is String) { "Expected Xml parsed from String" }
      val document = builder.parse(value.byteInputStream())
      val element = document.documentElement
      val parsed = parseNode(element, type, schema)?.let {
         TypedInstance.from(type, it, schema)
      }
      return parsed
   }

   // Returns either a List<Map<String,Any>>, a Map<String,Any>, or a scalar value
   private fun parseNode(element: Node, type: Type, schema: Schema): Any? {
      val children = element.childNodes.asList()
      val nodesByName = children.associateBy { it.nodeName }

      val parsed: Any? = when {
         type.isCollection -> {
            children.filter {
               it.nodeType == Node.ELEMENT_NODE
            }.map { childNode ->
               val parsedChild = parseNode(childNode, type.collectionType!!, schema)
               parsedChild
            }
         }

         type.isScalar -> {
            val childNodes = children
               .filter { it.nodeType == Node.TEXT_NODE }

            if (childNodes.size == 1) {
               childNodes.single().textContent
            } else {
               logger.info { "Parsing of type ${type.name.shortDisplayName} from Xml content failed, as there were multiple nodes present" }
               null
            }
         }

         else -> {
            // Parsing a TypedObject
            type.attributes.mapNotNull { (fieldName, field) ->
               when {
                  nodesByName.containsKey(fieldName) -> fieldName to parseNode(
                     nodesByName[fieldName]!!,
                     schema.type(field.type),
                     schema
                  )

                  field.hasMetadata(XmlAnnotationSpec.XmlAttributeName) -> {
                     fieldName to element.attributes.getNamedItem(fieldName)?.textContent
                  }

                  else -> null
               }
            }.toMap()
         }
      }
      return parsed
   }
}

fun NodeList.asList(): List<Node> {

   val list = mutableListOf<Node>()
   for (i in 0 until this.length) {
      list.add(this.item(i))
   }
   return list
}
