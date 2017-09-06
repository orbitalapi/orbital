package io.osmosis.polymer

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import es.usc.citius.hipster.graph.GraphBuilder
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.schemas.*
import io.osmosis.polymer.utils.log

enum class ElementType {
   TYPE,
   MEMBER,
   SERVICE,
   // An instance is something we have a real actual instance of
   INSTANCE,
   // A provided instance is something we expect to discover via the search,
//    but is not known at the start of the search.
   PROVIDED_INSTANCE,

   // Keep MEMBER and PROVIDED_INSTANCE_MEMBER seperate, as
   // in many cases we ONLY want to traverse from a member where actually have
   // been given an instance.  If we treat them as the same elementType, then links
   // will get created between nodes forming incorrect paths.
   // (Note - that's a theory, I haven't tested it, so this could be over complicating)
   PROVIDED_INSTANCE_MEMBER,
   PARAMETER;

   override fun toString(): String {
      return super.toString().toLowerCase().capitalize()
   }
}


data class Element(val value: Any, val elementType: ElementType) {

   fun graphNode(): Element {
      return if (this.elementType == ElementType.INSTANCE) {
         val typeName = (value as TypedInstance).type.name.fullyQualifiedName
         Element(typeName, ElementType.TYPE)
      } else {
         this
      }
   }

   fun valueAsQualifiedName(): QualifiedName {
      return value.toString().fqn()
   }

   override fun toString(): String {
      // Use  graphNode() so we don't dump the full value to the logs, which is boring to read
      return "$elementType(${graphNode().value})"
   }
}

fun type(name: String) = Element(name, ElementType.TYPE)
fun type(type: Type) = type(type.fullyQualifiedName)
fun member(name: String) = Element(name, ElementType.MEMBER)
fun parameter(operationName: String, index: Int) = Element("$operationName/param/$index", ElementType.PARAMETER)
fun operation(name: String) = Element(name, ElementType.SERVICE)
fun providedInstance(name: String) = Element(name, ElementType.PROVIDED_INSTANCE)
fun providedInstanceMember(name:String) = Element(name, ElementType.PROVIDED_INSTANCE_MEMBER)
fun instance(value: TypedInstance) = Element(value, ElementType.INSTANCE)

typealias TypeElement = Element
typealias MemberElement = Element

class PolymerGraphBuilder(val schema: Schema) {
   fun build(facts: Set<TypedInstance> = emptySet()): HipsterDirectedGraph<Element, Relationship> {
      val builder = GraphBuilder.create<Element, Relationship>()
      val typesAndWhereTheyreUsed = appendTypes(builder, schema)
      appendServices(builder, schema, typesAndWhereTheyreUsed)
      appendInstances(builder, facts, schema)
      return builder.createDirectedGraph()
   }

   private fun appendInstances(builder: GraphBuilder<Element, Relationship>, facts: Set<TypedInstance>, schema: Schema) {
      facts.forEach { typedInstance ->
         builder.connect(instance(typedInstance)).to(type(typedInstance.type)).withEdge(Relationship.IS_INSTANCE_OF)
      }
   }

   private fun appendTypes(builder: GraphBuilder<Element, Relationship>, schema: Schema): Multimap<TypeElement, MemberElement> {
      val typesAndWhereTheyreUsed: Multimap<TypeElement, MemberElement> = ArrayListMultimap.create()
      schema.types.forEach { type: Type ->

         val typeFullyQualifiedName = type.fullyQualifiedName
         val typeNode = type(typeFullyQualifiedName)
         type.attributes.map { (attributeName, attributeType) ->
            val attributeQualifiedName = attributeFqn(typeFullyQualifiedName, attributeName)
            val attributeNode = member(attributeQualifiedName)
            builder.connect(typeNode).to(attributeNode).withEdge(Relationship.HAS_ATTRIBUTE)

            // (attribute) -[IS_ATTRIBUTE_OF]-> (type)
            builder.connect(attributeNode).to(typeNode).withEdge(Relationship.IS_ATTRIBUTE_OF)

            val attributeTypeNode = type(attributeType.fullyQualifiedName)
            builder.connect(attributeNode).to(attributeTypeNode).withEdge(Relationship.IS_TYPE_OF)
            typesAndWhereTheyreUsed.put(attributeTypeNode, attributeNode)
            // See the relationship for why commented out ....
            // migrating this relationship to an INSTNACE_OF node.
//            builder.connect(attributeTypeNode).to(attributeNode).withEdge(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
         }
         log().debug("Added attribute ${type.name} to graph")
      }
      return typesAndWhereTheyreUsed
   }

   private fun attributeFqn(typeFullyQualifiedName: String, attributeName: AttributeName): String {
      return "$typeFullyQualifiedName/$attributeName"
   }

   private fun appendServices(builder: GraphBuilder<Element, Relationship>, schema: Schema, typesAndWhereTheyreUsed: Multimap<TypeElement, MemberElement>) {
      return schema.services.forEach { service: Service ->
         service.operations.forEach { operation: Operation ->
            val operationReference = "${service.qualifiedName}@@${operation.name}"
            val operationNode = operation(operationReference)
            operation.parameters.forEachIndexed { index, parameter ->
               // When building services, we need to use 'connector nodes'
               // as Hipster4J doesn't support identical vertex pairs with seperate edges.
               // eg: Service -[requiresParameter]-> Money && Service -[Provides]-> Money
               // isn't supported, and results in the Edge for the 2nd pair to remain undefined
               val typeFqn = parameter.type.fullyQualifiedName
               builder.connect(operationNode).to(parameter(operationReference, index)).withEdge(Relationship.REQUIRES_PARAMETER)
               builder.connect(parameter(operationReference, index)).to(type(typeFqn)).withEdge(Relationship.REQUIRES_PARAMETER)

               builder.connect(type(typeFqn)).to(parameter(operationReference, index)).withEdge(Relationship.IS_PARAMETER_ON)
               builder.connect(parameter(operationReference, index)).to(operationNode).withEdge(Relationship.IS_PARAMETER_ON)
            }

            // Build the instance.
            // It connects to it's type, but also to the attributes that are
            // now traversable, as we have an actual instance of the thing
            val resultInstanceFqn = operation.returnType.fullyQualifiedName
            builder.connect(operationNode).to(providedInstance(resultInstanceFqn)).withEdge(Relationship.PROVIDES)
//            builder.connect(providedInstance(resultInstanceFqn)).to(type(resultInstanceFqn)).withEdge(Relationship.IS_INSTANCE_OF)

            schema.type(resultInstanceFqn).attributes.forEach { attributeName, typeReference ->
               val typeElement = type(typeReference.fullyQualifiedName)
               val typeUsageSites = typesAndWhereTheyreUsed.get(typeElement)
               val providedInstanceMember = providedInstanceMember(attributeFqn(resultInstanceFqn, attributeName))
               builder.connect(providedInstanceMember).to(type(typeReference.fullyQualifiedName)).withEdge(Relationship.IS_INSTANCE_OF)
               builder.connect(providedInstance(resultInstanceFqn)).to(providedInstanceMember).withEdge(Relationship.INSTANCE_HAS_ATTRIBUTE)
               typeUsageSites.forEach { member ->
                  if (member.valueAsQualifiedName() != providedInstanceMember.valueAsQualifiedName()) {
                     builder.connect(providedInstanceMember).to(member).withEdge(Relationship.INSTANCE_HAS_ATTRIBUTE)
                  }
               }

            }


            log().debug("Added operation $operationReference to graph")
         }
      }
   }

}
