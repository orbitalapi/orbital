package io.vyne.pipelines.jet.pipelines

import io.vyne.pipelines.jet.api.DagDataset
import io.vyne.pipelines.jet.api.DagGraphLink
import io.vyne.pipelines.jet.api.DagGraphNode

object DotVizUtils {
   fun dotVizToGraphNodes(dotViz: String): DagDataset {
      val linksText = dotViz.removeSurrounding("digraph Pipeline {", "}")
      val elements = mutableSetOf<DagGraphNode>()
      val links = mutableSetOf<DagGraphLink>()
      linksText.lines()
         .filter { it.isNotBlank() && it.isNotEmpty() }
         .forEach { line ->
            val members = line.split("->")
               .map {
                  val entryText = it
                     .trim()
                     .removeSuffix(";")
                     .removeSurrounding("\"")
                  DagGraphNode(entryText.browserSafeId(), entryText)
               }
            val source = members[0]
            val target = members[1]
            elements.addAll(members)
            links.add(DagGraphLink(source.id, target.id, ""))
         }
      return DagDataset(elements.toList(), links.toList())
   }

   private fun String.browserSafeId(): String {
      return this
         .replace(" ", "")
         .replace(".", "")
         .replace("/", "")
         .replace("(", "")
         .replace(")", "")
         .replace("_", "")
         .replace("-", "")
         .replace("@", "")
   }
}



