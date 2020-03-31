package io.vyne.schemaServer

import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct

@Component
class CompilerService(@Value("\${taxi.project-home}") val projectHome: String,
                      val schemaStoreClient: SchemaStoreClient) {

   private var counter = 0
   @PostConstruct
   fun recompile() {
      counter++
      log().info("Starting to recompile sources at $projectHome")
      val path: Path = Paths.get(projectHome)
      val schemas = path.toFile().walkBottomUp()
         .filter { it.extension == "taxi" }
         .map {
            VersionedSource(it.nameWithoutExtension, "0.1.$counter", it.readText())
         }
         .toList()
      log().info("Recompiling ${schemas.size} files")
      schemaStoreClient.submitSchemas(schemas)
   }

}
