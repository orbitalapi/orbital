package io.vyne.testcli.commands

import io.vyne.regression.QueryTest
import io.vyne.regression.QueryTester
import io.vyne.utils.log
import picocli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable


@CommandLine.Command(
   name = "queryTest"
)
class QueryTestCommand: Callable<Int> {
   @CommandLine.Option(
      names = ["-p", "--path"],
      defaultValue = "",
      description = ["The directory of Query Specification"]
   )
   lateinit var specPath: Path
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

      val queryTester = QueryTester()
      return try {
         val failures =  queryTester.runTest(specFolder.toFile())
         println(failures)
         if (failures!!.isEmpty()) {
            ExecuteTestCommand.TEST_SUCCESSFUL
         } else {
            ExecuteTestCommand.TEST_FAILED
         }
      } catch (e: Exception) {
         ExecuteTestCommand.TEST_FAILED
      }

   }
}

