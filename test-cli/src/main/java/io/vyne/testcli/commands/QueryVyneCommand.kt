package io.vyne.testcli.commands

import picocli.CommandLine
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@CommandLine.Command(
   name = "submitQuery"
)
class QueryVyneCommand: Callable<Int> {
   @CommandLine.Option(
      names = ["-s", "--size"],
      defaultValue = "10",
      description = ["Thread pool size"]
   )
   var threadPoolSize = 10

   @CommandLine.Option(
      names = ["-n", "--numberOfQueries"],
      defaultValue = "10",
      description = ["Number of Queries submitted to the thread pool per second."]
   )
   var numberOfQueriesPerSecond: Int = 1

   @CommandLine.Option(
      names = ["-t", "--totalRunTime"],
      defaultValue = "10",
      description = ["Total Run Time in Seconds"]
   )
   var totalRunTime: Int = 1

   @CommandLine.Option(
      names = ["-u", "--url"],
      defaultValue = "http://localhost:9200",
      description = ["vyne url"]
   )
   lateinit var vyneUrl: String

   @CommandLine.Option(
      names = ["-q", "--query"],
      defaultValue = "",
      description = ["Query to be submitted"]
   )
   lateinit var query: String

   override fun call(): Int {
      val poolExecutor = Executors.newFixedThreadPool(threadPoolSize)
      
      return 0
   }
}
