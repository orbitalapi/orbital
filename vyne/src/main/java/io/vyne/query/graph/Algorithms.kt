package io.vyne.query.graph

import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphSearchProblem
import io.vyne.HipsterGraphBuilder
import io.vyne.schemas.Operation
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.synonymFullyQualifiedName
import lang.taxi.types.EnumType

object Algorithms {
   fun accessibleFromThroughFunctionInvocations(schema: Schema, fqn: String): Set<Type> {
      val startingType = schema.type(fqn)
      val builder = HipsterGraphBuilder.create<Type, Operation>()
      val connections = schema.operationsWithSingleArgument().flatMap { (_, operation) ->
         val argument = operation.parameters.first().type

         resolveSynonym(argument, schema).map { synonymType ->
            HipsterGraphBuilder.Connection(synonymType,operation.returnType,operation)
         } + HipsterGraphBuilder.Connection(argument,operation.returnType,operation)

      }
      val graph = builder.createDirectedGraph(connections)
      val problem = GraphSearchProblem
         .startingFrom(startingType)
         .`in`(graph)
         .takeCostsFromEdges()
         .build()

      val targetVertices = graph.vertices().filter { it != startingType }

      val solutions = targetVertices.mapNotNull { target ->
         val solution = Hipster.createAStar(problem).search(target)
         if (solution.goalNode.state() == target) {
            solution
         } else {
            null
         }
      }
      return solutions
         .map { solution -> solution.goalNode }
         .flatMap { it -> it.path() }
         .map { it.state() }
         .filter { it != startingType }
         .toSet()
   }


   private fun resolveSynonym(type: Type, schema: Schema): Set<Type> {
      return if (type.isEnum) {
         val underlyingEnumType = type.taxiType as EnumType
         underlyingEnumType.values.flatMap { enumValue ->
            enumValue.synonyms.map { synonym -> schema.type(synonym.synonymFullyQualifiedName()) }
         }.toSet()
      } else {
         setOf()
      }
   }
}


