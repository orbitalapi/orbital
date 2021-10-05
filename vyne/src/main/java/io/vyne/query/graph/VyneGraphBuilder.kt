package io.vyne.query.graph

import com.google.common.cache.CacheBuilder
import es.usc.citius.hipster.graph.GraphEdge
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import io.vyne.DisplayGraphBuilder
import io.vyne.HipsterGraphBuilder
import io.vyne.SchemaPathFindingGraph
import io.vyne.VyneGraphBuilderCacheSettings
import io.vyne.VyneHashBasedHipsterDirectedGraph
import io.vyne.VyneHashBasedHipsterDirectedGraph.Companion.createCachingGraph
import io.vyne.models.TypedEnumValue
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.query.SearchGraphExclusion
import io.vyne.query.excludedValues
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.Operation
import io.vyne.schemas.OperationNames
import io.vyne.schemas.ParamNames
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Relationship
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.utils.ImmutableEquality
import io.vyne.utils.StrategyPerformanceProfiler

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
   PARAMETER,
   // Only used for constructing display graphs
   SERVICE;

   override fun toString(): String {
      return super.toString().toLowerCase().capitalize()
   }
}

@Deprecated("Do we still need this?")
data class GraphBuildResult(
   val graph: SchemaPathFindingGraph,
   val addedInstanceVertices: List<Element>,
   val removedEdges: List<GraphEdge<Element, Relationship>>
)

typealias GraphConnection = HipsterGraphBuilder.Connection<Element, Relationship>

