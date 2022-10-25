package io.vyne.schemaServer.core.file.packages

import io.vyne.VersionedSource
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.nio.file.Path

class FileSystemPackageWriter {
   private val logger = KotlinLogging.logger {}

   fun writeSource(loader: FileSystemPackageLoader, changed: VersionedSource): Mono<VersionedSource> {
      return writeSources(loader, listOf(changed))
         .map { list -> list.first() }
   }

   fun writeSources(loader: FileSystemPackageLoader, changed: List<VersionedSource>): Mono<List<VersionedSource>> {

      return loader.loadTaxiProject()
         .map { (taxiConfPath, taxiProject) ->
            if (taxiProject == null) {
               // No real good reason for this, but I feel like we should be operating inside a project.
               // Can relax this if needed
               error("A taxi project is required in order to make changes")
            }
            val sourceRoot = taxiConfPath.parent.resolve(taxiProject.sourceRoot)
            writeSource(taxiProject, sourceRoot, changed)
         }
   }

   private fun writeSource(
      project: TaxiPackageProject,
      sourceRoot: Path,
      modifiedSources: List<VersionedSource>
   ): List<VersionedSource> {
      return modifiedSources.map { modifiedSource ->
         val sourcePath = sourceRoot.resolve(modifiedSource.name)
         sourcePath.parent.toFile().mkdirs()
         sourcePath.toFile().writeText(modifiedSource.content)
         logger.info { "Source file $sourcePath updated" }
         // TODO : We should really be incrementing the version, or something here
         modifiedSource.copy(version = project.version)
      }
   }

}
