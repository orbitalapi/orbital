package io.vyne.queryService.lsp.querying

import com.nhaarman.mockito_kotlin.mock
import io.vyne.schemas.Schema
import lang.taxi.lsp.LspServicesConfig
import lang.taxi.lsp.TaxiCompilerService
import lang.taxi.lsp.TaxiTextDocumentService
import lang.taxi.lsp.completion.CompositeCompletionService
import lang.taxi.lsp.completion.EditorCompletionService
import lang.taxi.lsp.sourceService.InMemoryWorkspaceSourceService
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.eclipse.lsp4j.services.LanguageClient
import java.nio.file.Path

fun documentServiceForSchema(
   taxi: String,
   includeEditorCompletionService: Boolean = false,
   includeQueryCompletionService: Boolean = true,
   schema: Schema
): TaxiTextDocumentService {
   val service = getDocumentService(includeEditorCompletionService, includeQueryCompletionService, schema)
   val languageClient: LanguageClient = mock { }
   service.connect(languageClient)

   val sourceService = InMemoryWorkspaceSourceService.from(taxi)
   val initializeParams = InitializeParams()
   service.initialize(initializeParams, sourceService)
   return service
}

private fun getDocumentService(
   includeEditorCompletionService: Boolean,
   includeQueryCompletionService: Boolean,
   schema: Schema
): TaxiTextDocumentService {
   val compilerService = TaxiCompilerService()
   val servicesConfig = LspServicesConfig(
      compilerService = compilerService,
      completionService = CompositeCompletionService(
         listOfNotNull(
            if (includeEditorCompletionService) EditorCompletionService(compilerService.typeProvider) else null,
            if (includeQueryCompletionService) QueryCodeCompletionProvider(
               compilerService.typeProvider,
               schema
            ) else null
         )
      )
   )
   val service = TaxiTextDocumentService(servicesConfig)
   return service
}

fun Path.versionedDocument(name: String, version: Int = 1): VersionedTextDocumentIdentifier {
   return VersionedTextDocumentIdentifier(
      this.resolve(name).toUri().toString(),
      version
   )
}

fun Path.document(name: String): TextDocumentIdentifier {
   return TextDocumentIdentifier(this.resolve(name).toUri().toString())
}
