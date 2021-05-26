package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonView
import com.google.common.cache.CacheBuilder
import io.vyne.VersionedSource
import io.vyne.models.*
import io.vyne.models.DataSource
import io.vyne.utils.ImmutableEquality
import lang.taxi.services.operations.constraints.PropertyFieldNameIdentifier
import lang.taxi.services.operations.constraints.PropertyIdentifier
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.*
import lang.taxi.utils.takeHead
import mu.KotlinLogging

interface TypeFullView : TypeLightView
interface TypeLightView

typealias AttributeName = String

private val logger = KotlinLogging.logger {}

/**
 * This is a documenation-only annotation - ie., it has no runtime
 * impact.
 *
 * Fields annotated with this annotation must remain lazy evaluation until
 * after a full typeCache has been populated, as they need to look up other
 * types in the typecache
 */
annotation class DeferEvaluationUntilTypeCacheCreated

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
   @JsonView(TypeLightView::class)
   val attributes: Map<AttributeName, Field> = emptyMap(),

   @JsonView(TypeFullView::class)
   val modifiers: List<Modifier> = emptyList(),

   @JsonView(TypeFullView::class)
   val metadata: List<Metadata> = emptyList(),

   // Implementation note: When this class is being constructed, we first pass
   // the name, then later come back and populate the aliasForType.
   // This is to address any circular references, which are permissable.
   @JsonView(TypeFullView::class)
   @JsonProperty(value = "aliasForType")
   val aliasForTypeName: QualifiedName? = null,

   @JsonView(TypeFullView::class)
   @JsonProperty("inheritsFrom")
   val inheritsFromTypeNames: List<QualifiedName> = emptyList(),

   @JsonView(TypeFullView::class)
   val enumValues: List<EnumValue> = emptyList(),

   @JsonView(TypeFullView::class)
   val sources: List<VersionedSource>,

   @JsonProperty("typeParameters")
   val typeParametersTypeNames: List<QualifiedName> = emptyList(),
   // Part of the migration back to taxi types
   @JsonIgnore
   val taxiType: lang.taxi.types.Type,

   val typeDoc: String?,

   @JsonIgnore
   val typeCache: TypeCache = EmptyTypeCache

) : SchemaMember {
   constructor(
      name: String,
      attributes: Map<AttributeName, Field> = emptyMap(),
      modifiers: List<Modifier> = emptyList(),
      metadata: List<Metadata> = emptyList(),
      aliasForTypeName: QualifiedName? = null,
      inheritsFromTypeNames: List<QualifiedName>,
      enumValues: List<EnumValue> = emptyList(),
      sources: List<VersionedSource>,
      taxiType: lang.taxi.types.Type,
      typeDoc: String? = null,
      typeCache: TypeCache
   ) :
      this(
         name.fqn(),
         attributes,
         modifiers,
         metadata,
         aliasForTypeName,
         inheritsFromTypeNames,
         enumValues,
         sources,
         taxiType = taxiType,
         typeDoc = typeDoc,
         typeCache = typeCache
      )

   // Intentionally excluded from equality:
   // taxiType - the antlr classes make equailty hard, and not meaningful in this context
   // typeCache - screws with equality, and not meaningful
   private val equality = ImmutableEquality(
      this,
      Type::name
   )

   private val resolvedAlias: Type


   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()

   @JsonView(TypeFullView::class)
   val isTypeAlias = aliasForTypeName != null

   @JsonView(TypeFullView::class)
   val offset: Int? = taxiType.offset

   @JsonView(TypeFullView::class)
   val format: List<String>? = taxiType.format

   @JsonView(TypeFullView::class)
   val hasFormat = format != null

   @JsonView(TypeFullView::class)
   val isCalculated = taxiType.calculation != null

   @get:JsonView(TypeFullView::class)
   val basePrimitiveTypeName: QualifiedName? = taxiType.basePrimitive?.toQualifiedName()?.toVyneQualifiedName()

   @get:JsonIgnore
   val calculation: Formula?
      get() = taxiType.calculation

   @get:JsonView(TypeFullView::class)
   val unformattedTypeName: QualifiedName?

   @get:JsonIgnore
   val inherits: List<Type> = this.inheritsFromTypeNames.mapNotNull { aliasName ->
      typeCache.type(aliasName)
   }


   @get:JsonIgnore
   val aliasForType: Type? =
      this.aliasForTypeName?.let { typeCache.type(it) }

   @get:JsonIgnore
   val typeParameters: List<Type> =
      this.typeParametersTypeNames.map { typeCache.type(it) }

   // TODO : This name sucks.  Need a consistent term for "the real thing, unwrapping the aliases if they exist"
   @get:JsonIgnore
   val underlyingTypeParameters: List<Type>

   @get:JsonProperty("underlyingTypeParameters")
   val underlyingTypeParameterNames: List<QualifiedName>

   @get:JsonIgnore
   val enumTypedInstances: List<TypedEnumValue> =
      this.enumValues.map { enumValue ->
         TypedEnumValue(this, enumValue, this.typeCache, UndefinedSource)
      }

   init {
      // placing these definitions against the field
      // causes exceptions because not all attributes have been populated,
      // which causes NPE's.
      this.unformattedTypeName = if (hasFormat || offset != null) {
         resolveUnderlyingFormattedType().qualifiedName
      } else null
      this.resolvedAlias = calculateResolvedAliases()
      this.underlyingTypeParameters = this.resolvedAlias.typeParameters
      this.underlyingTypeParameterNames = this.underlyingTypeParameters.map { it.name }
   }

   fun enumTypedInstance(value: Any, source: DataSource): TypedEnumValue {
      // Edge case - we allow parsing of boolean values, treated as strings
      val searchValue = if (value is Boolean) value.toString() else value
      // Use the TaxiType to resolve the value, so that defaults and lenients are used.
      val enumInstance = (this.taxiType as EnumType)
         .of(searchValue)
      val valueKind = EnumValueKind.from(value, this.taxiType)
      return this.enumTypedInstances.firstOrNull { it.name == enumInstance.name }
         ?.copy(source = source, valueKind = valueKind)
         ?: error("No typed instance found for value $value on ${this.fullyQualifiedName}")
   }

   /**
    * This is a relaxed version of enumTypedInstance.
    *  In e2e test, the following test
    * `Rfq ConvertibleBonds Report for Today`
    * raises error("No typed instance found for value $value on ${this.fullyQualifiedName}") in 'enumTypedInstance'
    * as there is an issue in the taxonomy:
    * bbg.rfq.RfqCbIngestion has the following:
    *    priceType : RfqPriceType? by when {
    *         this.price = null -> null
    *         this.price != null && this.bondUnit = "Yes" -> "CURR"
    *        else -> "PCT"
    *      }
    * However, RfgPriceType has the following enum values (i.e. it doesn't have CURR as a value)
    *    enum RfqPriceType {
    *        // doesn't work shows % in output
    *        // PCT1("%"),
    *        PCT1,
    *        default PCT synonym of cacib.common.price.PriceType.PCT
    *     }
    * TODO: We should have raised compilation error for 'priceType' in bbg.rfq.RfqCbIngestion as 'CURR' is not a valid RfqPriceType enum value.
    * We should get rid of this when Taxi is modified accordingly.
    */
   fun enumTypedInstanceOrNull(value: Any, source:DataSource): TypedEnumValue? {
      val underlyingEnumType = this.taxiType as EnumType
      return try {
         // Defer to the underlying enum, so that leniencey and default values
         // are considered.
         val enumValueFromProvidedValue = underlyingEnumType.of(value)
         this.enumTypedInstance(enumValueFromProvidedValue.name, source)
      } catch (e: Exception) {
         null
      }
   }

   // TODO : I suspect all of these isXxxx vars need to defer to the underlying aliased type.
   @JsonView(TypeFullView::class)
   val isParameterType: Boolean = this.modifiers.contains(Modifier.PARAMETER_TYPE)

   // TODO : I suspect all of these isXxxx vars need to defer to the underlying aliased type.
   @JsonView
   val isClosed: Boolean = this.modifiers.contains(Modifier.CLOSED)

   val isPrimitive: Boolean = this.modifiers.contains(Modifier.PRIMITIVE)

   val fullyQualifiedName: String
      get() = name.fullyQualifiedName

   @get:JsonIgnore
   val qualifiedName: QualifiedName
      get() = QualifiedName(fullyQualifiedName, typeParametersTypeNames)

   val longDisplayName: String
      get() {
         return if (this.hasFormat) {
            this.unformattedTypeName!!.longDisplayName + "(${this.format!!.joinToString(",")})"
         } else {
            qualifiedName.longDisplayName
         }
      }

   // Note : Lazy evaluation to work around that aliases are partiall populated during
   // construction.
   // If changing, make sure tests pass.
   @get:JsonIgnore // Double check that we really need this on the client.  Also, favour sending QualifiedName rather than full type
//   @get:JsonView(TypeFullView::class)
   val inheritanceGraph = calculateInheritanceGraph()

   // Note : Lazy evaluation to work around that aliases are partiall populated during
   // construction.
   // If changing, make sure tests pass.
   @get:JsonView(TypeFullView::class)
   @get:JsonProperty("isCollection")
   val isCollection: Boolean =
      (listOfNotNull(this.name, this.aliasForTypeName) + this.inheritanceGraph.flatMap {
         listOfNotNull(it.name, it.aliasForTypeName)
      }).any { it.parameterizedName.startsWith(ArrayType.NAME) }

   @get:JsonIgnore
   val collectionType: Type? =
      if (isCollection) {
         underlyingTypeParameters.firstOrNull().let { collectionTypeParam ->
            if (collectionTypeParam == null) {
               // This isn't really the right place to complain about such things,
               // this better served as a linter rule in Taxi
               logger.debug { "Collection does not have a declared type.  Using raw arrays is discouraged.  Will return Any" }
               typeCache.type(PrimitiveType.ANY.qualifiedName.fqn())
            } else {
               collectionTypeParam
            }
         }
      } else {
         null
      }

   @get:JsonProperty("collectionType")
   val collectionTypeName: QualifiedName? = collectionType?.name

   @get:JsonIgnore
   val isEnum: Boolean = resolveAliases().let { underlyingType ->
      underlyingType.taxiType is EnumType
   }

   // Note : Lazy evaluation to work around that aliases are partiall populated during
   // construction.
   // If changing, make sure tests pass.
   @get:JsonView(TypeFullView::class)
   @get:JsonProperty("isScalar")
   val isScalar: Boolean =
      resolveAliases().let { underlyingType ->
         underlyingType.attributes.isEmpty() && !underlyingType.isCollection
      }

   @get:JsonIgnore
   val defaultValues: Map<AttributeName, TypedInstance>? = this.typeCache.defaultValues(this.name)

   fun matches(other: Type, strategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES): Boolean {
      return strategy.matches(this, other)
   }

   fun attribute(name: AttributeName): Field {
      return attributes.getValue(name)
   }

   fun attribute(path: AttributePath): Field {
      val (name, remainder) = path.parts.takeHead()
      val attribute = attribute(name)
      return if (remainder.isEmpty()) {
         attribute
      } else {
         typeCache.type(attribute.type).attribute(AttributePath(remainder))
      }
   }

   fun attribute(identifer: PropertyIdentifier): Field {
      return when (identifer) {
         is PropertyFieldNameIdentifier -> attribute(identifer.name)
         is PropertyTypeIdentifier -> {
            val targetType = typeCache.type(identifer.type.toVyneQualifiedName())
            val candidates = this.attributes.values.map { it to this.typeCache.type(it.type) }
               .filter { (_, fieldType) -> fieldType.isAssignableTo(targetType) }
            when {
               candidates.isEmpty() -> error("No fields assignable to type ${targetType.name.parameterizedName}")
               candidates.size > 1 -> error("Ambiguous field selector - there are ${candidates.size} possible fields assignable to type ${targetType.name.parameterizedName}")
               else -> candidates.first().first
            }
         }
      }
   }

   private fun calculateInheritanceGraph(typesToExclude: List<Type> = emptyList()): List<Type> {
      return if (this.isTypeAlias) {
         this.aliasForType!!.calculateInheritanceGraph()
      } else {
         val allTypesToExclude = typesToExclude + listOf(this)
         this.inherits.flatMap { inheritedType ->
            if (!allTypesToExclude.contains(inheritedType)) {
               setOf(inheritedType) + inheritedType.calculateInheritanceGraph(allTypesToExclude)
            } else emptySet()
         }
      }
   }

   fun isAssignableFrom(other: QualifiedName, considerTypeParameters: Boolean = true): Boolean {
      return isAssignableFrom(this.typeCache.type(other), considerTypeParameters)
   }

   fun isAssignableFrom(other: Type, considerTypeParameters: Boolean = true): Boolean {
      return other.isAssignableTo(this, considerTypeParameters)
   }

   fun isAssignableTo(other: QualifiedName, considerTypeParameters: Boolean = true): Boolean {
      return isAssignableTo(this.typeCache.type(other), considerTypeParameters)
   }

   private val assignableCacheConsideringTypeParams = CacheBuilder
      .newBuilder()
      .build<Type, Boolean>()
   private val assignableCacheNotConsideringTypeParams = CacheBuilder
      .newBuilder()
      .build<Type, Boolean>()

   fun isAssignableTo(other: Type, considerTypeParameters: Boolean = true): Boolean {
      return if (considerTypeParameters) {
         assignableCacheConsideringTypeParams.get(other) {
            calculateIsAssignableTo(other, considerTypeParameters)
         }
      } else {
         assignableCacheNotConsideringTypeParams.get(other) {
            calculateIsAssignableTo(other, considerTypeParameters)
         }
      }
   }

   private fun calculateIsAssignableTo(other: Type, considerTypeParameters: Boolean): Boolean {
      val thisWithoutAliases = this.resolveAliases()
      val otherWithoutAliases = other.resolveAliases()

      if (thisWithoutAliases.resolvesSameAs(otherWithoutAliases, considerTypeParameters)) {
         return true
      }

      // Bail out early
      if (considerTypeParameters && thisWithoutAliases.typeParameters.size != otherWithoutAliases.typeParameters.size) {
         return false
      }

      // Variance rules (simple implementation)
      if (considerTypeParameters && thisWithoutAliases.typeParameters.isNotEmpty()) {
         // To check variance rules, we check that each of the raw types are assignable.
         // This feels like a naieve implementation.
         if (!isAssignableTo(otherWithoutAliases, considerTypeParameters = false)) {
            return false
         }
         thisWithoutAliases.typeParameters.forEachIndexed { index, type ->
            val otherParamType = otherWithoutAliases.typeParameters[index].resolveAliases()
            val thisParamType = type.resolveAliases()
            if (!thisParamType.isAssignableTo(otherParamType)) {
               return false
            }
         }
         return true
      } else {
         return if (thisWithoutAliases.isEnum && otherWithoutAliases.isEnum) {
            // When considering assignment, enums can be assigned to one another up & down the
            // inheritance tree.  We permit this because
            // subtypes of enums aren't allowed to change the set of enum values.
            // Therefore A.SomeValue is assignable to A1 and vice versa, since they are the same
            // underlying value.
            thisWithoutAliases.inheritsFrom(
               otherWithoutAliases,
               considerTypeParameters
            ) || otherWithoutAliases.inheritsFrom(thisWithoutAliases, considerTypeParameters)
         } else {
            thisWithoutAliases.inheritsFrom(otherWithoutAliases, considerTypeParameters)
         }
      }
   }

   private fun calculateResolvedAliases(): Type {
      val resolvedFormattedType = resolveUnderlyingFormattedType()
      return if (!resolvedFormattedType.isTypeAlias) {
         resolvedFormattedType
      } else {
         // Experiment...
         // type aliases for primtiives are a core building block for taxonomies
         // But they're causing problems :
         // type alias Height as Int
         // type alias Weight as Int
         // We clearly didn't mean that Height = Weight
         // Ideally, we need better constructrs in the langauge to suport definint the primitve types.
         // For now, let's stop resolving aliases one step before the primitive
         when {
            aliasForTypeName!!.fullyQualifiedName == ArrayType.NAME -> resolvedFormattedType.aliasForType!!.resolveAliases()
            resolvedFormattedType.aliasForType!!.isPrimitive -> this
            else -> resolvedFormattedType.aliasForType!!.resolveAliases()
         }
      }
   }

   /**
    * Walks down the entire chain of aliases until it hits the underlying non-aliased
    * type
    */
   fun resolveAliases(): Type {
      return resolvedAlias
   }

   // Don't call this directly, use resolveAliases()
   private fun resolveUnderlyingFormattedType(): Type {
      if (this.format == null && this.offset == null) {
         return this
      }
      require(this.inherits.size <= 1) { "A formatted type should have at most 1 supertype" }
      // Same apporoach as below -- stop the inheritence on formatted types before
      // hitting primitives
      if (this.isPrimitive) {
         return this
      }
      if (this.inherits.isEmpty()) {
         return this // probably a type alias
      }
      val superType = this.inherits.first()
      // If our supertype is primitive without being resolved, that means
      // we're an inline format -- ie, a format against an attribute on a type
      //          model ThingWithInlineInstant {
      //            eventDate : Instant( @format = "yyyy-MM-dd'T'HH:mm:ss.SSSX" )
      //         }
      // In this scneario, we want to refer to the primitive, because there's no other option.
      if (superType.isPrimitive && this.format != superType.format) {
         return superType
      }

      // Case for inline 'offset'
      //  model OutputModel {
      //            myField : Instant( @offset = 60 )
      //         }
      if (superType.isPrimitive && this.offset != superType.offset) {
         return superType
      }
      val resolvedSuperType = superType.resolveAliases()
      // Otherwise, if our supertype resolves to a primitive,
      // then we're at the bottom of the inheritence chain, and return this.
      return if (resolvedSuperType.isPrimitive) {
         this
      } else {
         resolvedSuperType
      }
   }

   fun resolvesSameAs(other: Type, considerTypeParameters: Boolean = true): Boolean {
      val thisUnaliasedType = this.resolveAliases()
      val otherUnaliasedType = other.resolveAliases()

      if (considerTypeParameters && (thisUnaliasedType.typeParameters.size != otherUnaliasedType.typeParameters.size)) {
         return false
      }

      val matchesOnName = (thisUnaliasedType.fullyQualifiedName == otherUnaliasedType.fullyQualifiedName)

      val parametersMatch = if (considerTypeParameters) {
         thisUnaliasedType.typeParameters.all { parameterType ->
            val index = thisUnaliasedType.typeParameters.indexOf(parameterType)
            val otherParameterType = otherUnaliasedType.typeParameters[index]
            parameterType.resolvesSameAs(otherParameterType)
         }
      } else {
         true
      }
      return matchesOnName && parametersMatch
   }

   /**
    * Returns true if this type is either the same as, or inherits
    * from the other type.
    *
    * Including checking for equivalent types in the inheritsFrom
    * matches the JVM convention.
    */
   fun inheritsFrom(other: Type, considerTypeParameters: Boolean = true): Boolean {
      if (this.resolveAliases().resolvesSameAs(other.resolveAliases())) {
         return true
      }
      val otherType = other.resolveAliases()
      val result = (this.inheritanceGraph + this).any { thisType ->
         val thisUnaliased = thisType.resolveAliases()
         thisUnaliased.resolvesSameAs(otherType, considerTypeParameters)
      }
      return result
   }

//   private fun inheritsFrom(qualifiedName: QualifiedName): Boolean {
//      // TODO : How does this handle TypeAliases?
//      // Note: This obviously doesn't work properly, as it
//      // ignores generics.
//      // Right now, I'm focussed on isCollection(), which works by looking at
//      // foo.inheritsFrom(lang.taxi.Array).
//      // Need to fix this, but when I do, make sure that isCollection still works.
//      val namesToEvaluate = (setOf(this.name) + this.inheritanceGraph.map { it.name } + setOf(this.aliasForType)).filterNotNull()
//      return namesToEvaluate.any { it.rawTypeEquals(qualifiedName) }
//   }

   fun hasMetadata(name: QualifiedName): Boolean {
      return this.metadata.any { it.name == name }
   }

   fun asArrayType(): Type {
      return this.typeCache.type(QualifiedName("lang.taxi.Array", listOf(this.name.parameterizedName.fqn())))
   }

   /**
    * Returns the type that is most in common with the between this and the
    * other types in the list.
    * Note: This is a placeholder and will currently return Any if there's mixed types.
    * Callers should always code for the scneario that Any is returned, but in a future implementation
    * we'll consider more types within the type hierarchy
    */
   fun commonTypeAncestor(types: List<Type>): Type {
      val resolvedTypes = (types.map { it.resolveAliases() } + this.resolveAliases())
         .map { it.name to it }
         .toMap()
      return if (resolvedTypes.size > 1) {
         this.typeCache.type(PrimitiveType.ANY.qualifiedName.fqn())
      } else {
         resolvedTypes.values.first()
      }
   }

   fun hasAttribute(name: String): Boolean {
      return this.attributes.containsKey(name)
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
