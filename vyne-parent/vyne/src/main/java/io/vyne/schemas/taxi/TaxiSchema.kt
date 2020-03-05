package io.vyne.schemas.taxi

import com.google.common.collect.ArrayListMultimap
import io.vyne.SchemaAggregator
import io.vyne.schemas.*
import io.vyne.schemas.Field
import io.vyne.schemas.FieldModifier
import io.vyne.schemas.Modifier
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.SourceCode
import io.vyne.schemas.Type
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.services.Constraint
import lang.taxi.types.*
import lang.taxi.types.Annotation
import org.antlr.v4.runtime.CharStreams
import java.io.Serializable

class TaxiSchema(private val document: TaxiDocument, val sources: List<NamedSource>) : Schema {
   override val types: Set<Type>
   override val services: Set<Service>
   override val policies: Set<Policy>
   override val typeCache: TypeCache
   private val constraintConverter = TaxiConstraintConverter(this)

   init {
      this.types = parseTypes(document)
      this.typeCache = DefaultTypeCache(this.types)
      this.services = parseServices(document)
      this.policies = parsePolicies(document)
   }

   private fun parsePolicies(document: TaxiDocument): Set<Policy> {
      return document.policies.map { taxiPolicy ->
         Policy(QualifiedName(taxiPolicy.qualifiedName),
            this.type(taxiPolicy.targetType.toVyneQualifiedName()),
            taxiPolicy.ruleSets
         )
      }.toSet()
   }

   private fun parseServices(document: TaxiDocument): Set<Service> {
      return document.services.map { taxiService ->
         // hahahaha
         Service(QualifiedName(taxiService.qualifiedName),
            operations = taxiService.operations.map { taxiOperation ->
               val returnType = this.type(taxiOperation.returnType.toVyneQualifiedName())
               Operation(OperationNames.qualifiedName(taxiService.qualifiedName, taxiOperation.name),
                  taxiOperation.parameters.map { taxiParam ->
                     val type = this.type(taxiParam.type.qualifiedName)
                     Parameter(
                        type = type,
                        name = taxiParam.name,
                        metadata = parseAnnotationsToMetadata(taxiParam.annotations),
                        constraints = constraintConverter.buildConstraints(type, taxiParam.constraints)
                     )
                  },
                  operationType = taxiOperation.scope,
                  returnType = returnType,
                  metadata = parseAnnotationsToMetadata(taxiOperation.annotations),
                  contract = constraintConverter.buildContract(returnType, taxiOperation.contract?.returnTypeConstraints
                     ?: emptyList()),
                  sources = taxiOperation.compilationUnits.toVyneSources()
               )
            },
            metadata = parseAnnotationsToMetadata(taxiService.annotations),
            sourceCode = taxiService.compilationUnits.toVyneSources()
         )
      }.toSet()
   }

   private fun parseAnnotationsToMetadata(annotations: List<Annotation>): List<Metadata> {
      return annotations.map { Metadata(it.name.fqn(), it.parameters) }
   }

