package io.vyne.schemas.taxi

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.VersionedSource
import io.vyne.schemas.*
import io.vyne.utils.log
import io.vyne.versionedSources
import lang.taxi.Compiler
import lang.taxi.Equality
import lang.taxi.TaxiDocument
import lang.taxi.packages.TaxiSourcesLoader
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.PrimitiveType
import org.antlr.v4.runtime.CharStreams
import java.nio.file.Path

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
      } catch (e:Exception){
         log().error("Exception occurred initializing the Taxi Schema", e)
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
         log().error("Failed to parse TaxiPrimitiveTypes.  This is a fatal error", e)
         emptySet()
      }

      fun forPackageAtPath(path: Path): TaxiSchema {
         return from(TaxiSourcesLoader.loadPackage(path).versionedSources())
      }

      fun from(sources: List<VersionedSource>, imports: List<TaxiSchema> = emptyList()): TaxiSchema {
         val doc =
            Compiler(sources.map { CharStreams.fromString(it.content, it.name) }, imports.map { it.document }).compile()
         // stdLib is always included.
         // Could make this optional in future if needed

         return TaxiSchema(doc, sources)
      }


      fun from(source: VersionedSource, importSources: List<TaxiSchema> = emptyList()): TaxiSchema {

         return from(listOf(source), importSources)
      }

      fun fromStrings(vararg taxi: String, importSources: List<TaxiSchema> = emptyList()): TaxiSchema {
         return fromStrings(taxi.toList(), importSources)
      }

      fun fromStrings(taxi: List<String>, importSources: List<TaxiSchema> = emptyList()): TaxiSchema {
         return from(taxi.map { VersionedSource.sourceOnly(it) }, importSources)
      }

      fun from(
         taxi: String,
         sourceName: String = "<unknown>",
         version: String = VersionedSource.DEFAULT_VERSION.toString(),
         importSources: List<TaxiSchema> = emptyList()
      ): TaxiSchema {
         return from(VersionedSource(sourceName, version, taxi), importSources)
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

