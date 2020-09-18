package io.vyne.query.graph

import com.google.common.cache.CacheBuilder
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import io.vyne.DisplayGraphBuilder
import io.vyne.HipsterGraphBuilder
import io.vyne.models.TypedInstance
import io.vyne.schemas.*

enum class ElementType {
   TYPE,
   MEMBER,
   OPERATION,

   // An instance is something we have a real actual instance of.
   // These are available before a search is commenced.
//   INSTANCE,
   // An instance of a type, expected to be discovered via the search,
//    but is not known at the start of the search.
   TYPE_INSTANCE,

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


data class Element(val value: Any, val elementType: ElementType, val instanceValue: Any? = null) {

   fun graphNode(): Element {
//      return if (this.elementType == ElementType.INSTANCE) {
//         val typeName = (value as TypedInstance).type.name.fullyQualifiedName
//         Element(typeName, ElementType.TYPE)
//      } else {
//         this
//      }
      return this
   }

   fun label(): String {
      return when (elementType) {
         ElementType.TYPE -> valueAsQualifiedName().name
         ElementType.MEMBER -> value.toString().split(".").last()
         ElementType.OPERATION -> value.toString().split(".").takeLast(2).joinToString("/")
         else -> value.toString()
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

fun Type.asElement(): Element = type(this)
fun Operation.asElement(): Element = operation(this.qualifiedName.fullyQualifiedName)
fun type(name: String) = Element(name, ElementType.TYPE)
fun type(type: Type): Element {
   return type(type.qualifiedName.parameterizedName)
}

fun member(name: String) = Element(name, ElementType.MEMBER)
fun parameter(paramTypeFqn: String) = Element(ParamNames.toParamName(paramTypeFqn), ElementType.PARAMETER)
fun operation(service: Service, operation: Operation): Element {
   val operationReference = OperationNames.name(service.qualifiedName, operation.name)
   return operation(operationReference)
}

fun operation(name: String) = Element(name, ElementType.OPERATION)
fun providedInstance(typedInstance: TypedInstance): Element {
   val instanceHash = typedInstance.value?.hashCode() ?: -1
   val nodeId = typedInstance.typeName + "@$instanceHash"
   return providedInstance(nodeId,typedInstance)
}
fun providedInstance(name: String, value: Any? = null) = Element(name, ElementType.TYPE_INSTANCE, value)
fun providedInstanceMember(name: String) = Element(name, ElementType.PROVIDED_INSTANCE_MEMBER)

// Note : We don't actually append the value itself to the graph, otherwise the instance node
// becomes unattainable (ie., when searching with a startNode: typedInstance(someName), it won't find entries
// added as typedInstance(someName, value).
// Might need to rethink this.  Should we add the typedInstance with a link of instanceValue?
fun instanceOfType(type: Type): Element {
   val qualifiedName = QualifiedName(type.fullyQualifiedName, type.typeParametersTypeNames)
   return providedInstance(qualifiedName.parameterizedName)
} // Element(value.type.fullyQualifiedName, ElementType.TYPE_INSTANCE, value)
//fun instance(value: TypedInstance) = providedInstance(value.type.fullyQualifiedName, value) // Element(value.type.fullyQualifiedName, ElementType.TYPE_INSTANCE, value)

typealias TypeElement = Element
typealias MemberElement = Element

private data class GraphWithFactTypesCacheKey(val facts:Set<Type>, val graphBuilder:HipsterGraphBuilder<Element,Relationship>)
private data class GraphWithFactInstancesCacheKey(val facts:List<TypedInstance>, val excludedEdges: List<EvaluatableEdge>, val graphBuilder:HipsterGraphBuilder<Element,Relationship>)
class VyneGraphBuilder(private val schema: Schema) {


   private val baseSchemaCache = CacheBuilder.newBuilder()
      .maximumSize(100) // arbitary, can tune later
      .build<Int, HipsterGraphBuilder<Element, Relationship>>()

   // experiment: migrating to graphWitFactInstances -- not sure why we used types here.
   private val graphWithFactTypesCache = CacheBuilder.newBuilder()
      .maximumSize(100)
      .build<GraphWithFactTypesCacheKey, HipsterDirectedGraph<Element,Relationship>>()

   // experiment: Why were we using types, instead of instances?
   private val graphWithFactInstancesCache = CacheBuilder.newBuilder()
      .maximumSize(100)
      .build<GraphWithFactInstancesCacheKey, HipsterDirectedGraph<Element,Relationship>>()

   fun build(facts:List<TypedInstance>, excludedOperations: Set<QualifiedName> = emptySet(), excludedEdges:List<EvaluatableEdge>): HipsterDirectedGraph<Element, Relationship> {
      val builder = baseSchemaCache.get(excludedOperations.hashCode()) {
         val instance = HipsterGraphBuilder.create<Element, Relationship>()
         appendTypes(instance, schema)
         appendServices(instance, schema, excludedOperations)
         instance
      }
      val graphWithFacts = graphWithFactInstancesCache.get(GraphWithFactInstancesCacheKey(facts,excludedEdges, builder)) {
         val thisBuilder = builder.copy()
         appendInstances(thisBuilder, facts, schema, excludedEdges)
         thisBuilder.createDirectedGraph(excludedEdges)
      }

      return graphWithFacts

   }
   fun build(types: Set<Type> = emptySet(), excludedOperations: Set<QualifiedName> = emptySet()): HipsterDirectedGraph<Element, Relationship> {
      val builder = baseSchemaCache.get(excludedOperations.hashCode()) {
         val instance = HipsterGraphBuilder.create<Element, Relationship>()
         appendTypes(instance, schema)
         appendServices(instance, schema, excludedOperations)
         instance
      }


      val graphWithFacts = graphWithFactTypesCache.get(GraphWithFactTypesCacheKey(types,builder)) {
         val thisBuilder = builder.copy()
         appendInstanceTypes(thisBuilder, types, schema)
         thisBuilder.createDirectedGraph()
      }

      return graphWithFacts
   }

   fun buildDisplayGraph(): HipsterDirectedGraph<Element, Relationship> {
      val graph = build()
      return DisplayGraphBuilder().convertToDisplayGraph(graph)
   }

   private fun appendInstanceTypes(builder: HipsterGraphBuilder<Element, Relationship>, types: Set<Type>, schema: Schema) {
      types.forEach {
         val typeFqn = it.qualifiedName.parameterizedName
         appendProvidedInstances(builder, typeFqn, schema)
         // Note: An old implementation has been removed from here.  Check the git history
         // if we think stuff has broken.

      }
   }

   private fun appendInstances(builder: HipsterGraphBuilder<Element, Relationship>, instances: List<TypedInstance>, schema: Schema, excludedEdges: List<EvaluatableEdge>) {
      instances.forEach {typedInstance ->
         appendProvidedInstances(builder, typedInstance.typeName, schema, value = typedInstance)
      }
   }

   private fun appendTypes(builder: HipsterGraphBuilder<Element, Relationship>, schema: Schema): Multimap<TypeElement, MemberElement> {
      val typesAndWhereTheyreUsed: Multimap<TypeElement, MemberElement> = ArrayListMultimap.create()
      schema.types.forEach { type: Type ->

         val typeFullyQualifiedName = type.fullyQualifiedName
         val typeNode = type(typeFullyQualifiedName)

         type.inherits.forEach { inheritedType ->
            builder.connect(typeNode).to(type(inheritedType)).withEdge(Relationship.EXTENDS_TYPE)
         }

         if (!type.isClosed) {
            type.attributes.map { (attributeName, attributeType) ->
               val attributeQualifiedName = attributeFqn(typeFullyQualifiedName, attributeName)
               val attributeNode = member(attributeQualifiedName)
               builder.connect(typeNode).to(attributeNode).withEdge(Relationship.HAS_ATTRIBUTE)

               // (attribute) -[IS_ATTRIBUTE_OF]-> (type)
               builder.connect(attributeNode).to(typeNode).withEdge(Relationship.IS_ATTRIBUTE_OF)

               val attributeTypeNode = type(attributeType.type.fullyQualifiedName)
               builder.connect(attributeNode).to(attributeTypeNode).withEdge(Relationship.IS_TYPE_OF)
               typesAndWhereTheyreUsed.put(attributeTypeNode, attributeNode)
               // See the relationship for why commented out ....
               // migrating this relationship to an INSTNACE_OF node.
//            builder.connect(attributeTypeNode).to(attributeNode).withEdge(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
            }
         }

//         log().debug("Added attribute ${type.name} to graph")
      }
      return typesAndWhereTheyreUsed
   }

   private fun attributeFqn(typeFullyQualifiedName: String, attributeName: AttributeName): String {
      return "$typeFullyQualifiedName/$attributeName"
   }

   private fun appendServices(builder: HipsterGraphBuilder<Element, Relationship>, schema: Schema, excludedOperations: Set<QualifiedName>) {
      return schema.services.forEach { service: Service ->
         service.operations
            .filter { !excludedOperations.contains(it.qualifiedName) }
            .forEach { operation: Operation ->
               val operationNode = operation(service, operation)
               operation.parameters.forEachIndexed { index, parameter ->
                  // When building services, we need to use 'connector nodes'
                  // as Hipster4J doesn't support identical vertex pairs with seperate edges.
                  // eg: Service -[requiresParameter]-> Money && Service -[Provides]-> Money
                  // isn't supported, and results in the Edge for the 2nd pair to remain undefined
                  val typeFqn = parameter.type.fullyQualifiedName
                  val paramNode = parameter(typeFqn)
                  builder.connect(operationNode).to(paramNode).withEdge(Relationship.REQUIRES_PARAMETER)
                  builder.connect(paramNode).to(operationNode).withEdge(Relationship.IS_PARAMETER_ON)

                  if (parameter.type.isParameterType) {
                     // Traverse into the attributes of param types, and add extra nodes.
                     // As we're allowed to instantiate param types, discovered values within the graph
                     // can be used to populate new instances, so form links.
                     parameter.type.attributes.forEach { attriubteName, typeRef ->
                        // Point back to the "parent" param node (the parameterObject)
                        // might revisit this in the future, and point back to the Operation itself.
                        builder.connect(parameter(typeRef.type.fullyQualifiedName)).to(paramNode).withEdge(Relationship.IS_PARAMETER_ON)
                     }
                  }
               }

               // Build the instance.
               // It connects to it's type, but also to the attributes that are
               // now traversable, as we have an actual instance of the thing
               val resultInstanceFqn = operation.returnType.qualifiedName.parameterizedName
               appendProvidedInstances(builder, resultInstanceFqn, schema, operationNode)


//            log().debug("Added Operation ${operationNode.value} to graph")
            }
      }
   }

   /**
    * Builds all the providedInstance() nodes required for modelling a returned instance from a service.
    * It's return type is created as an instance:type, and all the parameters of the return type
    * are also mapped as providedInstanceMembers() and instance:types.
    */
   private fun appendProvidedInstances(builder: HipsterGraphBuilder<Element, Relationship>, instanceFqn: String, schema: Schema, provider: Element? = null, value:TypedInstance? = null) {
      val providedInstance = if (value != null) {
         providedInstance(value)
      } else {
         // TODO : Not sure if this is still value -- ie., not provided a typedInstance here
         providedInstance(instanceFqn)
      }
      if (provider != null) {
         builder.connect(provider).to(providedInstance).withEdge(Relationship.PROVIDES)
      }

      val type = schema.type(instanceFqn)
      // Note - We don't link provided instances to their instance types, as it creates a
      // link that can't be removed later.
      // eg - this is a problem:
      // instanceOfFoo -[isInstanceOf]-> TypeFoo
      // TypeFoo -[hasAttribute] -> FooParam
      // This creates a problem if we decide that the specific instance of foo
      // should not be permitted to provide FooParam (because it's invalid / null / etc).
      // By linking Foo to TypeFoo, the graph is able to navigate instance -> type -> attribute
      // If we were to remove the type -> attribute link, it would break for
      // searches against values we haven't yet received (eg., finding a value from
      // a service to navigate to it's attribute).
      // So, build a link of instanceOfFoo -[canPopulate]-> TypeFoo.
      // Later, if we don't want to populate TypeFoo with this specific instance,
      // we can exclude the edge  (See PathExclusionCalculator and HipsterGraphBuilder.filterToEligibleConnections
      // So, only build the link if we're linking a theoretical instance (ie.,
      // one we haven't yet discovered).  If we're linking an actual value, just
      // use the canPopulate relationship.
      if (value == null) {
         builder.connect(providedInstance).to(type(instanceFqn)).withEdge(Relationship.IS_INSTANCE_OF)
      }
      builder.connect(providedInstance).to(parameter(instanceFqn)).withEdge(Relationship.CAN_POPULATE)

      // This instance can also populate any types that it inherits from.
      type.inheritanceGraph.forEach { inheritedType ->
         builder.connect(providedInstance).to(parameter(inheritedType.fullyQualifiedName)).withEdge(Relationship.CAN_POPULATE)
      }
      if (!type.isClosed) {
         appendInstanceAttributes(schema, instanceFqn, builder, providedInstance)
      }
   }

   private fun appendInstanceAttributes(schema: Schema, instanceFqn: String, builder: HipsterGraphBuilder<Element, Relationship>, providedInstance: Element) {
      schema.type(instanceFqn).attributes.forEach { attributeName, field ->
         val providedInstanceMember = providedInstanceMember(attributeFqn(instanceFqn, attributeName))
         builder.connect(providedInstance).to(providedInstanceMember).withEdge(Relationship.INSTANCE_HAS_ATTRIBUTE)
         // The "providedInstance" node of the member itself
         val memberInstance = providedInstance(field.type.fullyQualifiedName)
         builder.connect(providedInstanceMember).to(memberInstance).withEdge(Relationship.IS_ATTRIBUTE_OF)
         // The member instance we have can populate required params
         builder.connect(memberInstance).to(parameter(field.type.fullyQualifiedName)).withEdge(Relationship.CAN_POPULATE)
         builder.connect(memberInstance).to(type(field.type.fullyQualifiedName)).withEdge(Relationship.IS_INSTANCE_OF)
      }
   }

}
