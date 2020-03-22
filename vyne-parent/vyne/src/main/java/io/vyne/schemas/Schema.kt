package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import io.vyne.query.TypeMatchingStrategy
import io.vyne.schemas.taxi.DeferredConstraintProvider
import io.vyne.schemas.taxi.EmptyDeferredConstraintProvider
import io.vyne.utils.assertingThat
import lang.taxi.types.Accessor
import lang.taxi.types.FieldSetCondition
import lang.taxi.types.PrimitiveType
import java.io.IOException
import java.io.Serializable
import java.io.StreamTokenizer
import java.io.StringReader


fun String.fqn(): QualifiedName {

   return when {
      OperationNames.isName(this) -> QualifiedName(this, emptyList())
      ParamNames.isParamName(this) -> QualifiedName("param/" + ParamNames.typeNameInParamName(this).fqn().parameterizedName)
      else -> parse(this).toQualifiedName()
   }

}

private data class GenericTypeName(val baseType: String, val params: List<GenericTypeName>) {
   fun toQualifiedName(): QualifiedName {
      return QualifiedName(this.baseType, this.params.map { it.toQualifiedName() })
   }
}

private fun parse(s: String): GenericTypeName {
   val expandedName = convertArrayShorthand(s)
   val tokenizer = StreamTokenizer(StringReader(expandedName))
//   tokenizer.w .wordChars(".",".")
   try {
      tokenizer.nextToken()  // Skip "BOF" token
      return parse(tokenizer)
   } catch (e: IOException) {
      throw RuntimeException()
   }

}


// Converts Foo[] to lang.taxi.Array<Foo>
private fun convertArrayShorthand(name: String): String {
   if (name.endsWith("[]")) {
      val arrayType = name.removeSuffix("[]")
      return PrimitiveType.ARRAY.qualifiedName + "<$arrayType>"
   } else {
      return name
   }
}

private fun parse(tokenizer: StreamTokenizer): GenericTypeName {
   val baseName = tokenizer.sval
   tokenizer.nextToken()
   val params = mutableListOf<GenericTypeName>()
   if (tokenizer.ttype == '<'.toInt()) {
      do {
         tokenizer.nextToken()  // Skip '<' or ','
         params.add(parse(tokenizer))
      } while (tokenizer.ttype == ','.toInt())
      tokenizer.nextToken()  // skip '>'
   }
   return GenericTypeName(baseName, params)
}


data class Metadata(val name: QualifiedName, val params: Map<String, Any?> = emptyMap())

// A pointer to a type.
// Useful when parsing, and the type that we're referring to may not have been parsed yet.
// TODO : Move ConstraintProvider, since that's not an attribute of a TypeReference, and now we have fields
// TODO : Remove isCollection, and favour Array<T> types
data class TypeReference(val name: QualifiedName,
                         @Deprecated("Replace with lang.taxi.Array<T> types")
                         val isCollection: Boolean = false) {
   val fullyQualifiedName: String
      get() = name.fullyQualifiedName
}

// Part of the migration back to Taxi types
fun lang.taxi.types.Type.asVyneTypeReference(): TypeReference {
   // TODO : Resolve isCollection.  Just being lazy.
   return TypeReference(this.qualifiedName.fqn(), false)
}

data class QualifiedName(val fullyQualifiedName: String, val parameters: List<QualifiedName> = emptyList()) : Serializable {
   val name: String
      get() = fullyQualifiedName.split(".").last()

   val parameterizedName: String
      get() {
         return if (parameters.isEmpty()) {
            fullyQualifiedName
         } else {
            val params = this.parameters.joinToString(",") { it.parameterizedName }
            "$fullyQualifiedName<$params>"
         }
      }

   fun rawTypeEquals(other: QualifiedName): Boolean {
      return this.fullyQualifiedName == other.fullyQualifiedName
   }

   val namespace: String
      get() {
         return fullyQualifiedName.split(".").dropLast(1).joinToString(".")
      }

   override fun toString(): String = fullyQualifiedName
}

typealias OperationName = String
typealias ServiceName = String

object ParamNames {
   fun isParamName(input: String): Boolean {
      return input.startsWith("param/")
   }

   fun typeNameInParamName(paramName: String): String {
      return paramName.removePrefix("param/")
   }

   fun toParamName(typeName: String): String {
      return "param/$typeName"
   }
}

