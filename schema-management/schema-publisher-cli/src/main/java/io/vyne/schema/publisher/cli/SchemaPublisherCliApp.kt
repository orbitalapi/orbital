package io.vyne.schema.publisher.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.path
import java.net.URI
import java.nio.file.Path

class SchemaPublisherCliApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         PublishSchemaCommand().main(args)
      }
   }
}


class PublishSchemaCommand : CliktCommand() {
   val serverUrl: String by option(help = "The url of the server").prompt("Url of the schema server")
   val taxiPackage:Path by option(help = "The taxi package").path().default(Path.of("./taxi.conf"))
   val spec:Path by option(help = "The path of the OAS Spec").path().prompt("Path to the OpenAPI Spec")
   override fun run() {
      PublishSchemaTask(
         spec,
         URI.create(serverUrl),
         taxiPackage
      ).run()
   }


}
