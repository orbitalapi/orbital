package io.vyne.schemas.taxi

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.VersionedSource
import io.vyne.VersionedSource.Companion.UNNAMED
import io.vyne.schemas.*
import io.vyne.schemas.Field
import io.vyne.schemas.FieldModifier
import io.vyne.schemas.Modifier
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type
import io.vyne.versionedSources
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiSourcesLoader
import lang.taxi.types.*
import lang.taxi.types.Annotation
import org.antlr.v4.runtime.CharStreams
import java.nio.file.Path

class TaxiSchema(val document: TaxiDocument, @get:JsonIgnore override val sources: List<VersionedSource>) : Schema {
   override val types: Set<Type>
   override val services: Set<Service>
   override val policies : Set<Policy>

   @get:JsonIgnore
   override val typeCache: TypeCache
   override fun taxiType(name: QualifiedName): lang.taxi.types.Type {
      return taxi.type(name.fullyQualifiedName)
   }

   private val constraintConverter = TaxiConstraintConverter(this)

   init {
      this.types = parseTypes(document)
      this.typeCache = DefaultTypeCache(this.types)
      this.services = parseServices(document)
      this.policies = parsePolicies(document)
   }

   @get:JsonIgnore
   override val taxi = document

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
      // Register primitives, as they're implicitly defined
      val typeCache = DefaultTypeCache(getTaxiPrimitiveTypes())
      document.types.forEach { taxiType: lang.taxi.types.Type ->
         when (taxiType) {
            is ObjectType -> {
               val typeName = QualifiedName(taxiType.qualifiedName)
               val fields = taxiType.allFields.map { field ->
                  when (field.type) {
                     is ArrayType -> field.name to Field(
                        field.type.toVyneQualifiedName(),
                        field.modifiers.toVyneFieldModifiers(),
                        accessor = field.accessor,
                        readCondition = field.readCondition,
                        typeDoc = field.typeDoc
                     )
                     else -> field.name to Field(
                        field.type.qualifiedName.fqn(),
                        constraintProvider = buildDeferredConstraintProvider(field.type.qualifiedName.fqn(), field.constraints),
                        modifiers = field.modifiers.toVyneFieldModifiers(),
                        accessor = field.accessor,
                        readCondition = field.readCondition,
                        typeDoc = field.typeDoc)
                  }
               }.toMap()
               val modifiers = parseModifiers(taxiType)
               val type = Type(
                  typeName,
                  fields,
                  modifiers,
                  inheritsFromTypeNames = taxiType.inheritsFromNames.map { it.fqn() },
                  metadata = parseAnnotationsToMetadata(taxiType.annotations),
                  sources = taxiType.compilationUnits.toVyneSources(),
                  typeDoc = taxiType.typeDoc,
                  taxiType = taxiType
               )
               typeCache.add(type)
            }
            is TypeAlias -> {
               typeCache.add(Type(
                  QualifiedName(taxiType.qualifiedName),
                  metadata = parseAnnotationsToMetadata(taxiType.annotations),
                  aliasForTypeName = taxiType.aliasType!!.toQualifiedName().toVyneQualifiedName(),
                  sources = taxiType.compilationUnits.toVyneSources(),
                  typeDoc = taxiType.typeDoc,
                  taxiType = taxiType
               ))
            }
            is EnumType -> {
               val enumValues = taxiType.values.map { it.name }
               typeCache.add(Type(
                  QualifiedName(taxiType.qualifiedName),
                  modifiers = parseModifiers(taxiType),
                  metadata = parseAnnotationsToMetadata(taxiType.annotations),
                  enumValues = enumValues,
                  sources = taxiType.compilationUnits.toVyneSources(),
                  typeDoc = taxiType.typeDoc,
                  taxiType = taxiType
               ))
            }
            is ArrayType -> TODO()
            else -> typeCache.add(Type(
               QualifiedName(taxiType.qualifiedName),
               modifiers = parseModifiers(taxiType),
               sources = taxiType.compilationUnits.toVyneSources(),
               taxiType = taxiType,
               typeDoc = null
            ))
         }
      }
      return typeCache.types
   }

   private fun getTaxiPrimitiveTypes(): Set<Type> {
      return PrimitiveType.values().map { taxiPrimitive ->
         Type(
            taxiPrimitive.qualifiedName.fqn(),
            modifiers = parseModifiers(taxiPrimitive),
            sources = listOf(VersionedSource.sourceOnly("Native")),
            typeDoc = taxiPrimitive.typeDoc,
            taxiType = taxiPrimitive
         )
      }.toSet()
   }

   private fun buildDeferredConstraintProvider(fqn: QualifiedName, constraints: List<lang.taxi.services.operations.constraints.Constraint>): DeferredConstraintProvider {
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
      fun forPackageAtPath(path: Path):TaxiSchema {
         return from(TaxiSourcesLoader.loadPackage(path).versionedSources())
      }

      fun from(sources: List<VersionedSource>, imports: List<TaxiSchema> = emptyList()): TaxiSchema {
         val doc = Compiler(sources.map { CharStreams.fromString(it.content,it.name) }, imports.map { it.document }).compile()
         return TaxiSchema(doc,sources)
//         val typesInSources = sources.flatMap { namedSource ->
//            Compiler(namedSource.content, namedSource.id).declaredTypeNames().map { it to namedSource }
//         }.toMap()
//
//         val sourcesWithDependencies = sources.map { namedSource ->
//            val dependentSourceFiles = Compiler(namedSource.content)
//               .declaredImports()
//               .mapNotNull { typesInSources[it] }
//               .distinct()
//               .filter { it.name == UNNAMED || (it.name != namedSource.name) }
//            SourceWithDependencies(namedSource, dependentSourceFiles)
//         }
//
//         return DependencyAwareSchemaBuilder(sourcesWithDependencies, imports).build()
      }
//      private fun from(sources: List<NamedSource>, imports: List<TaxiSchema> = emptyList()): TaxiSchema {
//         val typesInSources = sources.flatMap { namedSource ->
//            Compiler(namedSource.taxi).declaredTypeNames().map { it to namedSource }
//         }.toMap()
//
//         val sourcesWithDependencies = sources.map { namedSource ->
//            val dependentSourceFiles = Compiler(namedSource.taxi).declaredImports().mapNotNull { typesInSources[it] }.distinct()
//            SourceWithDependencies(namedSource, dependentSourceFiles)
//         }
//
//         return DependencyAwareSchemaBuilder(sourcesWithDependencies, imports).build()
//      }

      fun from(source: VersionedSource, importSources: List<TaxiSchema> = emptyList()): TaxiSchema {
         return TaxiSchema(Compiler(CharStreams.fromString(source.content, source.name), importSources.map { it.document }).compile(), listOf(source))
      }

      fun from(taxi: String, sourceName: String = "<unknown>", version: String = VersionedSource.DEFAULT_VERSION.toString(), importSources: List<TaxiSchema> = emptyList()): TaxiSchema {
         return from(VersionedSource(sourceName, version, taxi), importSources)
      }
   }
}

