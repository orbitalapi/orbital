package io.vyne.testcli.commands

import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import io.vyne.asSourcePackage
import io.vyne.cask.api.ContentType
import io.vyne.models.Provided
import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvImporterUtil
import io.vyne.models.csv.CsvIngestionParameters
import io.vyne.query.TaxiJacksonModule
import io.vyne.query.VyneJacksonModule
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import io.vyne.versionedSources
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.packages.TaxiSourcesLoader
import org.skyscreamer.jsonassert.JSONAssert
import picocli.CommandLine
import picocli.CommandLine.Model.CommandSpec
import java.io.PrintWriter
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable

@CommandLine.Command(
   name = "test"
)
class ExecuteTestCommand : Callable<Int> {
   @CommandLine.Option(
      names = ["-p", "--path"],
      defaultValue = "",
      description = ["The directory of specs"]
   )
   lateinit var specPath: Path
   private val mapper = jacksonObjectMapper()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
      .registerModule(JavaTimeModule())
      .registerModule(VyneJacksonModule())
      .registerModule(TaxiJacksonModule())

   private val schemaCache = CacheBuilder
      .newBuilder()
      .build(object : CacheLoader<Path, Either<CompilationException, TaxiSchema>>() {
         override fun load(path: Path): Either<CompilationException, TaxiSchema> {
            val sources = TaxiSourcesLoader.loadPackage(path)
            return try {
               TaxiSchema.from(sources.asSourcePackage()).right()
            } catch (e: CompilationException) {
               e.left()
            }
         }
      })

   @CommandLine.Spec
   lateinit var commandSpec: CommandSpec


   companion object {
      const val TEST_SUCCESSFUL = 0
      const val TEST_FAILED = 1
   }

   private val specFolder: Path
      get() {
         // spec paths are typically absolute when we're running inside
         // unit tests
         val absoluteSpecPath = if (specPath.isAbsolute) {
            specPath
         } else {
            val currentDir = Paths.get(System.getProperty("user.dir"))
            log().debug("Launched from $currentDir")

            val resolvedPath = currentDir.resolve(specPath)
            log().debug("Searching for tests from $resolvedPath")
            resolvedPath
         }
         return if (Files.isDirectory(absoluteSpecPath)) {
            absoluteSpecPath
         } else {
            absoluteSpecPath.parent
         }
      }


   override fun call(): Int {
      val writer = commandSpec.commandLine().out
      val result = executeTests()
         .map {
            it.writeTo(writer)
            it
         }
         .toList()

      val successful = result.all { it.successful }
      val passCount = result.count { it.successful }
      val failCount = result.count { !it.successful }
      writer.println("${result.size} tests run, $passCount passed, $failCount failed")
      return if (successful) {
         TEST_SUCCESSFUL
      } else {
         TEST_FAILED
      }
   }

   fun executeTests(): Sequence<TestResult> {
      return buildTestSpecs()
         .map { testSpecFile ->
            if (testSpecFile.spec != null) {
               executeTest(testSpecFile, testSpecFile.spec)
            } else {
               TestResult(
                  testSpecFile,
                  testSpecFile.asFailure()
               )
            }
         }
   }

