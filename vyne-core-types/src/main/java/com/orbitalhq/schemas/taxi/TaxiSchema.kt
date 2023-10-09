package com.orbitalhq.schemas.taxi

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.Stopwatch
import com.orbitalhq.*
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.schemas.*
import com.orbitalhq.schemas.readers.SourceToTaxiConverter
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import lang.taxi.*
import lang.taxi.messages.Severity
import lang.taxi.packages.TaxiSourcesLoader
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.sources.SourceCodeLanguages
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.StreamType
import lang.taxi.types.TypeReference
import mu.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class TaxiSchema(
   @get:JsonIgnore val document: TaxiDocument,
   @get:JsonIgnore override val packages: List<SourcePackage>,
   override val functionRegistry: FunctionRegistry = FunctionRegistry.default,
   private val queryCacheSize: Long = 100,
//   override val additionalSources: Map<SourcesType, List<SourcePackage>> = emptyMap()
) : Schema {
   override val types: Set<Type>
   override val services: Set<Service>
   override val policies: Set<Policy>

   private val queryCompiler = DefaultQueryCompiler(this, queryCacheSize)

   @get:JsonIgnore
   override val sources: List<VersionedSource> = packages.flatMap { it.sourcesWithPackageIdentifier }

   private val equality = ImmutableEquality(this, TaxiSchema::document, TaxiSchema::sources)
   override fun equals(other: Any?): Boolean {
      return equality.isEqualTo(other)
   }

   override fun asTaxiSchema(): TaxiSchema {
      return this
   }

   override fun hashCode(): Int {
      return equality.hash()
   }

   @get:JsonIgnore
   override val typeCache: TypeCache
   override fun taxiType(name: QualifiedName): lang.taxi.types.Type {
      return taxi.type(name.parameterizedName)
   }

   override val queries: Set<SavedQuery>
      get() {
         return document.queries.map { it.asSavedQuery() }.toSet()
      }

   override val dynamicMetadata: List<QualifiedName> = document.undeclaredAnnotationNames
      .map { it.toVyneQualifiedName() }

   override val metadataTypes: List<QualifiedName> = document.annotations
      .mapNotNull { it.type?.toVyneQualifiedName() }

   private val constraintConverter = TaxiConstraintConverter(this)

   init {
      val stopwatch = Stopwatch.createStarted()
      try {
         val (typeCache, types) = parseTypes(document)
         this.typeCache = typeCache
         this.types = types
         this.services = parseServices(document)
         this.policies = parsePolicies(document)
      } catch (e: Exception) {
         logger.error(e) { "Exception occurred initializing the Taxi Schema" }
         throw e
      }
      logger.debug { "Parsing TaxiSchema took ${stopwatch.elapsed().toMillis()}ms" }
   }

   val hash = (services + types).hashCode()


   @get:JsonIgnore
   override val taxi = document

   private fun parsePolicies(document: TaxiDocument): Set<Policy> {
      return document.policies.map { taxiPolicy ->
         Policy(
            QualifiedName.from(taxiPolicy.qualifiedName),
            this.type(taxiPolicy.targetType.toVyneQualifiedName()),
            taxiPolicy.ruleSets
         )
      }.toSet()
   }

   private fun parseServices(document: TaxiDocument): Set<Service> {
      return document.services.map { taxiService ->
         val lineage = taxiService.lineage?.let { taxiServiceLineage ->
            val consumes = taxiServiceLineage.consumes.map { consumes ->
               ConsumedOperation(consumes.serviceName, consumes.operationName)
            }

            val metadata = parseAnnotationsToMetadata(taxiServiceLineage.annotations)
            ServiceLineage(
               consumes = consumes,
               stores = taxiServiceLineage.stores.map { QualifiedName.from(it.fullyQualifiedName) },
               metadata = metadata
            )
         }
         Service(
            QualifiedName.from(taxiService.qualifiedName),
            queryOperations = taxiService.queryOperations.map { queryOperation ->
               val returnType = this.type(queryOperation.returnType.toVyneQualifiedName())
               QueryOperation(
                  parameters = queryOperation.parameters.map { taxiParam -> parseOperationParameter(taxiParam) },
                  qualifiedName = OperationNames.qualifiedName(taxiService.qualifiedName, queryOperation.name),
                  metadata = parseAnnotationsToMetadata(queryOperation.annotations),
                  grammar = queryOperation.grammar,
                  returnType = returnType,
                  capabilities = queryOperation.capabilities,
                  typeDoc = queryOperation.typeDoc
               )
            },
            operations = taxiService.operations.map { taxiOperation ->
               val returnType = this.type(taxiOperation.returnType.toVyneQualifiedName())
               val parameters = taxiOperation.parameters.map { taxiParam -> parseOperationParameter(taxiParam) }
               Operation(
                  OperationNames.qualifiedName(taxiService.qualifiedName, taxiOperation.name),
                  parameters,
                  operationType = taxiOperation.scope,
                  returnType = returnType,
                  metadata = parseAnnotationsToMetadata(taxiOperation.annotations),
                  contract = constraintConverter.buildContract(
                     returnType, taxiOperation.contract?.returnTypeConstraints
                        ?: emptyList()
                  ),
                  sources = taxiOperation.compilationUnits.toVyneSources(),
                  typeDoc = taxiOperation.typeDoc
               )
            },
            tableOperations = taxiService.tables.map { taxiTable ->
               val returnType = this.type(taxiTable.returnType.toVyneQualifiedName())

               TableOperation.build(
                  qualifiedName = OperationNames.qualifiedName(taxiService.qualifiedName, taxiTable.name),
                  returnType = returnType,
                  metadata = parseAnnotationsToMetadata(taxiTable.annotations),
                  typeDoc = taxiTable.typeDoc,
                  schema = this
               )
               TableOperation.build(
                  qualifiedName = OperationNames.qualifiedName(taxiService.qualifiedName, taxiTable.name),
                  returnType = returnType,
                  metadata = parseAnnotationsToMetadata(taxiTable.annotations),
                  typeDoc = taxiTable.typeDoc,
                  schema = this
               )
            },
            streamOperations = taxiService.streams.map { taxiStream ->
               val returnType = this.type(taxiStream.returnType.toVyneQualifiedName())
               StreamOperation(
                  qualifiedName = OperationNames.qualifiedName(taxiService.qualifiedName, taxiStream.name),
                  returnType = returnType,
                  metadata = parseAnnotationsToMetadata(taxiStream.annotations),
                  typeDoc = taxiStream.typeDoc
               )
            },
            metadata = parseAnnotationsToMetadata(taxiService.annotations),
            sourceCode = taxiService.compilationUnits.toVyneSources(),
            typeDoc = taxiService.typeDoc,
            lineage = lineage
         )
      }.toSet()
   }

   private fun parseOperationParameter(taxiParam: lang.taxi.services.Parameter): Parameter {
      val vyneQualifiedName = taxiParam.type.toVyneQualifiedName()
      val type = this.type(vyneQualifiedName)
      return Parameter(
         type = type,
         name = taxiParam.name,
         metadata = parseAnnotationsToMetadata(taxiParam.annotations),
         constraints = constraintConverter.buildConstraints(type, taxiParam.constraints),
         typeDoc = taxiParam.typeDoc,
         nullable = taxiParam.nullable
      )
   }

   private fun parseAnnotationsToMetadata(annotations: List<Annotation>): List<Metadata> {
      return annotations.map { Metadata(it.name.fqn(), it.parameters) }
   }

   private fun parseTypes(document: TaxiDocument): Pair<TypeCache, Set<Type>> {
      // Register primitives, as they're implicitly defined
      val typeCache = TaxiTypeCache(document, this)
      return typeCache to typeCache.types
   }


   fun merge(schema: TaxiSchema): TaxiSchema {
      return TaxiSchema(
         this.document.merge(schema.document),
         this.packages + schema.packages,
         this.functionRegistry.merge(schema.functionRegistry),
//         additionalSources = this.additionalSources.mergeLists(schema.additionalSources)
      )
   }

   override fun parseQuery(vyneQlQuery: TaxiQLQueryString): Pair<TaxiQlQuery, QueryOptions> {
      return queryCompiler.compile(vyneQlQuery)
   }

   companion object {
      enum class TaxiSchemaErrorBehaviour {
         RETURN_EMPTY,
         THROW_EXCEPTION
      }

      val taxiPrimitiveTypes: Set<Type> = try {
         // Use a cache of only taxi types initially.
         // These will be migrated to other type caches as they are created
         val taxiTypeCache = DefaultTypeCache()
         (PrimitiveType.values().toList() + ArrayType.untyped() + StreamType.untyped() + TypeReference.untyped())
            .map { taxiPrimitive ->
               taxiTypeCache.add(
                  Type(
                     taxiPrimitive.qualifiedName.fqn(),
                     modifiers = TaxiTypeMapper.parseModifiers(taxiPrimitive),
                     sources = listOf(VersionedSource.sourceOnly("Native")),
                     typeDoc = taxiPrimitive.typeDoc,
                     taxiType = taxiPrimitive,
                     typeCache = taxiTypeCache
                  )
               )
            }.toSet()
      } catch (e: Exception) {
         logger.error(e) { "Failed to parse TaxiPrimitiveTypes.  This is a fatal error" }
         emptySet()
      }

      fun forPackageAtPath(path: Path): TaxiSchema {
         return from(TaxiSourcesLoader.loadPackage(path).asSourcePackage())
      }

      fun empty(): TaxiSchema {
         return fromPackages(emptyList()).second
      }

      fun fromPackages(
         packages: List<SourcePackage>,
         imports: List<TaxiSchema> = emptyList(),
         functionRegistry: FunctionRegistry = FunctionRegistry.default,
         sourceConverters: List<SourceToTaxiConverter> = listOf(TaxiSourceConverter)
      ): Pair<List<CompilationError>, TaxiSchema> {
         return this.compiled(packages, imports, functionRegistry, sourceConverters)
      }

      fun compiled(
         packages: List<SourcePackage>,
         imports: List<TaxiSchema> = emptyList(),
         functionRegistry: FunctionRegistry = FunctionRegistry.default,
         sourceConverters: List<SourceToTaxiConverter> = listOf(TaxiSourceConverter)
      ): Pair<List<CompilationError>, TaxiSchema> {
         val stopwatch = Stopwatch.createStarted()

         // TODO : We need to improve the processing order here, to consider
         // import / dependencies between projects.
         val packagesByLanguage = packages
            .filter { it.languages.isNotEmpty() }
            .groupBy {
            it.languages.singleOrNull()
               ?: error("Package ${it.identifier} contains multiple languages, which is not currently supported")
         }.toSortedMap { o1, o2 ->
               when {
                  o1 == SourceCodeLanguages.TAXI && o2 == SourceCodeLanguages.TAXI -> 0
                  o1 == SourceCodeLanguages.TAXI && o2 != SourceCodeLanguages.TAXI -> -1
                  o1 != SourceCodeLanguages.TAXI && o2 == SourceCodeLanguages.TAXI -> 1
                  else -> 0
               }
            }

         val importedTaxiDocs = imports.map { it.taxi }

         val empty = emptyList<CompilationError>() to TaxiDocument.empty()
         val (compilationErrors, doc) = packagesByLanguage.values.fold(empty) { acc, sourcePackages ->
            val (accErrors, accTaxiDoc) = acc
            val firstSourcePackage = sourcePackages.first()
            val converter = sourceConverters.firstOrNull { it.canLoad(firstSourcePackage) }
            if (converter == null) {
               logger.warn { "No converters provided capable of converting sources of languages(s): ${firstSourcePackage.languages.joinToString()}. This source package is being ignored." }
               acc
            } else {
               val (errors, doc) = converter.loadAll(sourcePackages, listOf(accTaxiDoc) + importedTaxiDocs)
               // TODO : Need to get smarter about how errors are handled.
               // Currently, an error in an earlier compilation may be resolved by a later compilation.
               // However, it may not be, and at present, it may not be re-reported, as it's part of the
               // compiled imports.
               // Basically, this approach is wrong.  We don't report some errors, and we report other errors
               // incorrectly.
               (errors + accErrors) to accTaxiDoc.merge(doc)
            }

         }


         logger.debug { "Compilation of TaxiSchema took ${stopwatch.elapsed().toMillis()}ms" }

         // This is to prevent startup errors if there are compilation errors.
         // If we don't, then the main thread can error, causing
         val schemaErrors = compilationErrors.filter { it.severity == Severity.ERROR }
         val schemaWarnings = compilationErrors.filter { it.severity == Severity.WARNING }
         when {
            schemaErrors.isNotEmpty() -> {
               logger.info {
                  "There were ${schemaErrors.size} compilation errors found in sources. \n ${
                     compilationErrors.errors().toMessage()
                  }"
               }
            }

            compilationErrors.any { it.severity == Severity.WARNING } -> {
               logger.info { "There are ${schemaWarnings.size} warning found in the sources" }
            }

            compilationErrors.isNotEmpty() -> {
               logger.info { "Compiler provided the following messages: \n ${compilationErrors.toMessage()}" }
            }
         }
         return compilationErrors to TaxiSchema(
            doc,
            packages,
            functionRegistry,
         )

      }

      private fun <A, B> List<Pair<A, B>>.groupByPairFirstValue(): List<Pair<A, List<B>>> {
         return this.groupBy { it.first }
            .map { (a, b) -> a to b.map { it.second } }
      }


      /**
       * Returns a schema.  If compilation errors exist, defers to the onErrorBehaviour.
       *
       * By default, we return an empty schema when errors exist, becuase throwing an exception
       * can cause the application runtime to crash, which is very bad.
       *
       * In tests, you should use THROW_EXCEPTION
       * You should consider using compiled() instead.
       */
      fun from(
         packages: List<SourcePackage>,
         imports: List<TaxiSchema> = emptyList(),
         onErrorBehaviour: TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY,
         sourceConverters: List<SourceToTaxiConverter> = listOf(TaxiSourceConverter)
      ): TaxiSchema {
         val (messages, schema) = compiled(packages, imports, sourceConverters = sourceConverters)
         val errors = messages.errors()
         return when {
            errors.isEmpty() -> schema
            errors.isNotEmpty() && onErrorBehaviour == TaxiSchemaErrorBehaviour.RETURN_EMPTY -> schema
            else -> throw CompilationException(errors)
         }
      }


      fun from(
         sourcePackage: SourcePackage,
         importSources: List<TaxiSchema> = emptyList(),
         onErrorBehaviour: TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY
      ): TaxiSchema {

         return from(listOf(sourcePackage), importSources, onErrorBehaviour)
      }

      /**
       * This should only be used in tests
       */
      fun fromStrings(
         vararg taxi: String,
         importSources: List<TaxiSchema> = emptyList(),
         onErrorBehaviour: TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY
      ): TaxiSchema {
         return fromStrings(taxi.toList(), importSources, onErrorBehaviour)
      }

      /**
       * This should only be used in tests
       */
      fun fromStrings(
         taxi: List<String>,
         importSources: List<TaxiSchema> = emptyList(),
         onErrorBehaviour: TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY,
      ): TaxiSchema {
         return from(
            taxi.map { VersionedSource.sourceOnly(it) }.asDummySourcePackages(), importSources, onErrorBehaviour
         )
      }

      /**
       * This should only be used in tests
       */
      fun from(
         taxi: String,
         sourceName: String = "<unknown>",
         version: String = VersionedSource.DEFAULT_VERSION.toString(),
         importSources: List<TaxiSchema> = emptyList(),
         onErrorBehaviour: TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY
      ): TaxiSchema {
         return from(
            listOf(VersionedSource(sourceName, version, taxi)).asDummySourcePackages(),
            importSources,
            onErrorBehaviour
         )
      }

      fun compiled(
         taxi: String,
         sourceName: String = "<unknown>",
         version: String = VersionedSource.DEFAULT_VERSION.toString(),
         importSources: List<TaxiSchema> = emptyList(),
         functionRegistry: FunctionRegistry = FunctionRegistry.default
      ): Pair<List<CompilationError>, TaxiSchema> {
         return compiled(
            listOf(VersionedSource(sourceName, version, taxi)).asDummySourcePackages(),
            importSources,
            functionRegistry
         )
      }

      fun compileOrFail(
         taxi: String,
         sourceName: String = "<unknown>",
         version: String = VersionedSource.DEFAULT_VERSION.toString(),
         importSources: List<TaxiSchema> = emptyList(),
         functionRegistry: FunctionRegistry = FunctionRegistry.default
      ): TaxiSchema {
         val (messages, schema) = compiled(taxi, sourceName, version, importSources, functionRegistry)
         if (messages.errors().isNotEmpty()) {
            throw CompilationException(messages)
         }
         return schema
      }

      private fun List<VersionedSource>.asDummySourcePackages(): List<SourcePackage> {
         return listOf(SourcePackage(PackageMetadata.from("com.orbitalhq", "dummy", "0.1.0"), this, emptyMap()))
      }
   }
}

