package io.osmosis.polymer

import com.orientechnologies.orient.core.command.OCommandOutputListener
import com.orientechnologies.orient.core.db.tool.ODatabaseExport
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import io.osmosis.polymer.utils.log
import org.apache.commons.io.FileUtils
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.File

class DbExporter(val db: OrientGraph) {

   fun export(path: String? = null): String {
      val lines = mutableListOf<String>()
      val listener = OCommandOutputListener { iText ->
         lines.add(iText)
         print(iText)
      }

      val stream = ByteArrayOutputStream()
      val export = ODatabaseExport(db.rawGraph, stream, listener)
      export.exportDatabase()
      export.close()
      val result = stream.toString()
      if (path != null) {
         FileUtils.writeStringToFile(File(path), result)
         log().debug("Exported to $path")
      }
      return result
   }
}