data class Element(val value: Any, val elementType: ElementType, val instanceValue: Any? = null) {
   val equality = ImmutableEquality(this, Element::value, Element::elementType, Element::instanceValue)
   override fun hashCode(): Int = equality.hash()
   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
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
//      val prefix = "{" + elementType.name + "}:"
      return when (elementType) {
         ElementType.TYPE -> valueAsQualifiedName().name
         ElementType.MEMBER -> value.toString().split(".").last()
         ElementType.OPERATION -> OperationNames.shortDisplayNameFromOperation(value.toString().fqn())
         else ->  value.toString()
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
   return providedInstance(nodeId, typedInstance)
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

private data class GraphWithFactTypesCacheKey(
   val facts: Set<Type>,
   val graphBuilder: HipsterGraphBuilder<Element, Relationship>
)

private data class GraphWithFactInstancesCacheKey(
   val facts: Collection<TypedInstance>,
   val excludedEdges: List<EvaluatableEdge>,
   val baseGraph: VyneHashBasedHipsterDirectedGraph<Element, Relationship>
)

class VyneGraphBuilder(private val schema: Schema, vyneGraphBuilderCache: VyneGraphBuilderCacheSettings) {
   private val graphCache = CacheBuilder.newBuilder()
      .maximumSize(vyneGraphBuilderCache.graphWithFactTypesCacheSize)
      .build<List<GraphConnection>, GraphBuildResult>()

   private val baseSchemaConnectionsCache = CacheBuilder.newBuilder()
      .maximumSize(vyneGraphBuilderCache.baseSchemaGraphCacheSize) // arbitary, can tune later
      .build<Int, List<GraphConnection>>()

   fun build(
      facts: Collection<TypedInstance>,
      excludedOperations: Set<QualifiedName> = emptySet(),
      excludedEdges: List<EvaluatableEdge>,
      excludedServices: Set<QualifiedName>
   ):
      GraphBuildResult {

      // Our base graph constructing from type and service definitions.
      val baseSchemaConnections = getBaseSchemaConnections(excludedOperations, excludedServices)

      val connectionsForFacts = StrategyPerformanceProfiler.profiled("buildCreatedInstancesConnections") {
          createdInstances(facts, schema)
      }

      val connections = baseSchemaConnections + connectionsForFacts
      return buildGraph(connections, excludedEdges)
      // Here we're adding 'instance value' based connections into the 'base graph'
//      appendInstanceBasedElementsIntoBaseGraph(facts, excludedEdges)
   }

   private fun getBaseSchemaConnections(
      excludedOperations: Set<QualifiedName>,
      excludedServices: Set<QualifiedName>
   ): List<GraphConnection> {
      return baseSchemaConnectionsCache.get(excludedOperations.hashCode()) {
         StrategyPerformanceProfiler.profiled("getBaseSchemaConnections") {
            val typeConnections = buildTypeConnections(schema)
            val serviceConnections = buildServiceConnections(schema, excludedOperations, excludedServices)
            typeConnections + serviceConnections
         }
      }
   }

   private fun buildGraph(
      connections: List<GraphConnection>,
      excludedEdges: List<EvaluatableEdge> = emptyList()
   ): GraphBuildResult {
      val filteredFacts = StrategyPerformanceProfiler.profiled("buildGraph.filterExcludedEdges") {
         if (excludedEdges.isEmpty()) {
            connections
         } else {
            val excludedConnections = excludedEdges.map { it.connection }
            connections.filter { !excludedConnections.contains(it) }
         }
      }

      return graphCache.get(filteredFacts) {
         StrategyPerformanceProfiler.profiled("buildGraph") {
            val graph = createCachingGraph(filteredFacts)
            // TODO : Waiting to see if we actually use GraphBuildResult anymore, if not, just reutnr the graph here.
            GraphBuildResult(graph, emptyList(), emptyList())
         }
      }
   }


   fun build(
      types: Set<Type> = emptySet(),
      excludedOperations: Set<SearchGraphExclusion<QualifiedName>> = emptySet(),
      excludedServices: Set<SearchGraphExclusion<QualifiedName>> = emptySet()
   ): HipsterDirectedGraph<Element, Relationship> {
      val baseConnections =
         getBaseSchemaConnections(excludedOperations.excludedValues(), excludedServices.excludedValues())
      val connections = baseConnections + appendInstanceTypes(types, schema)
      return buildGraph(connections).graph
   }

   fun buildDisplayGraph(): HipsterDirectedGraph<Element, Relationship> {
      val graph = build()
      return DisplayGraphBuilder().convertToDisplayGraph(graph)
   }

   private fun appendInstanceTypes(
      types: Set<Type>,
      schema: Schema
   ): List<GraphConnection> {
      return types.flatMap {
         val typeFqn = it.qualifiedName.parameterizedName
         buildProvidedInstancesConnections(typeFqn, schema)
         // Note: An old implementation has been removed from here.  Check the git history
         // if we think stuff has broken.
      }
   }

   private fun appendInstances(
      instances: Collection<TypedInstance>, schema: Schema
   ): List<GraphConnection> {
      return instances.flatMap { typedInstance ->
         buildProvidedInstancesConnections(typedInstance.typeName, schema, value = typedInstance)
      }
   }


   private fun buildTypeConnections(schema: Schema): List<GraphConnection> {
      val connections = mutableListOf<GraphConnection>()
      fun addConnection(fromEdge: Element, toEdge: Element, relationship: Relationship) {
         connections.add(GraphConnection(fromEdge, toEdge, relationship))
      }
      schema.types.forEach { type: Type ->

         val typeFullyQualifiedName = type.fullyQualifiedName
         val typeNode = type(typeFullyQualifiedName)

         type.inherits.forEach { inheritedType ->
            addConnection(typeNode, type(inheritedType), Relationship.EXTENDS_TYPE)
         }

         type.unformattedTypeName?.let { unformattedType ->
            // A formatted value can be populated by it's unformatted value,
            // and vice versa
            // Note: Is CanPopulate the right relationship here? Might need another one.
            val unformattedTypeNode = type(unformattedType.fullyQualifiedName)
            addConnection(typeNode, unformattedTypeNode, Relationship.CAN_POPULATE)
            addConnection(unformattedTypeNode, typeNode, Relationship.CAN_POPULATE)
         }

         if (!type.isClosed) {
            type.attributes.map { (attributeName, attributeType) ->
               val attributeQualifiedName = attributeFqn(typeFullyQualifiedName, attributeName)
               val attributeNode = member(attributeQualifiedName)
               addConnection(typeNode, attributeNode, Relationship.HAS_ATTRIBUTE)

               // (attribute) -[IS_ATTRIBUTE_OF]-> (type)
               addConnection(attributeNode, typeNode, Relationship.IS_ATTRIBUTE_OF)

               val attributeTypeNode = type(attributeType.type.fullyQualifiedName)
               addConnection(attributeNode, attributeTypeNode, Relationship.IS_TYPE_OF)
//               typesAndWhereTheyreUsed.put(attributeTypeNode, attributeNode)
               // See the relationship for why commented out ....
               // migrating this relationship to an INSTNACE_OF node.
//            builder.connect(attributeTypeNode).to(attributeNode).withEdge(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
            }
         }

//         log().debug("Added attribute ${type.name} to graph")
      }
      return connections
   }

   private fun attributeFqn(typeFullyQualifiedName: String, attributeName: AttributeName): String {
      return "$typeFullyQualifiedName/$attributeName"
   }

   private fun buildServiceConnections(
      schema: Schema,
      excludedOperations: Set<QualifiedName>,
      excludedServices: Set<QualifiedName>
   ): List<GraphConnection> {
      val connections = mutableListOf<GraphConnection>()
      fun addConnection(fromEdge: Element, toEdge: Element, relationship: Relationship) {
         connections.add(GraphConnection(fromEdge, toEdge, relationship))
      }
      schema
         .services
         .filter { !excludedServices.contains(it.name) }
         .forEach { service: Service ->
            service.operations
               .filter { !excludedOperations.contains(it.qualifiedName) }
               .forEach { operation: Operation ->
                  val operationNode = operation(service, operation)
                  operation.parameters.forEachIndexed { _, parameter ->
                     // When building services, we need to use 'connector nodes'
                     // as Hipster4J doesn't support identical vertex pairs with seperate edges.
                     // eg: Service -[requiresParameter]-> Money && Service -[Provides]-> Money
                     // isn't supported, and results in the Edge for the 2nd pair to remain undefined
                     val typeFqn = parameter.type.fullyQualifiedName
                     val paramNode = parameter(typeFqn)
                     addConnection(operationNode, paramNode, Relationship.REQUIRES_PARAMETER)
                     addConnection(paramNode, operationNode, Relationship.IS_PARAMETER_ON)

                     if (parameter.type.isParameterType) {
                        // Traverse into the attributes of param types, and add extra nodes.
                        // As we're allowed to instantiate param types, discovered values within the graph
                        // can be used to populate new instances, so form links.
                        parameter.type.attributes.forEach { (_, typeRef) ->
                           // Point back to the "parent" param node (the parameterObject)
                           // might revisit this in the future, and point back to the Operation itself.
                           addConnection(
                              parameter(typeRef.type.fullyQualifiedName),
                              paramNode,
                              Relationship.IS_PARAMETER_ON
                           )
                        }
                     }
                  }

                  // Build the instance.
                  // It connects to it's type, but also to the attributes that are
                  // now traversable, as we have an actual instance of the thing
                  val resultInstanceFqn = operation.returnType.qualifiedName.parameterizedName
                  connections.addAll(buildProvidedInstancesConnections(resultInstanceFqn, schema, operationNode))


//            log().debug("Added Operation ${operationNode.value} to graph")
               }
         }

      return connections
   }

   /**
    * Builds all the providedInstance() nodes required for modelling a returned instance from a service.
    * It's return type is created as an instance:type, and all the parameters of the return type
    * are also mapped as providedInstanceMembers() and instance:types.
    */
   private fun buildProvidedInstancesConnections(
      instanceFqn: String,
      schema: Schema,
      provider: Element? = null,
      value: TypedInstance? = null
   ): List<GraphConnection> {
      val connections = mutableListOf<GraphConnection>()
      fun addConnection(fromEdge: Element, toEdge: Element, relationship: Relationship) {
         connections.add(GraphConnection(fromEdge, toEdge, relationship))
      }

      val providedInstance = if (value != null) {
         providedInstance(value)
         error("buildProvidedInstancesConnections - Bomb triggered -- value != null")
      } else {
         // TODO : Not sure if this is still value -- ie., not provided a typedInstance here
         providedInstance(instanceFqn)
      }
      if (provider != null) {
         addConnection(provider, providedInstance, Relationship.PROVIDES)
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
         addConnection(providedInstance, type(instanceFqn), Relationship.IS_INSTANCE_OF)
      }
      addConnection(providedInstance, parameter(instanceFqn), Relationship.CAN_POPULATE)

      // This instance can also populate any types that it inherits from.
      type.inheritanceGraph.forEach { inheritedType ->
         addConnection(providedInstance, parameter(inheritedType.fullyQualifiedName), Relationship.CAN_POPULATE)
      }
      if (type.isEnum) {
         TODO()
      }

      if (!type.isClosed) {
         // We treat attributes of actual values that we know differently
         // from those that we could theoretically discover.
         // Attributes from real instances have atttributes with null values excluded.
         // If it's discoverable, we optimistically add them.
         // Note: We might need to modify this so that once a discoverable value
         // becomes known, we rebuild our graph, trimming edges where attribues were discovered
         // to be null
         if (value != null) {
            connections.addAll(
               buildInstanceAttributesOfActualInstanceConnections(
                  schema,
                  instanceFqn,
                  providedInstance,
                  value
               )
            )
         } else {
            connections.addAll(
               buildInstanceAttributesOfDiscoverableInstanceConnections(
                  schema,
                  instanceFqn,
                  providedInstance
               )
            )
         }

      }
      return connections
   }

   private fun buildInstanceAttributesOfActualInstanceConnections(
      schema: Schema,
      instanceFqn: String,
      providedInstanceNode: Element,
      instance: TypedInstance
   ): List<GraphConnection> {
      if (instance !is TypedObject) {
         return emptyList()
      }
      return schema.type(instanceFqn).attributes.flatMap { (attributeName, field) ->
         val fieldValue = instance[attributeName]
         when {
            fieldValue.value != null && fieldValue.value != "" -> buildProvidedInstanceAttributeConnections(
               instanceFqn,
               attributeName,
               providedInstanceNode,
               field
            )
            // Include calculated fields
            field.formula != null -> buildProvidedInstanceAttributeConnections(
               instanceFqn,
               attributeName,
               providedInstanceNode,
               field
            )
            else -> emptyList()
         }
         // else -> log().debug("Not building link to attribute $attributeName on typedInstance, as provided value was null")
      }
   }

   private fun createdInstances(
      instances: Collection<TypedInstance>,
      schema: Schema
   ): List<GraphConnection> {
      return instances.map { typedInstance ->
         createProvidedInstances(typedInstance.typeName, schema, value = typedInstance)
      }.flatten()
   }

   private fun createProvidedInstances(
      instanceFqn: String,
      schema: Schema,
      provider: Element? = null,
      value: TypedInstance? = null
   ): MutableList<HipsterGraphBuilder.Connection<Element, Relationship>> {
      val createdConnections = mutableListOf<HipsterGraphBuilder.Connection<Element, Relationship>>()
      val providedInstance = if (value != null) {
         providedInstance(value)
      } else {
         // TODO : Not sure if this is still value -- ie., not provided a typedInstance here
         providedInstance(instanceFqn)
      }
      if (provider != null) {
         createdConnections.add(HipsterGraphBuilder.Connection(provider, providedInstance, Relationship.PROVIDES))
         // builder.connect(provider).to(providedInstance).withEdge(Relationship.PROVIDES)
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
         createdConnections.add(
            HipsterGraphBuilder.Connection(
               providedInstance,
               type(instanceFqn),
               Relationship.IS_INSTANCE_OF
            )
         )
         //builder.connect(providedInstance).to(type(instanceFqn)).withEdge(Relationship.IS_INSTANCE_OF)
      }
      createdConnections.add(
         HipsterGraphBuilder.Connection(
            providedInstance,
            parameter(instanceFqn),
            Relationship.CAN_POPULATE
         )
      )
      //builder.connect(providedInstance).to(parameter(instanceFqn)).withEdge(Relationship.CAN_POPULATE)

      // This instance can also populate any types that it inherits from.
      type.inheritanceGraph.forEach { inheritedType ->
         createdConnections.add(
            HipsterGraphBuilder.Connection(
               providedInstance,
               parameter(inheritedType.fullyQualifiedName),
               Relationship.CAN_POPULATE
            )
         )
         //builder.connect(providedInstance).to(parameter(inheritedType.fullyQualifiedName)).withEdge(Relationship.CAN_POPULATE)
      }
      if (value is TypedEnumValue) {
         val synonymConnections = StrategyPerformanceProfiler.profiled("buildCreatedInstancesConnections.buildTypedValueEnums") {
            value.synonyms.flatMap {   synonym ->
                  // Even though the synonymss are technically providedInstances,
                  // We're not recursing into createProvidedInstances here as it would create a
                  // stack overflow, pointing back to this synonym.
                  // SO, just carefully add the links we care about.
                  val synonymInstance = providedInstance(synonym)
                  listOf(
                     GraphConnection(
                        providedInstance,
                        synonymInstance,
                        Relationship.IS_SYNONYM_OF
                     ),
                     GraphConnection(
                        synonymInstance,
                        parameter(synonym.typeName),
                        Relationship.CAN_POPULATE
                     )
                  )
               }
         }
         createdConnections.addAll(synonymConnections)
      }
      if (!type.isClosed) {
         // We treat attributes of actual values that we know differently
         // from those that we could theoretically discover.
         // Attributes from real instances have atttributes with null values excluded.
         // If it's discoverable, we optimistically add them.
         // Note: We might need to modify this so that once a discoverable value
         // becomes known, we rebuild our graph, trimming edges where attribues were discovered
         // to be null
         if (value != null) {
            createdConnections.addAll(
               createInstanceAttributesOfActualInstance(
                  schema,
                  instanceFqn,
                  providedInstance,
                  value
               )
            )
         } else {
            createdConnections.addAll(
               createInstanceAttributesOfDiscoverableInstance(
                  schema,
                  instanceFqn,
                  providedInstance
               )
            )
         }
      }
      return createdConnections
   }

   private fun createInstanceAttributesOfActualInstance(
      schema: Schema,
      instanceFqn: String,
      providedInstanceNode: Element,
      instance: TypedInstance
   ):
      MutableList<HipsterGraphBuilder.Connection<Element, Relationship>> {
      val connections = mutableListOf<HipsterGraphBuilder.Connection<Element, Relationship>>()
      if (instance !is TypedObject) {
         return connections
      }
      schema.type(instanceFqn).attributes.forEach { (attributeName, field) ->
         val fieldValue = if (instance.hasAttribute(attributeName)) instance[attributeName] else null
         when {
            fieldValue?.value != null && fieldValue.value != "" ->
               connections.addAll(
                  createProvidedInstanceAttribute(
                     instanceFqn,
                     attributeName,
                     providedInstanceNode,
                     field,
                     fieldValue
                  )
               )
            // Include calculated fields
            field.formula != null -> connections.addAll(
               createProvidedInstanceAttribute(
                  instanceFqn,
                  attributeName,
                  providedInstanceNode,
                  field,
                  null
               )
            )
         }
         // else -> log().debug("Not building link to attribute $attributeName on typedInstance, as provided value was null")
      }
      return connections
   }

   private fun createInstanceAttributesOfDiscoverableInstance(
      schema: Schema,
      instanceFqn: String,
      providedInstance: Element
   ):
      List<HipsterGraphBuilder.Connection<Element, Relationship>> {
      return schema.type(instanceFqn).attributes.map { (attributeName, field) ->
         // fieldValue is null, as we don't have an actual value, but could discover one if we wanted to.
         // The idea here is that once the value has been discovered, we'll rebuild the graph with the actual values.
         createProvidedInstanceAttribute(instanceFqn, attributeName, providedInstance, field, fieldValue = null)
      }.flatten()
   }


   private fun buildProvidedInstanceAttributeConnections(
      instanceFqn: String,
      attributeName: AttributeName,
      providedInstanceNode: Element,
      field: Field
   ): List<GraphConnection> {
      val connections = mutableListOf<GraphConnection>()
      fun addConnection(fromEdge: Element, toEdge: Element, relationship: Relationship) {
         connections.add(GraphConnection(fromEdge, toEdge, relationship))
      }

      val providedInstanceMember = providedInstanceMember(attributeFqn(instanceFqn, attributeName))
      addConnection(providedInstanceNode, providedInstanceMember, Relationship.INSTANCE_HAS_ATTRIBUTE)
      // The "providedInstance" node of the member itself
      val memberInstance = providedInstance(field.type.fullyQualifiedName)
      addConnection(providedInstanceMember, memberInstance, Relationship.IS_ATTRIBUTE_OF)
      // The member instance we have can populate required params
      addConnection(memberInstance, parameter(field.type.fullyQualifiedName), Relationship.CAN_POPULATE)
      addConnection(memberInstance, type(field.type.fullyQualifiedName), Relationship.IS_INSTANCE_OF)

      return connections
   }

   private fun createProvidedInstanceAttribute(
      instanceFqn: String,
      attributeName: AttributeName,
      providedInstanceNode: Element,
      field: Field,
      fieldValue: TypedInstance?
   ): List<GraphConnection> {
      val connections = mutableListOf<HipsterGraphBuilder.Connection<Element, Relationship>>()
      val providedInstanceMember = providedInstanceMember(attributeFqn(instanceFqn, attributeName))
      connections.add(
         GraphConnection(
            providedInstanceNode,
            providedInstanceMember,
            Relationship.INSTANCE_HAS_ATTRIBUTE
         )
      )
      //builder.connect(providedInstanceNode).to(providedInstanceMember).withEdge(Relationship.INSTANCE_HAS_ATTRIBUTE)
      // The "providedInstance" node of the member itself
      val memberInstance = providedInstance(field.type.fullyQualifiedName)
      connections.add(
         HipsterGraphBuilder.Connection(
            providedInstanceMember,
            memberInstance,
            Relationship.IS_ATTRIBUTE_OF
         )
      )

      //builder.connect(providedInstanceMember).to(memberInstance).withEdge(Relationship.IS_ATTRIBUTE_OF)

      // The member instance we have can populate required params
      connections.add(
         HipsterGraphBuilder.Connection(
            memberInstance,
            parameter(field.type.fullyQualifiedName),
            Relationship.CAN_POPULATE
         )
      )
      //builder.connect(memberInstance).to(parameter(field.type.fullyQualifiedName)).withEdge(Relationship.CAN_POPULATE)
      connections.add(
         HipsterGraphBuilder.Connection(
            memberInstance,
            type(field.type.fullyQualifiedName),
            Relationship.IS_INSTANCE_OF
         )
      )

      // In the future we may wish to consider if the fieldValue is null (possibly because we haven't yet discovered the value),
      // we still want to recurse into the attributes that are discoverable from this field, which would allow deeper
      // traversal.
      if (fieldValue != null && fieldValue is TypedObject) {
         val linksToNestedFieldAttributes = createInstanceAttributesOfActualInstance(
            schema,
            fieldValue.type.fullyQualifiedName,
            memberInstance,
            fieldValue
         )
         connections.addAll(linksToNestedFieldAttributes)
      }
      //builder.connect(memberInstance).to(type(field.type.fullyQualifiedName)).withEdge(Relationship.IS_INSTANCE_OF)
      return connections
   }

   private fun buildInstanceAttributesOfDiscoverableInstanceConnections(
      schema: Schema,
      instanceFqn: String,
      providedInstance: Element
   ): List<GraphConnection> {
      return schema.type(instanceFqn).attributes.flatMap { (attributeName, field) ->
         buildProvidedInstanceAttributeConnections(instanceFqn, attributeName, providedInstance, field)
      }
   }

   fun prune(graphBuildResult: GraphBuildResult) {
      val (graph, instanceBasedVertices, removedEdges) = graphBuildResult
      val vyneGraph = graph as VyneHashBasedHipsterDirectedGraph<Element, Relationship>
      vyneGraph.prune(instanceBasedVertices)
      vyneGraph.addRemovedEdges(removedEdges)
   }
}