private fun List<lang.taxi.types.FieldModifier>.toVyneFieldModifiers(): List<FieldModifier> {
   return this.map { FieldModifier.valueOf(it.name) }
}

class CircularDependencyInSourcesException(message: String) : RuntimeException(message)

private class DependencyAwareSchemaBuilder(val sources: List<SourceWithDependencies>, val importSources: List<TaxiSchema>) {
   private val namedSources: Map<VersionedSource, SourceWithDependencies> = sources.associateBy { it.source }
   private val builtSchemas = mutableMapOf<VersionedSource, TaxiSchema>()
   private val schemasBeingBuilt = mutableListOf<SourceWithDependencies>()
   // Note: This contract used to return a List<TaxiSchema>, but I can't
   // remember why, so I've dropped it back to return a single
   // We're tring to handle folding all the imports together in a smart way,
   // so there shoulnd't be a need to reutnr multiple.
   // If I remember why, swap it back and document it here.
   fun build(): TaxiSchema {
      if (sources.isEmpty()) {
         error("Cannot call build without providing sources")
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

      return builtSchemas.values.reduce(TaxiSchema::merge)
   }

   private fun buildWithDependencies(source: SourceWithDependencies): TaxiSchema {

      if (!builtSchemas.containsKey(source.source)) {
         if (schemasBeingBuilt.contains(source)) {
            val message = "A circular dependency in sources exists: ${schemasBeingBuilt.joinToString(" -> ") { it.source.name }}"
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

private data class SourceWithDependencies(val source: VersionedSource, val dependencies: List<VersionedSource>)

private fun lang.taxi.types.QualifiedName.toVyneQualifiedName(): QualifiedName {
   return QualifiedName(this.toString(), this.parameters.map { it.toVyneQualifiedName() })
}

private fun lang.taxi.types.Type.toVyneQualifiedName(): QualifiedName {
   return this.toQualifiedName().toVyneQualifiedName()
}

private fun lang.taxi.sources.SourceCode.toVyneSource(): VersionedSource {
   // TODO : Find the version.
   return VersionedSource(this.sourceName,VersionedSource.DEFAULT_VERSION.toString(),this.content)
}

private fun List<lang.taxi.types.CompilationUnit>.toVyneSources(): List<VersionedSource> {
   return this.map { it.source.toVyneSource() }
}