   private fun parseTypes(document: TaxiDocument): Set<Type> {
      val rawTypes = mutableSetOf<Type>()
      val typesWithInheritence = ArrayListMultimap.create<Type, String>()
      // Register primitives, as they're implicitly defined
      rawTypes.addAll(getTaxiPrimitiveTypes())

      document.types.forEach { taxiType: lang.taxi.types.Type ->
         when (taxiType) {
            is ObjectType -> {
               val typeName = QualifiedName(taxiType.qualifiedName)
               val fields = taxiType.allFields.map { field ->
                  when (field.type) {
                     is ArrayType -> field.name to Field(
                        TypeReference((field.type as ArrayType).type.qualifiedName.fqn(), isCollection = true),
                        field.modifiers.toVyneFieldModifiers(),
                        accessor = field.accessor,
                        readCondition = field.readCondition
                     )
                     else -> field.name to Field(
                        TypeReference(field.type.qualifiedName.fqn()),
                        constraintProvider = buildDeferredConstraintProvider(field.type.qualifiedName.fqn(), field.constraints),
                        modifiers = field.modifiers.toVyneFieldModifiers(),
                        accessor = field.accessor,
                        readCondition = field.readCondition)
                  }
               }.toMap()
               val modifiers = parseModifiers(taxiType)
               val type = Type(typeName, fields, modifiers, sources = taxiType.compilationUnits.toVyneSources(), typeDoc = taxiType.typeDoc)
               rawTypes.add(type)
               if (taxiType.inheritsFrom.isNotEmpty()) {
                  typesWithInheritence.putAll(type, taxiType.inheritsFromNames)
               }
            }
            is TypeAlias -> {
               rawTypes.add(Type(QualifiedName(taxiType.qualifiedName), aliasForType = QualifiedName(taxiType.aliasType!!.qualifiedName), sources = taxiType.compilationUnits.toVyneSources(), typeDoc = taxiType.typeDoc))
            }
            is EnumType -> {
               val enumValues = taxiType.values.map { it.name }
               rawTypes.add(Type(QualifiedName(taxiType.qualifiedName), modifiers = parseModifiers(taxiType), enumValues = enumValues, sources = taxiType.compilationUnits.toVyneSources(), typeDoc = taxiType.typeDoc))
            }
            is ArrayType -> TODO()
            else -> rawTypes.add(Type(QualifiedName(taxiType.qualifiedName), modifiers = parseModifiers(taxiType), sources = taxiType.compilationUnits.toVyneSources(), typeDoc = null))
         }
      }
      val originalTypes = rawTypes.associateBy { it.fullyQualifiedName }


      // Now we have a full set of types, expand
      // references to other types - ie., typeAliased types & inheritence,
      // so they have the correct fields / modifiers /etc
      val typesWithAliases = rawTypes.map { rawType ->
         val inheritedTypes = if (typesWithInheritence.containsKey(rawType)) {
            typesWithInheritence[rawType].map { inheritedType ->
               originalTypes[inheritedType]
                  ?: error("Type ${rawType.fullyQualifiedName} inherits from type $inheritedType, which doesn't exist")
            }
         } else {
            emptyList()
         }

         if (rawType.isTypeAlias) {
            val aliasedType = originalTypes[rawType.aliasForType!!.fullyQualifiedName]
               ?: error("Type ${rawType.fullyQualifiedName} is declared as a type alias of type ${rawType.aliasForType!!.fullyQualifiedName}, but that type doesn't exist")
            aliasedType.copy(name = rawType.name, aliasForType = aliasedType.name, inherits = inheritedTypes, sources = rawType.sources)
         } else {
            rawType.copy(inherits = inheritedTypes)
         }
      }.toSet()
      return typesWithAliases
   }

   private fun getTaxiPrimitiveTypes(): Collection<Type> {
      return PrimitiveType.values().map { taxiPrimitive ->
         Type(taxiPrimitive.qualifiedName.fqn(), modifiers = parseModifiers(taxiPrimitive), sources = listOf(SourceCode.native(TaxiSchema.LANGUAGE)), typeDoc = taxiPrimitive.typeDoc)
      }
   }

   private fun buildDeferredConstraintProvider(fqn: QualifiedName, constraints: List<Constraint>): DeferredConstraintProvider {
      return FunctionConstraintProvider {
         val type = this.type(fqn)
         constraintConverter.buildConstraints(type, constraints)
      }
   }

   private fun parseModifiers(type: lang.taxi.types.Type): List<Modifier> {
      return when (type) {
         is EnumType -> listOf(Modifier.ENUM)
         is PrimitiveType -> listOf(Modifier.PRIMITIVE)
         is ObjectType -> type.modifiers.map {
            when (it) {
               lang.taxi.types.Modifier.CLOSED -> Modifier.CLOSED
               lang.taxi.types.Modifier.PARAMETER_TYPE -> Modifier.PARAMETER_TYPE
            }
         }
         else -> emptyList()
      }
   }

   fun merge(schema: TaxiSchema): TaxiSchema {
      return TaxiSchema(this.document.merge(schema.document), this.sources + schema.sources)
   }

   companion object {
      const val LANGUAGE = "Taxi"
      fun from(sources: List<NamedSource>, imports: List<TaxiSchema> = emptyList()): List<TaxiSchema> {
         val typesInSources = sources.flatMap { namedSource ->
            Compiler(namedSource.taxi).declaredTypeNames().map { it to namedSource }
         }.toMap()

         val sourcesWithDependencies = sources.map { namedSource ->
            val dependentSourceFiles = Compiler(namedSource.taxi).declaredImports().mapNotNull { typesInSources[it] }.distinct()
            SourceWithDependencies(namedSource, dependentSourceFiles)
         }

         return DependencyAwareSchemaBuilder(sourcesWithDependencies, imports).build()
      }

      internal fun from(source: NamedSource, importSources: List<TaxiSchema> = emptyList()): TaxiSchema {
         return TaxiSchema(Compiler(CharStreams.fromString(source.taxi, source.sourceName), importSources.map { it.document }).compile(), listOf(source))
      }

      fun from(taxi: String, sourceName: String = "<unknown>", importSources: List<TaxiSchema> = emptyList()): TaxiSchema {
         return from(NamedSource(taxi, sourceName), importSources)
      }
   }
}