object OperationNames {
   private const val DELIMITER: String = "@@"
   fun name(serviceName: String, operationName: String): String {
      return listOf(serviceName, operationName).joinToString(DELIMITER)
   }

   fun displayName(serviceName: String, operationName: OperationName): String {
      return "$serviceName.$operationName()"
   }

   fun qualifiedName(serviceName: ServiceName, operationName: OperationName): QualifiedName {
      return name(serviceName, operationName).fqn()
   }

   fun serviceAndOperation(qualifiedOperationName: QualifiedName): Pair<ServiceName, OperationName> {
      val parts = qualifiedOperationName.fullyQualifiedName.split(DELIMITER)
      require(parts.size == 2) { "${qualifiedOperationName.fullyQualifiedName} is not a valid operation name." }
      return parts[0] to parts[1]
   }

   fun operationName(qualifiedOperationName: QualifiedName): OperationName {
      return serviceAndOperation(qualifiedOperationName).second
   }

   fun serviceName(qualifiedOperationName: QualifiedName): ServiceName {
      return serviceAndOperation(qualifiedOperationName).first
   }

   fun isName(memberName: String): Boolean {
      return memberName.contains(DELIMITER)
   }

   fun isName(memberName: QualifiedName): Boolean {
      return memberName.fullyQualifiedName.contains(DELIMITER)
   }

   fun displayNameFromOperationName(operationName: QualifiedName): String {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(operationName)
      return displayName(serviceName, operationName)
   }
}

typealias AttributeName = String

interface SchemaMember {

   @Deprecated(message = "Workaround for https://gitlab.com/vyne/vyne/issues/34.  Will be removed")
   val memberQualifiedName: QualifiedName
      get() {
         return when (this) {
            is Type -> this.name
            is Service -> this.name
            is Operation -> this.qualifiedName
            else -> error("Unhandled SchemaMember type : ${this.javaClass.name}")
         }
      }
}

// Note: I'm progressively moving this towards Taxi schemas, as discussed
// on the Type comment.
data class Field(
   val type: TypeReference,
   val modifiers: List<FieldModifier>,
   private val constraintProvider: DeferredConstraintProvider = EmptyDeferredConstraintProvider(),
   val accessor: Accessor?,
   val readCondition: FieldSetCondition?
) {
   // TODO : Why take the provider, and not the constraints?  I have a feeling it's because
   // we parse fields before we parse their underlying types, so constrains may not be
   // fully resolved at construction time.
   val constraints: List<Constraint> by lazy { constraintProvider.buildConstraints() }
}

