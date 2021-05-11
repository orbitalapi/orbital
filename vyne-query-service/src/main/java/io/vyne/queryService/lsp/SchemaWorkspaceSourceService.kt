package io.vyne.queryService.lsp

import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.VersionedSourceProvider
import lang.taxi.lsp.sourceService.WorkspaceSourceService
import lang.taxi.lsp.sourceService.WorkspaceSourceServiceFactory
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.sources.SourceCode
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.services.LanguageClient
import org.springframework.stereotype.Component

/**
 * Adapts the federated schema from the schemaSourceProvider to a workspace source service.
 * If versioned sources are available, then more fine-grained sources are provided to the workspace.
 * Otherwise, just a single string.
 */
class SchemaWorkspaceSourceService(private val schemaProvider: SchemaSourceProvider) : WorkspaceSourceService {
   override fun loadSources(): Sequence<SourceCode> {
      return if (schemaProvider is VersionedSourceProvider) {
         schemaProvider.versionedSources
            .asSequence()
            .map { SourceCode(it.name, it.content) }
      } else {
         sequenceOf(SourceCode("Federated Schema", schemaProvider.schemaString()))
      }
   }

   /**
    * Not supported in federated projects
    */
   override fun loadProject(): TaxiPackageProject? {
      return null
   }
}

@Component
class SchemaWorkspaceSourceServiceFactory(private val schemaProvider: SchemaSourceProvider) :
   WorkspaceSourceServiceFactory {
   override fun build(params: InitializeParams, client: LanguageClient): WorkspaceSourceService {
      return SchemaWorkspaceSourceService(schemaProvider)
   }

}
