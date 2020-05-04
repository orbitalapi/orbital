package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonView

interface TypeFullView : TypeLightView
interface TypeLightView

typealias AttributeName = String

/**
 * TODO: We should consider deprecating and removing the vyne specific versions of the type system,
 * and just use Taxi's model throughout.
 * There are clear downsides to becoming coupled to Taxi, but
 * given how interweaved their evolution is, and tha the Taxi model is now sufficiently more advanced,
 * it may be worth collapsing them.
 */
data class Type(
   @JsonView(TypeLightView::class)
   val name: QualifiedName,
   @JsonView(TypeFullView::class)
   val attributes: Map<AttributeName, Field> = emptyMap(),

   @JsonView(TypeFullView::class)
   val modifiers: List<Modifier> = emptyList(),

   @JsonView(TypeFullView::class)
   val metadata: List<Metadata> = emptyList(),

   @JsonView(TypeFullView::class)
   val aliasForType: QualifiedName? = null,

   @JsonView(TypeFullView::class)
   val inherits: List<Type> = emptyList(),

   @JsonView(TypeFullView::class)
   val enumValues: List<String> = emptyList(),

   @JsonView(TypeFullView::class)
   val sources: List<SourceCode>,

   val typeParameters: List<Type> = emptyList(),


   // Part of the migration back to taxi types
   val taxiType: lang.taxi.types.Type,

   val typeDoc: String?

) : SchemaMember {
   constructor(name: String, attributes: Map<AttributeName, Field> = emptyMap(), modifiers: List<Modifier> = emptyList(), metadata: List<Metadata> = emptyList(), aliasForType: QualifiedName? = null, inherits: List<Type>, enumValues: List<String> = emptyList(), sources: List<SourceCode>, taxiType: lang.taxi.types.Type, typeDoc: String? = null) : this(name.fqn(), attributes, modifiers, metadata, aliasForType, inherits, enumValues, sources, taxiType = taxiType, typeDoc = typeDoc)

   @JsonView(TypeFullView::class)
   val isTypeAlias = aliasForType != null
   @JsonView(TypeFullView::class)
   val isParameterType: Boolean = this.modifiers.contains(Modifier.PARAMETER_TYPE)
   @JsonView
   val isClosed: Boolean = this.modifiers.contains(Modifier.CLOSED)

   val isPrimitive: Boolean = this.modifiers.contains(Modifier.PRIMITIVE)

   val fullyQualifiedName: String
      get() = name.fullyQualifiedName

   @JsonView(TypeFullView::class)
   val inheritanceGraph = calculateInheritanceGraph()

   @JsonView(TypeFullView::class)
   val isCollection: Boolean = this.inheritsFrom("lang.taxi.Array".fqn())

   @JsonView(TypeFullView::class)
   val isScalar = attributes.isEmpty() && !isCollection

   fun matches(other: Type, strategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES): Boolean {
      return strategy.matches(this, other)
   }

   fun attribute(name: AttributeName): Field {
      return attributes.getValue(name)
   }

   private fun calculateInheritanceGraph(typesToExclude: List<Type> = emptyList()): List<Type> {
      val allTypesToExclude = typesToExclude + listOf(this)
      return this.inherits.flatMap { inheritedType ->
         if (!allTypesToExclude.contains(inheritedType)) {
            setOf(inheritedType) + inheritedType.calculateInheritanceGraph(allTypesToExclude)
         } else emptySet()
      }
   }

   fun resolvesSameAs(other: Type): Boolean {
      // Note: We need to consider parameterised types here.
      // In doing so, note that some usages use this approach to check if something
      // is a collection.
      if (this.fullyQualifiedName == other.fullyQualifiedName) return true
      if (this.isTypeAlias && this.aliasForType!!.fullyQualifiedName == other.fullyQualifiedName) return true
      if (other.isTypeAlias && other.aliasForType!!.fullyQualifiedName == this.fullyQualifiedName) return true
      return false
   }

   fun inheritsFrom(other: Type): Boolean {
      return this.inheritsFrom(other.name)
   }

   private fun inheritsFrom(qualifiedName: QualifiedName): Boolean {
      // TODO : How does this handle TypeAliases?
      // Note: This obviously doesn't work properly, as it
      // ignores generics.
      // Right now, I'm focussed on isCollection(), which works by looking at
      // foo.inheritsFrom(lang.taxi.Array).
      // Need to fix this, but when I do, make sure that isCollection still works.
      val namesToEvaluate = (setOf(this.name) + this.inheritanceGraph.map { it.name } + setOf(this.aliasForType)).filterNotNull()
      return namesToEvaluate.any { it.rawTypeEquals(qualifiedName) }
   }

   fun hasMetadata(name: QualifiedName): Boolean {
      return this.metadata.any { it.name == name }
   }
}

// Part of the migration back to Taxi types
fun lang.taxi.types.Type.asVyneTypeReference(): TypeReference {
   // TODO : Resolve isCollection.  Just being lazy.
   return TypeReference(this.qualifiedName.fqn(), false)
}

enum class Modifier {
   CLOSED,
   PARAMETER_TYPE,
   // TODO : Is it right to treat these as modifiers?  They're not really,
   // but I'm trying to avoid a big collection of boolean flags
   ENUM,
   PRIMITIVE
}
