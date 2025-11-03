package com.orbitalhq.schemas.taxi

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.Stopwatch
import com.google.common.base.Throwables
import com.orbitalhq.*
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.schemas.*
import com.orbitalhq.schemas.readers.SourceConverterLoadResult
import com.orbitalhq.schemas.readers.SourceToTaxiConverter
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import lang.taxi.*
import lang.taxi.messages.Severity
import lang.taxi.packages.SourcesTypes
import lang.taxi.packages.TaxiSourcesLoader
import lang.taxi.policies.Policy
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.sources.SourceCodeLanguages
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.CompilationUnit
import lang.taxi.types.PrimitiveType
import lang.taxi.types.StreamType
import lang.taxi.types.TypeReference
import lang.taxi.utils.log
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

class TaxiSchema(
   @get:JsonIgnore val document: TaxiDocument,
   @get:JsonIgnore override val packages: List<SourcePackage>,
   override val functionRegistry: FunctionRegistry = FunctionRegistry.default,
   private val queryCacheSize: Long = 100,
   environmentVariables: Map<String, String> = System.getenv(),
   compilerMessages: List<CompilationMessage> = emptyList()
//   override val additionalSources: Map<SourcesType, List<SourcePackage>> = emptyMap()
) : Schema {
   override val types: Set<Type>
   override val services: Set<Service>
   override val policies: Set<Policy>

   private val _compilerMessages: MutableList<CompilationMessage> = compilerMessages.toMutableList();
   val compilerMessages: List<CompilationMessage>
      get() {
         return _compilerMessages.toList()
      }

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

   private val systemAndLoadedEnvVariables = computeSystemAndLoadedEnvVariables(environmentVariables)

   init {
      val stopwatch = Stopwatch.createStarted()
      try {
         val (typeCache, types) = parseTypes(document)
         this.typeCache = typeCache
         this.types = types
         this.services = parseServices(document)
         this.policies = document.policies
      } catch (e: Exception) {
         logger.error(e) { "Exception occurred initializing the Taxi Schema" }
         throw e
      }
      logger.info { "Parsing TaxiSchema took ${stopwatch.elapsed().toMillis()}ms" }
   }


   /**
    * Returns the env variables that come from both:
    *  - The environmentVariables property passed in to the constructor (typically System.getEnv()
    *  - env.conf in the packages
    *
    *  If parsing using the env.conf fails, then we fallback to ONLY the provided environmentVariables
    */
   private fun computeSystemAndLoadedEnvVariables(environmentVariables: Map<String, String>): Map<String, String> {
      val providedConfig = ConfigFactory.parseMap(environmentVariables)
      val envConfConfigs = this.packages.flatMap { sourcePackage ->
         (sourcePackage.additionalSources[SourcesTypes.ORBITAL_CONFIG].orEmpty())
            .filter { source -> Paths.get(source.name).fileName.toString() == "env.conf" }
            .mapNotNull { source ->
               try {
                  ConfigFactory.parseString(source.content)
               } catch (e: Exception) {
                  val rootCause = Throwables.getRootCause(e)
                  _compilerMessages.add(
                     CompilationMessage(
                        CompilationUnit.Companion.generatedFor(source.name),
                        "Failed to parse configuration file: ${rootCause.message ?: "A ${rootCause::class.simpleName} was thrown without a message"}"
                     )
                  )
                  null
               }
            }
      }
      val configMap = try {
         val allConfigs = listOf(providedConfig) + envConfConfigs
         val config = allConfigs.reduceRight(Config::withFallback)
         config.root().unwrapped()
            .mapValues { (_, value) -> value.toString() }
      } catch (e:Exception) {
         val rootCause = Throwables.getRootCause(e)
         _compilerMessages.add(CompilationError(CompilationUnit.unspecified(), "Failed to resolve env.conf variables - ${rootCause.message}"))
         // Just fall back to the env variables we were provided
         environmentVariables
      }
      return configMap
   }

   @get:JsonIgnore
   override val taxi = document

   override val hash: SchemaHash = (taxi.services + taxi.types).hashCode()

   private fun parseServices(document: TaxiDocument): Set<Service> {
      return document.services.map { taxiService ->
         val lineage = taxiService.lineage?.let { taxiServiceLineage ->
            val consumes = taxiServiceLineage.consumes.map { consumes ->
               ConsumedOperation(consumes.serviceName, consumes.operationName)
            }

            val metadata = parseAnnotationsToMetadata(
               taxiServiceLineage.annotations,
               taxiServiceLineage.compilationUnits.firstOrNull()
            )
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
                  metadata = parseAnnotationsToMetadata(
                     queryOperation.annotations,
                     queryOperation.compilationUnits.firstOrNull()
                  ),
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
                  metadata = parseAnnotationsToMetadata(
                     taxiOperation.annotations,
                     taxiOperation.compilationUnits.firstOrNull()
                  ),
                  contract = OperationContract(
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
                  metadata = parseAnnotationsToMetadata(
                     taxiTable.annotations,
                     taxiTable.compilationUnits.firstOrNull()
                  ),
                  typeDoc = taxiTable.typeDoc,
                  schema = this
               )
               TableOperation.build(
                  qualifiedName = OperationNames.qualifiedName(taxiService.qualifiedName, taxiTable.name),
                  returnType = returnType,
                  metadata = parseAnnotationsToMetadata(
                     taxiTable.annotations,
                     taxiTable.compilationUnits.firstOrNull()
                  ),
                  typeDoc = taxiTable.typeDoc,
                  schema = this
               )
            },
            streamOperations = taxiService.streams.map { taxiStream ->
               val returnType = this.type(taxiStream.returnType.toVyneQualifiedName())
               StreamOperation(
                  qualifiedName = OperationNames.qualifiedName(taxiService.qualifiedName, taxiStream.name),
                  returnType = returnType,
                  metadata = parseAnnotationsToMetadata(
                     taxiStream.annotations,
                     taxiStream.compilationUnits.firstOrNull()
                  ),
                  typeDoc = taxiStream.typeDoc
               )
            },
            metadata = parseAnnotationsToMetadata(
               taxiService.annotations,
               taxiService.compilationUnits.firstOrNull()
            ),
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
         metadata = parseAnnotationsToMetadata(taxiParam.annotations, compilationUnit = null),
         constraints = taxiParam.constraints,
         typeDoc = taxiParam.typeDoc,
         nullable = taxiParam.nullable,
         defaultValue = taxiParam.defaultValue
      )
   }

   private fun parseAnnotationsToMetadata(
      annotations: List<Annotation>,
      compilationUnit: CompilationUnit?
   ): List<Metadata> {
      return annotations.map { annotation ->
         val resolvedParameters = annotation.parameters.mapValues { (paramKey, paramValue) ->
            resolveEnvironmentVariablesInAnnotationValue(paramValue, annotation, compilationUnit)
         }
         Metadata(annotation.name.fqn(), resolvedParameters)

      }
   }

   /**
    * Allows users to specify an env variable in an annotation, and we'll swap it out
    * and resolve it at runtime.
    *
    * eg:
    * ```
    * @KafkaOperation( topic = "${envTopic}", offset = "earliest" )
    * stream comments : Stream<Comment>
    * ```
    *
    * Note - after discussion, we felt like this was a 'runtime' not 'compile time' feature, so should
    * live inside Orbital, not the Taxi compiler.
    *
    * Eg: If this were in Taxi, then compiler errors would get raised in CI/CD scenarios where
    * env variables aren't reasonably expected to be set. Similarly, pulling someone elses project
    * into yours would throw compiler errors if env variables were declared - which aren't required until
    * runtime.
    *
    */
   private fun resolveEnvironmentVariablesInAnnotationValue(
      paramValue: Any?,
      annotation: Annotation,
      compilationUnit: CompilationUnit?
   ): Any? {
      return if (paramValue is String && Metadata.getVariableName(paramValue) != null) {
         val variableName = Metadata.getVariableName(paramValue)!!
         if (systemAndLoadedEnvVariables.containsKey(variableName)) {
            systemAndLoadedEnvVariables[variableName]!!
         } else {
            val messageText =
               "Annotation ${annotation.name} specifies env variable $variableName which is not defined"
            val compilerMessage = if (compilationUnit != null) {
               CompilationMessage(
                  compilationUnit, messageText
               )
            } else {
               CompilationMessage(CompilationUnit.unspecified(), messageText)
            }
            _compilerMessages.add(compilerMessage)
         }
      } else {
         paramValue
      }
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
         compilerMessages = this.compilerMessages + schema.compilerMessages
      )
   }

   override fun parseQuery(
      vyneQlQuery: TaxiQLQueryString,
      useCache: Boolean
   ): Triple<TaxiQlQuery, QueryOptions, Schema> {
      return queryCompiler.compile(vyneQlQuery, useCache)
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
         sourceConverters: List<SourceToTaxiConverter> = listOf(TaxiSourceConverter),
         environmentVariables: Map<String, String> = System.getenv()
      ): Pair<List<CompilationError>, TaxiSchema> {
         return this.compiled(packages, imports, functionRegistry, sourceConverters, environmentVariables)
      }

      fun compiled(
         packages: List<SourcePackage>,
         imports: List<TaxiSchema> = emptyList(),
         functionRegistry: FunctionRegistry = FunctionRegistry.default,
         sourceConverters: List<SourceToTaxiConverter> = listOf(TaxiSourceConverter),
         environmentVariables: Map<String, String> = System.getenv()
      ): Pair<List<CompilationError>, TaxiSchema> {
         val stopwatch = Stopwatch.createStarted()

         // TODO : We need to improve the processing order here, to consider
         // import / dependencies between projects.
         val packagesByLanguage = packages
            .groupBy {
               when (it.languages.size) {
                  0 -> SourceCodeLanguages.TAXI // Default to Taxi if there's nothing there
                  1 -> it.languages.first()
                  else -> {
                     it.languages.first()
                     // We used to error here, but I don't think that matters - the real question is
                     // "Is this taxi?"
//                     error("Package ${it.identifier} contains multiple languages, which is not currently supported")
                  }
               }
            }.toSortedMap { o1, o2 ->
               // Load Taxi first.
               // This is a sloppy workaround to us not supporting dependenices when creating a Taxi Schema.
               // Should be removed once dependency loading has advaned
               when {
                  o1 == SourceCodeLanguages.TAXI && o2 == SourceCodeLanguages.TAXI -> 0
                  o1 == SourceCodeLanguages.TAXI && o2 != SourceCodeLanguages.TAXI -> -1
                  o1 != SourceCodeLanguages.TAXI && o2 == SourceCodeLanguages.TAXI -> 1
                  else -> 0
               }
            }

         val importedTaxiDocs = imports.map { it.taxi }

         val empty = SourceConverterLoadResult.empty()
         val (compilationErrors, doc, sourcePackagesWithConvertedCode) = packagesByLanguage.values.fold(empty) { acc, sourcePackages ->
            val (accErrors, accTaxiDoc) = acc
            val firstSourcePackage = sourcePackages.first()
            val converter = sourceConverters.firstOrNull { it.canLoad(firstSourcePackage) }
            if (converter == null) {
               logger.warn { "No converters provided capable of converting sources of languages(s): ${firstSourcePackage.languages.joinToString()}. This source package is being ignored." }
               acc
            } else {
               val (errors, doc, transpiledSources) = converter.loadAll(
                  sourcePackages,
                  listOf(accTaxiDoc) + importedTaxiDocs
               )
               // TODO : Need to get smarter about how errors are handled.
               // Currently, an error in an earlier compilation may be resolved by a later compilation.
               // However, it may not be, and at present, it may not be re-reported, as it's part of the
               // compiled imports.
               // Basically, this approach is wrong.  We don't report some errors, and we report other errors
               // incorrectly.
               SourceConverterLoadResult(
                  (errors + accErrors),
                  accTaxiDoc.merge(doc),
                  acc.transpiledSource + transpiledSources
               )
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

         val composedFunctionRegistry = buildComposedFunctionRegistry(functionRegistry, sourcePackagesWithConvertedCode)

         val schema = TaxiSchema(
            doc,
            sourcePackagesWithConvertedCode,
            composedFunctionRegistry,
            environmentVariables = environmentVariables,
            compilerMessages = compilationErrors
         )
         // We used to return the compiler messages directly.
         // However, we have some orbital-only compiler messages we want to return,
         // such as env varaibles in annotations that can't be resolved
         // So, we pass the compiler message into the schema, and then return the version
         // that comes back from the parser, so Orbital can inject it's own
         return schema.compilerMessages to schema
      }

      private fun buildComposedFunctionRegistry(
         functionRegistry: FunctionRegistry,
         sourcePackages: List<SourcePackage>
      ): FunctionRegistry {
         val sourcePackageHandlers = sourcePackages.flatMap {
            it.functionHandlers
         }
         return if (sourcePackageHandlers.isEmpty()) {
            logger.info { "Using default function registry, no cutsom handlers provided" }
            functionRegistry
         } else {
            logger.info { "Building FunctionRegistry containing ${sourcePackageHandlers.size} custom functions" }
            val customFunctionRegistry = FunctionRegistry(sourcePackageHandlers)
            functionRegistry.merge(customFunctionRegistry)
         }
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
         sourceConverters: List<SourceToTaxiConverter> = listOf(TaxiSourceConverter),
         environmentVariables: Map<String, String> = System.getenv()
      ): TaxiSchema {
         val (messages, schema) = compiled(
            packages,
            imports,
            sourceConverters = sourceConverters,
            environmentVariables = environmentVariables
         )
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
         onErrorBehaviour: TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY,
         environmentVariables: Map<String, String> = System.getenv()
      ): TaxiSchema {

         return from(
            listOf(sourcePackage),
            importSources,
            onErrorBehaviour,
            environmentVariables = environmentVariables
         )
      }

      /**
       * This should only be used in tests
       */
      fun fromStrings(
         vararg taxi: String,
         importSources: List<TaxiSchema> = emptyList(),
         onErrorBehaviour: TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY,
         environmentVariables: Map<String, String> = System.getenv()
      ): TaxiSchema {
         return fromStrings(taxi.toList(), importSources, onErrorBehaviour, environmentVariables)
      }

      /**
       * This should only be used in tests
       */
      fun fromStrings(
         taxi: List<String>,
         importSources: List<TaxiSchema> = emptyList(),
         onErrorBehaviour: TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY,
         environmentVariables: Map<String, String> = System.getenv()
      ): TaxiSchema {
         return from(
            taxi.map { VersionedSource.sourceOnly(it) }.asDummySourcePackages(),
            importSources,
            onErrorBehaviour,
            environmentVariables = environmentVariables
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
         onErrorBehaviour: TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY,
         environmentVariables: Map<String, String> = System.getenv()
      ): TaxiSchema {
         return from(
            listOf(VersionedSource(sourceName, version, taxi)).asDummySourcePackages(),
            importSources,
            onErrorBehaviour,
            environmentVariables = environmentVariables
         )
      }

      fun compiled(
         taxi: String,
         sourceName: String = "<unknown>",
         version: String = VersionedSource.DEFAULT_VERSION.toString(),
         importSources: List<TaxiSchema> = emptyList(),
         functionRegistry: FunctionRegistry = FunctionRegistry.default,
         environmentVariables: Map<String, String> = System.getenv()
      ): Pair<List<CompilationError>, TaxiSchema> {
         return compiled(
            listOf(VersionedSource(sourceName, version, taxi)).asDummySourcePackages(),
            importSources,
            functionRegistry,
            environmentVariables = environmentVariables
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
         return listOf(
            SourcePackage(
               PackageMetadata.from(VyneTypes.NAMESPACE, "dummy", "0.1.0"),
               this,
               emptyMap(),
               null
            )
         )
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


private fun lang.taxi.sources.SourceCode.toVyneSource(packageIdentifier: PackageIdentifier? = null): VersionedSource {
   // TODO : Find the version.
   val (sourceNamePackageIdentifier, sourceName) = VersionedSource.splitPackageIdentifier(this.sourceName)
   if (packageIdentifier != null && sourceNamePackageIdentifier != null && sourceNamePackageIdentifier != packageIdentifier) {
      log().warn("Converting Taxi source to VersionedSource - two package identifiers are provided which are different - ${packageIdentifier.id} and ${sourceNamePackageIdentifier.id}")
   }
   if (packageIdentifier == null && sourceNamePackageIdentifier == null) {
      log().debug("Constructing VersionedSource without a PackageIdentifier can cause errors with edits")
   }
   val packageIdentifierToUse = packageIdentifier ?: sourceNamePackageIdentifier
   return VersionedSource(
      sourceName,
      packageIdentifier?.version ?: VersionedSource.DEFAULT_VERSION.toString(),
      this.content,
      packageIdentifierToUse,
      language = this.language
   )
}

fun List<lang.taxi.types.CompilationUnit>.toVyneSources(packageIdentifier: PackageIdentifier? = null): List<VersionedSource> {
   return this.map { it.source.toVyneSource(packageIdentifier) }
}

fun List<CompilationError>.toMessage(): String {
   return this.joinToString("\n") { it.toString() }
}


fun TaxiQlQuery.asSavedQuery(packageIdentifier: PackageIdentifier? = null): SavedQuery {
   return SavedQuery(
      this.name.toVyneQualifiedName(),
      this.compilationUnits.toVyneSources(packageIdentifier),
      SavedQuery.QueryKind.forQueryMode(this.queryMode),
      QueryPublications.fromQuery(this)
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