private fun List<lang.taxi.types.FieldModifier>.toVyneFieldModifiers(): List<FieldModifier> {
   return this.map { FieldModifier.valueOf(it.name) }
}

class CircularDependencyInSourcesException(message: String) : RuntimeException(message)

private class DependencyAwareSchemaBuilder(val sources: List<SourceWithDependencies>, val importSources: List<TaxiSchema>) {
   private val namedSources: Map<NamedSource, SourceWithDependencies> = sources.associateBy { it.source }
   private val builtSchemas = mutableMapOf<NamedSource, TaxiSchema>()
   private val schemasBeingBuilt = mutableListOf<SourceWithDependencies>()
   fun build(): List<TaxiSchema> {
      if (sources.isEmpty()) {
         return emptyList()
      }

      // This little nugget compiles the sources in order, where sources that are imported
      // later are compiled earlier.
      // Each iteration appends the total set of compilation sources, so the last unique
      // source to be compiled will contain the full set of types, as parsed out by the compiler.
      // (ie., ensuring that imports have been correctly applied, and that type extensions have been
      // applied).
      // Because of import order, each source may be processed more than once,
      // ie., sources that are imported may be visited twice - first when they were imported
      // by another type, and then again when they are visited as a source of their own.
      // We only consider each source once - the first time it was imported.
      // I have a feeling this may not work forever, but its a good start
      val builtSchemasInOrder = sources.mapNotNull { source ->
         if (builtSchemas.containsKey(source.source)) {
            null
         } else {
            buildWithDependencies(source)
         }
      }

      val combined = builtSchemas.values.reduce(TaxiSchema::merge)

//      Unclear why I was doing this.
//      return listOf(builtSchemasInOrder.last())
      return listOf(combined)
   }

   private fun buildWithDependencies(source: SourceWithDependencies): TaxiSchema {

      if (!builtSchemas.containsKey(source.source)) {
         if (schemasBeingBuilt.contains(source)) {
            val message = "A circular dependency in sources exists: ${schemasBeingBuilt.joinToString(" -> ") { it.source.sourceName }}"
            throw CircularDependencyInSourcesException(message)
         }
         schemasBeingBuilt.add(source)

         // Build dependencies first
         source.dependencies.forEach {
            buildWithDependencies(namedSources[it]!!)
         }

         // Now build the actual file
         getOrBuild(source)

         schemasBeingBuilt.remove(source)
      }

      // It's now guaranteed to be present
      return builtSchemas.getValue(source.source)
   }

   private fun getOrBuild(source: SourceWithDependencies): TaxiSchema {
      return builtSchemas.getOrPut(source.source) {
         val imports = builtSchemas.values + importSources
         val schema = TaxiSchema.from(source.source, imports)
         schema
      }
   }
}

private data class SourceWithDependencies(val source: NamedSource, val dependencies: List<NamedSource>)

data class NamedSource(val taxi: String, val sourceName: String) : Serializable {
   companion object {
      fun unnamed(taxi: String) = NamedSource(taxi, "<unknown>")
      fun unnamed(taxi: List<String>): List<NamedSource> = taxi.map { unnamed(it) }
   }
}

private fun lang.taxi.types.QualifiedName.toVyneQualifiedName(): QualifiedName {
   return QualifiedName(this.toString(), this.parameters.map { it.toVyneQualifiedName() })
}

private fun lang.taxi.types.Type.toVyneQualifiedName(): QualifiedName {
   return this.toQualifiedName().toVyneQualifiedName()
}

private fun lang.taxi.types.SourceCode.toVyneSource(): SourceCode {
   return io.vyne.schemas.SourceCode(this.origin, TaxiSchema.LANGUAGE, this.content)
}

private fun List<lang.taxi.types.CompilationUnit>.toVyneSources(): List<SourceCode> {
   return this.map { it.source.toVyneSource() }
}


class TaxiSchemaAggregator : SchemaAggregator {
   override fun aggregate(schemas: List<Schema>): Pair<Schema?, List<Schema>> {
      val taxiSchemas = schemas.filterIsInstance(TaxiSchema::class.java)
      val remaining = schemas - taxiSchemas
      val aggregatedSchema = if (taxiSchemas.isNotEmpty()) {
         taxiSchemas.reduce { a, b -> a.merge(b) }
      } else null
      return aggregatedSchema to remaining
   }

}
