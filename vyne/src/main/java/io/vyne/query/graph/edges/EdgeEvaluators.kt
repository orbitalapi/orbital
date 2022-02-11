package io.vyne.query.graph.edges

import com.fasterxml.jackson.annotation.JsonIgnore
import es.usc.citius.hipster.graph.GraphEdge
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.graph.Element
import io.vyne.query.graph.ElementType
import io.vyne.query.graph.GraphConnection
import io.vyne.query.graph.instanceOfType
import io.vyne.schemas.Relationship
import io.vyne.schemas.fqn
import io.vyne.utils.assertingThat


fun GraphEdge<Element, Relationship>.description(): String {
   return "${this.vertex1.valueAsQualifiedName().name} -[${this.edgeValue.description}]-> ${this.vertex2.valueAsQualifiedName().name}"
}

// TODO : Come up with a better name for this...
// I need an interface because I want to be able to recurse backwards,
// but need to have a start point.  Struggling a bit with this right now.
interface PathEvaluation {
   val resultValue: TypedInstance?
   val element: Element
}

data class StartingEdge(
   override val resultValue: TypedInstance,
   override val element: Element
) : PathEvaluation

data class EvaluatableEdge(
   @JsonIgnore
   val previous: PathEvaluation,
   @JsonIgnore
   val relationship: Relationship,
   @JsonIgnore
   val target: Element
) {
   @JsonIgnore
   val vertex1: Element = previous.element

   @JsonIgnore
   val vertex2 = target;

   @JsonIgnore
   val previousValue: TypedInstance? = previous.resultValue

   val connection = GraphConnection(vertex1, vertex2, relationship)

   val description = "$vertex1 -[${relationship}]-> $vertex2"

   fun success(value: TypedInstance?): EvaluatedEdge {
      // TODO : Are we adding any value by having "target" here? -- isn't it always vertex2?
      // If so, just remove it - as it's inferrable from 'previous'
      return EvaluatedEdge.success(this, target, value)
   }

   fun failure(value: TypedInstance?, failureReason: String = "Error"): EvaluatedEdge {
      // TODO : Are we adding any value by having "target" here? -- isn't it always vertex2?
      // If so, just remove it - as it's inferrable from 'previous'
      return EvaluatedEdge(this, target, value, failureReason)
   }
}

class RequiresParameterEdgeEvaluator(val parameterFactory: ParameterFactory = ParameterFactory()) : EdgeEvaluator {
   override val relationship: Relationship = Relationship.REQUIRES_PARAMETER

   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      if (edge.target.elementType == ElementType.PARAMETER) {
         // Pass through, the next vertex should be the param type
         return EvaluatedEdge.success(edge, edge.vertex2, edge.previousValue)
      }
      assert(
         edge.vertex2.elementType == ElementType.TYPE
      ) { "RequiresParameter must be evaluated on an element type of Type, received ${edge.description}" }

      // Normally, we'd just use vertex2 to tell us the type.
      // But, because Hispter4J doesn't support identical Vertex Pairs, we can't.
      // (See usages of the parameter() function for more details)
      // So, look up the service directly, and get the parameter type from the schema
      val parts = (edge.vertex1.value as String).split("/").assertingThat({ it.size == 3 })
      val operationReference = parts[0]
      val paramIndex = Integer.parseInt(parts[2])
      val (_, operation) = context.schema.operation(operationReference.fqn())
      val paramType = operation.parameters[paramIndex].type

      val discoveredParam = parameterFactory.discover(paramType, context, null, operation)
      return EvaluatedEdge.success(edge, instanceOfType(discoveredParam.type), discoveredParam)

      //return  parameterFactory.discover(paramType, context, operation).map {
      //   EvaluatedEdge.success(edge, instanceOfType(it.type), it)
      //}

   }
}


abstract class PassThroughEdgeEvaluator(override val relationship: Relationship) : EdgeEvaluator {
   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      return edge.success(edge.previousValue)
   }
}

class AttributeOfEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_ATTRIBUTE_OF)
class IsTypeOfEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_TYPE_OF)
class HasParamOfTypeEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
class OperationParameterEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_PARAMETER_ON)
class IsInstanceOfEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_INSTANCE_OF)
class CanPopulateEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.CAN_POPULATE)
class ExtendsTypeEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.EXTENDS_TYPE)
class EnumSynonymEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_SYNONYM_OF)

class InstanceHasAttributeEdgeEvaluator : AttributeEvaluator(Relationship.INSTANCE_HAS_ATTRIBUTE)

// Note: I suspect this might cause problems.
// I'm using this because in a solution path, I receive a value from the server, but still end up
// evaluating a HasAttribute, rather than InstanceHasAttribute. (see TradeComplianceTest.canFindTraderMaxValue).
// I have a feeling that I'm going to hit issues here when I'm evluating some paths PRIOR to fetching values.
// TODO.
class HasAttributeEdgeEvaluator : AttributeEvaluator(Relationship.HAS_ATTRIBUTE)