fun List<lang.taxi.types.FieldModifier>.toVyneFieldModifiers(): List<FieldModifier> {
   return this.map { FieldModifier.valueOf(it.name) }
}

private fun lang.taxi.types.QualifiedName.toVyneQualifiedName(): QualifiedName {
   return QualifiedName.from(this.toString(), this.parameters.map { it.toVyneQualifiedName() })
}

fun lang.taxi.types.Type.toVyneQualifiedName(): QualifiedName {
   return this.toQualifiedName().toVyneQualifiedName()
}

fun lang.taxi.types.Type.toVyneType(schema: Schema): Type {
   return schema.type(this.toVyneQualifiedName())
}


private fun lang.taxi.sources.SourceCode.toVyneSource(packageIdentifier: PackageIdentifier?): VersionedSource {
   // TODO : Find the version.
   return VersionedSource(
      this.sourceName,
      VersionedSource.DEFAULT_VERSION.toString(),
      this.content,
      packageIdentifier,
      language = this.language
   )
}

private fun lang.taxi.sources.SourceCode.toVyneSource(): VersionedSource {
   // TODO : Find the version.
   return VersionedSource(
      this.sourceName,
      VersionedSource.DEFAULT_VERSION.toString(),
      this.content,
      language = this.language
   )
}


fun List<lang.taxi.types.CompilationUnit>.toVyneSources(packageIdentifier: PackageIdentifier?): List<VersionedSource> {
   return this.map { it.source.toVyneSource(packageIdentifier) }
}

fun List<lang.taxi.types.CompilationUnit>.toVyneSources(): List<VersionedSource> {
   return this.map { it.source.toVyneSource() }
}

fun List<CompilationError>.toMessage(): String {
   return this.joinToString("\n") { it.toString() }
}


fun TaxiQlQuery.asSavedQuery(): SavedQuery {
   return SavedQuery(
      this.name.toVyneQualifiedName(),
      this.compilationUnits.toVyneSources(),
      null  // TODO : URL
   )
}


/**
 * Creates a map that contains all the keys of both maps.
 * The lists are merged, containing all elements from both maps.
 */
fun <K, V> Map<K, List<V>>.mergeLists(other: Map<K, List<V>>): Map<K, List<V>> {
   return (this.keys + other.keys).associateWith { key ->
      val merged = (this[key] ?: emptyList()) + (other[key] ?: emptyList())
      merged
   }
}