interface TypeFullView : TypeLightView
interface TypeLightView

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

   val typeDoc: String?
) : SchemaMember {
   constructor(name: String, attributes: Map<AttributeName, Field> = emptyMap(), modifiers: List<Modifier> = emptyList(), metadata: List<Metadata> = emptyList(), aliasForType: QualifiedName? = null, inherits: List<Type>, enumValues: List<String> = emptyList(), sources: List<SourceCode>, typeDoc: String? = null) : this(name.fqn(), attributes, modifiers, metadata, aliasForType, inherits, enumValues, sources, typeDoc = typeDoc)

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

enum class FieldModifier {
   CLOSED
}

enum class Modifier {
   CLOSED,
   PARAMETER_TYPE,
   // TODO : Is it right to treat these as modifiers?  They're not really,
   // but I'm trying to avoid a big collection of boolean flags
   ENUM,
   PRIMITIVE
}

data class SourceCode(
   val origin: String,
   val language: String,
   val content: String
) {
   companion object {
      fun undefined(language: String): SourceCode {
         return SourceCode("Unknown", language, "")
      }

      fun native(language: String): SourceCode {
         return SourceCode("Native", language, "")
      }
   }
}

class SimpleSchema(override val types: Set<Type>, override val services: Set<Service>) : Schema {

   override val policies: Set<Policy> = emptySet()
   override val typeCache: TypeCache = DefaultTypeCache(this.types)
}

interface Schema {
   val types: Set<Type>
   val services: Set<Service>

   val policies: Set<Policy>

   val typeCache: TypeCache


   val operations: Set<Operation>
      get() = services.flatMap { it.operations }.toSet()

   fun operationsWithReturnType(requiredType: Type, typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES): Set<Pair<Service, Operation>> {
      return services.flatMap { service ->
         service.operations.filter { operation -> typeMatchingStrategy.matches(requiredType, operation.returnType) }
            .map { service to it }
      }.toSet()
   }

   fun type(name: String): Type {
      val type = typeCache.type(name)
      return type
   }

   fun type(name: QualifiedName) = typeCache.type(name)

   fun hasType(name: String) = typeCache.hasType(name)

   fun hasService(serviceName: String): Boolean {
      return this.services.any { it.qualifiedName == serviceName }
   }

   fun service(serviceName: String): Service {
      return this.services.firstOrNull { it.qualifiedName == serviceName }
         ?: throw IllegalArgumentException("Service $serviceName was not found within this schema")
   }

   fun policy(type: Type): Policy? {
      return this.policies.firstOrNull { it.targetType.fullyQualifiedName == type.fullyQualifiedName }
   }

   fun hasOperation(operationName: QualifiedName): Boolean {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(operationName)
      if (!hasService(serviceName)) return false

      val service = service(serviceName)
      return service.hasOperation(operationName)
   }

   fun operation(operationName: QualifiedName): Pair<Service, Operation> {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(operationName)
      val service = service(serviceName)
      return service to service.operation(operationName)
   }

   fun attribute(attributeName: String): Pair<Type, Type> {
      val parts = attributeName.split("/").assertingThat({ it.size == 2 })
      val declaringType = type(parts[0])
      val attributeType = type(declaringType.attributes.getValue(parts[1]).type)

      return declaringType to attributeType

   }

   fun type(nestedTypeRef: TypeReference): Type {
      return type(nestedTypeRef.name)
   }
}


interface TypeCache {
   fun type(name: String): Type
   fun type(name: QualifiedName): Type
   fun hasType(name: String): Boolean
   fun hasType(name: QualifiedName): Boolean
}

data class DefaultTypeCache(private val types: Set<Type>) : TypeCache {
   private val cache: Map<QualifiedName, Type> = types.associateBy { it.name }
   private val shortNames: Map<String, Type>

   init {
      val possibleShortNames: MutableMap<String, Pair<Int, QualifiedName?>> = mutableMapOf()
      cache.forEach { name: QualifiedName, type ->
         possibleShortNames.compute(name.name) { _, existingPair ->
            if (existingPair == null) {
               1 to type.name
            } else {
               existingPair.first + 1 to null
            }
         }
      }
      shortNames = possibleShortNames
         .filter { (shortName, countAndFqn) -> countAndFqn.first == 1 }
         .map { (shortName, countAndFqn) ->
            val type = this.cache[countAndFqn.second!!] ?: error("Expected a type named ${countAndFqn.second!!}")
            shortName to type
         }.toMap()
   }

   override fun type(name: String): Type {
      return type(name.fqn())
   }

   override fun type(name: QualifiedName): Type {
//      if (isArrayType(name)) {
//         val typeNameWithoutArray = name.substringBeforeLast("[]")
//         val type = type(typeNameWithoutArray)
//         TODO()
//      } else {
      return this.cache[name]
         ?: this.shortNames[name.fullyQualifiedName]
         ?: parameterisedType(name)
         ?: throw IllegalArgumentException("Type ${name.parameterizedName} was not found within this schema, and is not a valid short name")
//      }

//      return type(name.fullyQualifiedName)
   }

   private fun parameterisedType(name: QualifiedName): Type? {
      if (name.parameters.isEmpty()) return null

      if (hasType(name.fullyQualifiedName) && name.parameters.all { hasType(it) }) {
         // We've been asked for a parameterized type.
         // All the parameters are correctly defined, but no type exists.
         // This is caused by (for example), a service returning Array<Foo>, where both Array and Foo have been declared as types
         // but not Array<Foo> directly.
         // It's still valid, so we'll construct the type
         val baseType = type(name.fullyQualifiedName)
         val params = name.parameters.map { type(it) }
         return baseType.copy(name = name, typeParameters = params)
      } else {
         return null
      }

   }

   override fun hasType(name: QualifiedName): Boolean {
      if (cache.containsKey(name)) return true
      if (name.parameters.isNotEmpty()) {
         return hasType(name.fullyQualifiedName) // this is the base type
            && name.parameters.all { hasType(it) }
      }
      return false
   }

   override fun hasType(name: String): Boolean {
      return shortNames.containsKey(name) || hasType(name.fqn())
   }
}
