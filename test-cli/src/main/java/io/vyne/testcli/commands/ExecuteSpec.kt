package io.vyne.testcli.commands

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.versionedSources
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.packages.TaxiPackageSources
import lang.taxi.packages.TaxiSourcesLoader
import org.skyscreamer.jsonassert.JSONAssert
import picocli.CommandLine
import picocli.CommandLine.Model.CommandSpec
import java.io.PrintWriter
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable

@CommandLine.Command(
   name = "spec"
)
class ExecuteSpecCommand : Callable<Int> {
   @CommandLine.Option(
      names = ["-f", "--file", "--spec"],
      defaultValue = "input.json",
      description = ["The spec to execute.  Should be in a json or HOCON format, ending in .json or .conf"]
   )
   lateinit var specPath: Path
   private val mapper = Jackson.defaultObjectMapper

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
         val absoluteSpecPath = if(specPath.isAbsolute) {
            specPath
         } else {
            FileSystems.getDefault().getPath("").toAbsolutePath().resolve(specPath)
         }
         return if (Files.isDirectory(absoluteSpecPath)) {
            absoluteSpecPath
         } else {
            absoluteSpecPath.parent
         }
      }


   override fun call(): Int {
      val result = executeTest()
      result.writeTo(commandSpec.commandLine().out)
      return if (result.successful) {
         TEST_SUCCESSFUL
      } else {
         TEST_FAILED
      }
   }

   fun executeTest(): TestResult {
      val spec = buildTestSpec()
      return try {
         val schema = TaxiSchema.from(loadSources().versionedSources())

         val source = readSource(spec)
         val expected = readExpected(spec)
         val sourceType = schema.type(spec.targetType)
         val instance = TypedInstance.from(sourceType, source, schema, source = Provided)
         val actual = mapper.writer().writeValueAsString(instance)
         attempt("Output did not match expected") {
            JSONAssert.assertEquals(expected, actual, true)
         }
         TestResult(spec)
      } catch (exception: TestFailureException) {
         TestResult(spec, exception.failure)
      }

   }

   private fun readExpected(spec: TestSpec): String {
      val input = specFolder.resolve(spec.expected)
      if (!Files.exists(input)) {
         failure("$input does not exist")
      }
      return attempt("Failed to load expected at $input") {
         input.toFile().readText()
      }
   }

   private fun readSource(spec: TestSpec): String {
      val input = specFolder.resolve(spec.source)
      if (!Files.exists(input)) {
         failure("$input does not exist")
      }
      return attempt("Failed to load source at $input") {
         input.toFile().readText()
      }
   }

   internal fun buildTestSpec(): TestSpec {
      return attempt("Failed to load test spec") {
         ConfigFactory.parseFile(specPath.toFile()).extract<TestSpec>()
      }
   }

   private fun <T> attempt(failureDescription: String, callback: () -> T): T {
      return try {
         callback()
      } catch (exception: Exception) {
         throw exception.asFailure(failureDescription)
      }
   }

   internal fun loadSources(): TaxiPackageSources {
      var taxiProjectDirectoryPath = specFolder
      while (!Files.exists(taxiProjectDirectoryPath.resolve("taxi.conf")) && taxiProjectDirectoryPath.parent != null) {
         taxiProjectDirectoryPath = taxiProjectDirectoryPath.parent
      }
      val taxiProjectFilePath = taxiProjectDirectoryPath.resolve("taxi.conf")
      if (!Files.exists(taxiProjectDirectoryPath)) {
         failure("Could not find a taxi.conf project searching from $taxiProjectDirectoryPath")
      }
      return TaxiSourcesLoader.loadPackage(taxiProjectDirectoryPath)

   }

   // probably just for testing
   internal fun buildProject(): TaxiDocument {
      return Compiler(loadSources()).compile()
   }
}

data class TestSpec(
   val name: String,
   val targetType: String,
   val source: String,
   val expected: String
)

fun failure(message: String): Nothing = throw TestFailureException(TestFailure(message))
data class TestFailure(val message: String)
class TestFailureException(val failure: TestFailure) : RuntimeException(failure.message)

fun Throwable.asFailure(prefix: String): TestFailureException = TestFailureException(TestFailure(prefix + this.message))

data class TestResult(
   val spec: TestSpec,
   val failure: TestFailure? = null
) {
   val successful = failure == null

   fun writeTo(out: PrintWriter) {
      val prefix = if (successful) {
         "@|bold,green ✓ [Pass] "
      } else {
         "@|bold,red ✗ [Fail] "
      }
      val message = CommandLine.Help.Ansi.AUTO.string("$prefix ${spec.name} ${failure?.message ?: ""}|@")
      out.println(message)
   }

}