   private fun executeTest(file: TestSpecFile, spec: TestSpec): TestResult {

      return try {
         schemaCache.get(findTaxiProjectDirectoryPath(file.directory)).map { schema ->
            val source = readSource(file, spec)
            val expected = readExpected(file, spec)
            val sourceType = schema.type(spec.targetType)
            val instance = parseContent(sourceType, source, schema, spec)
            val actual = when (instance) {
               is Either.Left -> mapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance.value)
               is Either.Right -> mapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance.value)
            }
            attempt("Output did not match expected") {
               JSONAssert.assertEquals(expected, actual, true)
            }
            TestResult(file)
         }.getOrHandle { compilationException ->
            TestResult(file, TestFailure("Compilation error: ${compilationException.message}"))
         }
      } catch (exception: TestFailureException) {
         TestResult(file, exception.failure)
      }

   }

   private fun parseContent(sourceType: Type, source: String, schema: TaxiSchema, spec: TestSpec): Either<TypeNamedInstance, List<TypeNamedInstance>> {
      return when {
         spec.contentType == ContentType.csv -> parseCsvContent(sourceType, source, schema, spec).right()
         spec.csvOptions != null -> parseCsvContent(sourceType, source, schema, spec).right()
         else -> {
            // TODO : Why isn't .toTypeNamedInstance returning a TypeNamedInstance?  Suspect collections.
            val instance = TypedInstance.from(sourceType, source, schema, source = Provided).toTypeNamedInstance() as TypeNamedInstance
            instance.left()
         }
      }
   }

   private fun parseCsvContent(sourceType: Type, source: String, schema: TaxiSchema, spec: TestSpec): List<TypeNamedInstance> {
      val csvOptions = spec.csvOptions ?: CsvIngestionParameters()
      val results = CsvImporterUtil.parseCsvToType(
         source,
         csvOptions,
         schema,
         sourceType.fullyQualifiedName
      ).map { it.typeNamedInstance as TypeNamedInstance }
      return results
   }

   private fun readExpected(file: TestSpecFile, spec: TestSpec): String {
      val input = file.directory.resolve(spec.expected)
      if (!Files.exists(input)) {
         failure("$input does not exist")
      }
      return attempt("Failed to load expected at $input") {
         input.toFile().readText()
      }
   }

   private fun readSource(specFile: TestSpecFile, spec: TestSpec): String {
      val input = specFile.directory.resolve(spec.source)
      if (!Files.exists(input)) {
         failure("$input does not exist")
      }
      return attempt("Failed to load source at $input") {
         input.toFile().readText()
      }
   }

   internal fun buildTestSpecs(): Sequence<TestSpecFile> {
      return specFolder.toFile().walk()
         .filter { it.name.endsWith("spec.json") }
         .map { file ->
            try {
               val testSpec = mapper.readValue<TestSpec>(file)
               TestSpecFile(file.toPath(), testSpec, null)
            } catch (exception: Exception) {
               TestSpecFile(file.toPath(), null, message = "Failed to load spec at $file :" + exception.message)
            }
         }
   }

   private fun <T> attempt(failureDescription: String, callback: () -> T): T {
      return try {
         callback()
      } catch (exception: Throwable) {
         throw exception.asFailure(failureDescription)
      }
   }

   internal fun findTaxiProjectDirectoryPath(directory: Path): Path {
      var taxiProjectDirectoryPath = directory
      while (!Files.exists(taxiProjectDirectoryPath.resolve("taxi.conf")) && taxiProjectDirectoryPath.parent != null) {
         taxiProjectDirectoryPath = taxiProjectDirectoryPath.parent
      }
      if (!Files.exists(taxiProjectDirectoryPath)) {
         failure("Could not find a taxi.conf project searching from $taxiProjectDirectoryPath")
      }
      return taxiProjectDirectoryPath
   }

   internal fun findTaxiProjectDirectoryPath(file: TestSpecFile): Path {
      var taxiProjectDirectoryPath = file.directory
      return findTaxiProjectDirectoryPath(taxiProjectDirectoryPath)
   }

   // probably just for testing
   internal fun buildProject(): TaxiDocument {
      return schemaCache.get(findTaxiProjectDirectoryPath(specPath)).map {
         it.document
      }.getOrHandle { e -> throw e }
   }
}

data class TestSpecFile(
   val path: Path,
   val spec: TestSpec?,
   val message: String?
) {
   val directory = path.parent
   fun asFailure(): TestFailure {
      return TestFailure(this.message!!)
   }

   val description: String
      get() {
         return if (this.spec != null) {
            this.spec.name
         } else {
            this.path.toString()
         }
      }
}

data class TestSpec(
   val name: String,
   val targetType: String,
   val source: String,
   val expected: String,
   val contentType: ContentType? = null,
   val csvOptions: CsvIngestionParameters? = null
)

fun failure(message: String): Nothing = throw TestFailureException(TestFailure(message))
data class TestFailure(val message: String)
class TestFailureException(val failure: TestFailure) : RuntimeException(failure.message)

fun Throwable.asFailure(prefix: String): TestFailureException {
   val message = when (this) {
      is InvocationTargetException -> this.targetException.message
      else -> this.message
   }
   return TestFailureException(TestFailure(prefix + this.message))
}

data class TestResult(
   val spec: TestSpecFile,
   val failure: TestFailure? = null
) {
   val successful = failure == null

   fun writeTo(out: PrintWriter) {
      val prefix = if (successful) {
         "@|bold,green ✓ [Pass] "
      } else {
         "@|bold,red ✗ [Fail] "
      }

      val message = CommandLine.Help.Ansi.AUTO.string("$prefix ${spec.description} ${failure?.message ?: ""}|@")
      out.println(message)
   }

}
