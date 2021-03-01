package io.vyne.utils.xml

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.ximpleware.AutoPilot
import com.ximpleware.VTDGen
import com.ximpleware.VTDNav
import io.vyne.utils.batchTimed
import io.vyne.utils.timed
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

class XmlDocumentProvider(private val elementSelector: String? = null) {
   companion object {
      private val factory = DocumentBuilderFactory.newInstance()
      private val builder = factory.newDocumentBuilder()
      private val xpathFactory = XPathFactory.newInstance()
      private val xpathCache: LoadingCache<String, XPathExpression> = CacheBuilder.newBuilder()
         .expireAfterAccess(5, TimeUnit.MINUTES)
         .build(object : CacheLoader<String, XPathExpression>() {
            override fun load(key: String): XPathExpression {
               return timed("xpath compilation") {
                  val xpath = xpathFactory.newXPath()
                  xpath.compile(key)
               }

            }
         })
   }

   fun parseXmlStreamToVTDNav(input: InputStream): List<VTDNav> {
      // TODO : This is a very heavy way of parsing XML content, we need
      // to evaluate a streaming approch now-ish.
      return when (elementSelector) {
         null -> {
            val doc = VTDGen()
            doc.setDoc(input.readBytes())
            doc.parse(true)
            listOf(doc.nav)
         }
         else -> {
            batchTimed("xpath stuff") {
               val doc = VTDGen()
               doc.setDoc(input.readBytes())
               doc.parse(true)
               val navigator = doc.nav
               val ap = AutoPilot(navigator)
               ap.selectXPath(elementSelector)
               val navigatorList = mutableListOf<VTDNav>()
               var index: Int
               while ({ index = ap.evalXPath(); index }() != -1) {
                  val subElementBytes = navigator.elementFragmentNs.toBytes()
                  val subDoc = VTDGen()
                  subDoc.setDoc(subElementBytes)
                  subDoc.parse(true)
                  navigatorList.add(subDoc.nav)
               }
               navigatorList
            }
         }
      }
   }

   fun parseXmlStream(input: InputStream): List<Document> {
      // TODO : This is a very heavy way of parsing XML content, we need
      // to evaluate a streaming approch now-ish.
      val document = batchTimed("XmlDocumentProvider.builder.parse") { builder.parse(input) }
      return when (elementSelector) {
         null -> listOf(document)
         else -> {
            batchTimed("xpath stuff") {
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
      }
   }
}
