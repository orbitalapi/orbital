package io.osmosis.polymer.query

import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphBuilder
import es.usc.citius.hipster.model.Transition
import es.usc.citius.hipster.model.impl.WeightedNode
import es.usc.citius.hipster.model.problem.ProblemBuilder
import io.osmosis.polymer.*
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.junit.Test

// Note : At the time of writing this test,
// I'm exploring JHipser as a solver.
// Will plug in full Polymer parsing once experiment is complete
typealias TypeName = String
class JHipsterTests {

   val schema = TaxiSchema.from("""
type alias ClientId as String
type Invoice {
   clientId : ClientId
}
type Client {
   clientId : ClientId
   clientName : ClientName as String
}
service ClientLookupService {
   operation findClient(ClientId):Client
}
""")



   val invoice = TypedObject.fromAttributes("Invoice", mapOf("clientId" to "client123"), schema)
   val client = TypedObject.fromAttributes("Client", mapOf("clientId" to "client123", "clientName" to "Jimmy"), schema)
   val graph = GraphBuilder.create<Element, Relationship>()
      .connect(type("Invoice")).to(member("Invoice/clientId")).withEdge(Relationship.HAS_ATTRIBUTE)
      .connect(member("Invoice/clientId")).to(type("ClientId")).withEdge(Relationship.IS_TYPE_OF)
      .connect(type("ClientId")).to(member("Client/clientId")).withEdge(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
      .connect(member("Client/clientId")).to(type("Client")).withEdge(Relationship.IS_ATTRIBUTE_OF)
      .connect(type("ClientId")).to(operation("ClientLookupService")).withEdge(Relationship.IS_PARAMETER_ON)
      .connect(type("Client")).to(member("Client/clientName")).withEdge(Relationship.HAS_ATTRIBUTE)
      .connect(member("Client/clientName")).to(type("ClientName")).withEdge(Relationship.IS_TYPE_OF)
      .connect(operation("ClientLookupService")).to(type("Client")).withEdge(Relationship.PROVIDES)
      .createDirectedGraph()

   @Test
   fun given_aRequiredNodeIsNotPresent_but_canBeDiscovered_then_the_discoverySolutionIsProvided() {

      // Try thinking about this as a maze, rather than a graph.
      // The relationships between nodes defines the "moves" that can be
      // made from a given node.
      // By passing this into a maze solver, rather than a graph solver,
      // we can evaluate if a given move is permissable at query time, rather than
      // when the node is built.
      val context = emptyMap<TypeName, TypedInstance>()
      val searchProblem = ProblemBuilder.create()
         .initialState(instance(invoice))
         .defineProblemWithExplicitActions()
         .useActionFunction({ element ->
            // Find all the relationships that we consider traversable right now
            val edges = graph.outgoingEdgesOf(element.graphNode())
            edges.filter { graphEdge ->
               graphEdge.edgeValue.canBeEvaluated(graphEdge.vertex1, graphEdge.vertex2, context)
            }.map { it.edgeValue }
         })
         .useTransitionFunction { relationship, element ->
            val edge = graph.outgoingEdgesOf(element.graphNode()).first { it.edgeValue == relationship }
            edge.edgeValue.evaluate(element, edge.vertex2)
         }
         .useCostFunction { transition: Transition<Relationship, Element>? ->
            1.0
         }
         .build()

      val targetType = "ClientName"
      val searchCompletedFn = { node: WeightedNode<Relationship, Element, Double> ->

         val state = node.state()
         val matched = state.elementType == ElementType.INSTANCE && (state.value as TypedInstance).type.name.fullyQualifiedName == targetType
         if (matched) {
            TODO()
         }
         matched
      }
      val result = Hipster.createAStar(searchProblem).search(searchCompletedFn)

      TODO()
   }

   private fun Relationship.evaluate(from: Element, to: Element): Element {
      return when (this) {
         Relationship.HAS_ATTRIBUTE -> to
         Relationship.IS_TYPE_OF -> to
         Relationship.IS_PARAMETER_ON -> to
         Relationship.PROVIDES -> invokeService(from,to)
         else -> TODO()
      }
   }

   private fun invokeService(from: Element, to: Element): Element {
      // This is a test, so we only handle one kind of function...
      assert(to.value == "Client")
      return instance(client)
   }

   private fun Relationship.canBeEvaluated(from: Element, to: Element, context: Map<TypeName, TypedInstance>): Boolean {
      if (this == Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE) {
         // TODO : This should check more thoroughly -- ie., is the actual instance?
         return context.containsKey(to.value)
      } else {
         // TODO
         return true
      }
   }

}
