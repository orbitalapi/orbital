package io.vyne.utils.xml

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import reactor.core.publisher.Flux
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

class XmlDocumentProvider(private val elementSelector: String? = null) {
   private val factory = DocumentBuilderFactory.newInstance()
   private val builder = factory.newDocumentBuilder()
   private val xpathFactory = XPathFactory.newInstance()
   private val xpathCache: LoadingCache<String, XPathExpression> = CacheBuilder.newBuilder()
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .build(object : CacheLoader<String, XPathExpression>() {
         override fun load(key: String): XPathExpression {
            val xpath = xpathFactory.newXPath()
            return xpath.compile(key)
         }
      })

   fun parseXmlStream(input: Flux<InputStream>): Flux<Document> {
      return input
         .map { stream -> builder.parse(stream) }
         .map { document ->
            // when
            when (elementSelector) {
               null -> listOf(document)
               else -> {
                  val xpath = xpathCache.get(elementSelector)
                  val result = xpath.evaluate(document, XPathConstants.NODESET) as NodeList
                  (0 until result.length).map {
                     val elementDocument = builder.newDocument()
                     val individualDocumentContent = elementDocument.importNode(result.item(it), true)
                     elementDocument.appendChild(individualDocumentContent)
                     elementDocument
                  }
               }
            }
         }.flatMapIterable { it }
   }
}
