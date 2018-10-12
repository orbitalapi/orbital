package io.vyne

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.Direction
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.orient.OrientEdge
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import io.osmosis.polymer.schemas.*
import io.osmosis.polymer.utils.log

class OrientDbGraphSearcher(private val orientGraph: OrientGraphNoTx) : SchemaPathResolver {
   override fun findPath(start: String, target: String): Path {
      return findPath(start.fqn(), target.fqn())
   }

   override fun findPath(start: Type, target: Type): Path = findPath(start.name, target.name)

   override fun findPath(start: QualifiedName, target: QualifiedName): Path {

      log().debug("Searching for path from $start -> $target")
      // TODO : This is very hacky
      val startNode = orientGraph.getVertices(GraphAttributes.QUALIFIED_NAME, start.fullyQualifiedName).toList().firstOrNull() ?: throw IllegalArgumentException("${start.fullyQualifiedName} is not present within the orientGraph")
      val endNode = orientGraph.getVertices(GraphAttributes.QUALIFIED_NAME, target.fullyQualifiedName).toList().firstOrNull() ?: throw IllegalArgumentException("${target.fullyQualifiedName} is not present within the orientGraph")
      val sql = """
SELECT expand(path) FROM (
  SELECT shortestPath(${startNode.id}, ${endNode.id}) AS path
  UNWIND path
)
         """.trim()
      val path = orientGraph.command(OCommandSQL(sql)).execute<Iterable<OrientVertex>>().toList()
      val links = convertToLinks(path)
      log().debug(sql)
      val resolvedPath = Path(start, target, links)
      if (resolvedPath.exists) {
         log().debug("Path from $start -> $target found with ${resolvedPath.links.size} links")
         log().debug(resolvedPath.description)
      } else {
         log().debug("Path from $start -> $target not found")
      }
      return resolvedPath
   }

   private fun convertToLinks(path: List<OrientVertex>): List<Link> {
      val links = path.mapIndexed { index, vertex ->
         if (index + 1 >= path.size) {
            null
         } else {
            val fromNode = vertex
            val toNode = path[index + 1]
            val edge = fromNode.getEdges(toNode, Direction.BOTH).toList().firstOrNull() as OrientEdge ?: throw IllegalStateException("No edge found from ${fromNode[GraphAttributes.QUALIFIED_NAME]} -> ${toNode[GraphAttributes.QUALIFIED_NAME]}, but they were adjoining nodes in the shortestPath query")

            val inboundNode = edge.inVertex as ODocument
            val outboundNode = edge.outVertex as ODocument
            val relationship = Relationship.valueOf(edge.label)
            Link(outboundNode.field<String>(GraphAttributes.QUALIFIED_NAME).fqn(), relationship, inboundNode.field<String>(GraphAttributes.QUALIFIED_NAME).fqn())
         }

      }.filterNotNull()
      return links
   }

   private operator fun Vertex.get(propertyName: String): String {
      return this.getProperty(propertyName)
   }

}
