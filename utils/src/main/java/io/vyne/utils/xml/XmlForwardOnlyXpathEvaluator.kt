package io.vyne.utils.xml

import java.io.InputStream
import javax.xml.stream.XMLInputFactory

class XmlForwardOnlyXpathEvaluator(xpaths: List<String>) {
   companion object {
      private val inputFactory = XMLInputFactory.newInstance()
      /**
       * Returns a list of Xpath expressions that are not supported, or
       * an empty list if everything is supported.
       */
      fun getUnsupported(xpaths: List<String>): List<String> {
         return emptyList()
      }
   }

   fun evaluate(input: InputStream, handler: (xpath: String, value: Any) -> Unit) {
      val reader = inputFactory.createXMLStreamReader(input)
      while (reader.hasNext()) {
         TODO()
      }
   }
}
