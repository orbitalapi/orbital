package com.orbitalhq.cockpit.core.lsp

import com.orbitalhq.schema.api.SchemaSourceProvider
import com.orbitalhq.utils.orElse
import lang.taxi.lsp.sourceService.WorkspaceSourceService
import lang.taxi.lsp.sourceService.WorkspaceSourceServiceFactory
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.sources.SourceCode
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.services.LanguageClient
import org.springframework.stereotype.Component
import java.nio.file.Paths

/**
 * Adapts the federated schema from the schemaSourceProvider to a workspace source service.
 * If versioned sources are available, then more fine-grained sources are provided to the workspace.
 * Otherwise, just a single string.
 */
class SchemaWorkspaceSourceService(private val schemaProvider: SchemaSourceProvider) : WorkspaceSourceService {
   override fun loadSources(): List<Pair<TaxiPackageProject?, Sequence<SourceCode>>> {
      // MP 11-Feb-25:
      // The API has evolved here allowing us to emit project data as well as the source code files.
      // We don't have that easily available inside the Orbital code context, so returning null for
      // the project. We could refactor this at a later date if required.
      // Variable here is just for readability
      val nullTaxiPackageProject:TaxiPackageProject? = null

      return schemaProvider.packages
         .map {
            nullTaxiPackageProject to it.sources.asSequence()
               .map { source -> SourceCode(source.name, source.content, path = source.pathOrName) }
         }
   }

   /**
    * Not supported in federated projects
    */
   override fun loadProjects(): List<TaxiPackageProject> {
      return emptyList()
   }
}

@Component
class SchemaWorkspaceSourceServiceFactory(private val schemaProvider: SchemaSourceProvider) :
   WorkspaceSourceServiceFactory {
   override fun build(params: InitializeParams, client: LanguageClient): WorkspaceSourceService {
      return SchemaWorkspaceSourceService(schemaProvider)
   }

}
