package io.vyne.schemas.taxi

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.VersionedSource
import io.vyne.schemas.DefaultTypeCache
import io.vyne.schemas.FieldModifier
import io.vyne.schemas.Metadata
import io.vyne.schemas.Operation
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Parameter
import io.vyne.schemas.Policy
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QueryOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.TaxiTypeCache
import io.vyne.schemas.TaxiTypeMapper
import io.vyne.schemas.Type
import io.vyne.schemas.TypeCache
import io.vyne.schemas.fqn
import io.vyne.versionedSources
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.Compiler
import lang.taxi.Equality
import lang.taxi.TaxiDocument
import lang.taxi.errors
import lang.taxi.messages.Severity
import lang.taxi.packages.TaxiSourcesLoader
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging
import org.antlr.v4.runtime.CharStreams
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class TaxiSchema(
   val document: TaxiDocument,
   @get:JsonIgnore override val sources: List<VersionedSource>
) : Schema {
   override val types: Set<Type>
   override val services: Set<Service>
   override val policies: Set<Policy>

   private val equality = Equality(this, TaxiSchema::document, TaxiSchema::sources)
   override fun equals(other: Any?): Boolean {
      return equality.isEqualTo(other)
   }

   override fun hashCode(): Int {
      return equality.hash()
   }

   @get:JsonIgnore
   override val typeCache: TypeCache
   override fun taxiType(name: QualifiedName): lang.taxi.types.Type {
      return taxi.type(name.fullyQualifiedName)
   }

   private val constraintConverter = TaxiConstraintConverter(this)

   init {
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
   }

   @get:JsonIgnore
   override val taxi = document

   private fun parsePolicies(document: TaxiDocument): Set<Policy> {
      return document.policies.map { taxiPolicy ->
         Policy(
            QualifiedName(taxiPolicy.qualifiedName),
            this.type(taxiPolicy.targetType.toVyneQualifiedName()),
            taxiPolicy.ruleSets
         )
      }.toSet()
   }

   private fun parseServices(document: TaxiDocument): Set<Service> {
      return document.services.map { taxiService ->
         Service(
            QualifiedName(taxiService.qualifiedName),
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
               Operation(
                  OperationNames.qualifiedName(taxiService.qualifiedName, taxiOperation.name),
                  taxiOperation.parameters.map { taxiParam -> parseOperationParameter(taxiParam) },
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
            metadata = parseAnnotationsToMetadata(taxiService.annotations),
            sourceCode = taxiService.compilationUnits.toVyneSources(),
            typeDoc = taxiService.typeDoc
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
         constraints = constraintConverter.buildConstraints(type, taxiParam.constraints)
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
      return TaxiSchema(this.document.merge(schema.document), this.sources + schema.sources)
   }

   companion object {
      const val LANGUAGE = "Taxi"
      enum class TaxiSchemaErrorBehaviour {
         RETURN_EMPTY,
         THROW_EXCEPTION
      }
      val taxiPrimitiveTypes: Set<Type> = try {
         // Use a cache of only taxi types initially.
         // These will be migrated to other type caches as they are created
         val taxiTypeCache = DefaultTypeCache()
         (PrimitiveType.values().toList() + ArrayType.untyped())
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
         return from(TaxiSourcesLoader.loadPackage(path).versionedSources())
      }

      fun compiled(
         sources: List<VersionedSource>,
         imports: List<TaxiSchema> = emptyList()
      ): Pair<List<CompilationError>, TaxiSchema> {
         val (compilationErrors, doc) =
            Compiler(
               sources.map { CharStreams.fromString(it.content, it.name) },
               imports.map { it.document }).compileWithMessages()

         // This is to prevent startup errors if there are compilation errors.
         // If we don't, then the main thread can error, causing
         val schemaErrors = compilationErrors.filter { it.severity == Severity.ERROR }
         val schemaWarnings = compilationErrors.filter { it.severity == Severity.WARNING }
         when {
            schemaErrors.isNotEmpty() -> {
               logger.error { "There were ${schemaErrors.size} compilation errors found in sources. \n ${compilationErrors.errors().toMessage()}" }
            }
            compilationErrors.any { it.severity == Severity.WARNING } -> {
               logger.warn { "There are ${schemaWarnings.size} warning found in the sources" }
            }
            compilationErrors.isNotEmpty() -> {
               logger.info { "Compiler provided the following messages: \n ${compilationErrors.toMessage()}" }
            }
         }
         return compilationErrors to TaxiSchema(doc, sources)

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
      fun from(sources: List<VersionedSource>, imports: List<TaxiSchema> = emptyList(), onErrorBehaviour:TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY): TaxiSchema {
         val (messages,schema) =  compiled(sources, imports)
         val errors = messages.errors()
         return when {
            errors.isEmpty() -> schema
            errors.isNotEmpty() && onErrorBehaviour == TaxiSchemaErrorBehaviour.RETURN_EMPTY -> schema
            else -> throw CompilationException(errors)
         }
      }


      fun from(source: VersionedSource, importSources: List<TaxiSchema> = emptyList(), onErrorBehaviour:TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY): TaxiSchema {

         return from(listOf(source), importSources, onErrorBehaviour)
      }

      fun fromStrings(vararg taxi: String, importSources: List<TaxiSchema> = emptyList(), onErrorBehaviour:TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY): TaxiSchema {
         return fromStrings(taxi.toList(), importSources, onErrorBehaviour)
      }

      fun fromStrings(taxi: List<String>, importSources: List<TaxiSchema> = emptyList(), onErrorBehaviour:TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY): TaxiSchema {
         return from(taxi.map { VersionedSource.sourceOnly(it) }, importSources, onErrorBehaviour)
      }

      fun from(
         taxi: String,
         sourceName: String = "<unknown>",
         version: String = VersionedSource.DEFAULT_VERSION.toString(),
         importSources: List<TaxiSchema> = emptyList()
         , onErrorBehaviour:TaxiSchemaErrorBehaviour = TaxiSchemaErrorBehaviour.RETURN_EMPTY
      ): TaxiSchema {
         return from(VersionedSource(sourceName, version, taxi), importSources, onErrorBehaviour)
      }

      fun compiled(
         taxi: String,
         sourceName: String = "<unknown>",
         version: String = VersionedSource.DEFAULT_VERSION.toString(),
         importSources: List<TaxiSchema> = emptyList()
      ): Pair<List<CompilationError>, TaxiSchema> {
         return compiled(listOf(VersionedSource(sourceName, version, taxi)), importSources)
      }

      fun compileOrFail(
         taxi: String,
         sourceName: String = "<unknown>",
         version: String = VersionedSource.DEFAULT_VERSION.toString(),
         importSources: List<TaxiSchema> = emptyList()
      ):TaxiSchema {
         val (messages,schema) = compiled(taxi, sourceName, version, importSources)
         if (messages.errors().isNotEmpty()) {
            throw CompilationException(messages)
         }
         return schema
      }
   }
}

fun List<lang.taxi.types.FieldModifier>.toVyneFieldModifiers(): List<FieldModifier> {
   return this.map { FieldModifier.valueOf(it.name) }
}

private fun lang.taxi.types.QualifiedName.toVyneQualifiedName(): QualifiedName {
   return QualifiedName(this.toString(), this.parameters.map { it.toVyneQualifiedName() })
}

fun lang.taxi.types.Type.toVyneQualifiedName(): QualifiedName {
   return this.toQualifiedName().toVyneQualifiedName()
}

private fun lang.taxi.sources.SourceCode.toVyneSource(): VersionedSource {
   // TODO : Find the version.
   return VersionedSource(this.sourceName, VersionedSource.DEFAULT_VERSION.toString(), this.content)
}

fun List<lang.taxi.types.CompilationUnit>.toVyneSources(): List<VersionedSource> {
   return this.map { it.source.toVyneSource() }
}


fun List<CompilationError>.toMessage(): String {
   return this.joinToString("\n") { it.toString() }
}
