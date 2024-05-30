package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asTaxiSource
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import lang.taxi.CompilationException
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser
import lang.taxi.TaxiParser.MultiNamespaceDocumentContext
import lang.taxi.TaxiParser.SingleNamespaceDocumentContext
import lang.taxi.errors
import lang.taxi.searchUpForRule
import lang.taxi.source
import org.antlr.v4.runtime.ParserRuleContext

enum class EditKind(
   /**
    * Order that edits should be applied.
    * Lower values are applied first
    */
   val precedence: Int
) {
   CreateOrReplace(0),
   CreateOrReplaceQuery(0),
   ChangeFieldType(10),
   ChangeOperationParameterType(11),
   AddOrRemoveFieldAnnotation(12),
   ChangeOperationReturnType(13),
   EditMemberDescription(14),
   ChangeInheritedType(15),
   AddOrRemoveHttpEndpointAnnotation(16),
   AddOrRemoveWebsocketEndpointAnnotation(16),
}

@JsonTypeInfo(
   use = JsonTypeInfo.Id.NAME,
   include = JsonTypeInfo.As.EXISTING_PROPERTY,
   property = "editKind"
)
@JsonSubTypes(
   JsonSubTypes.Type(ChangeFieldType::class, name = "ChangeFieldType"),
   JsonSubTypes.Type(CreateOrReplaceSource::class, name = "CreateOrReplace"),
   JsonSubTypes.Type(CreateOrReplaceQuery::class, name = "CreateOrReplaceQuery"),
   JsonSubTypes.Type(ChangeOperationParameterType::class, name = "ChangeOperationParameterType"),
   JsonSubTypes.Type(AddOrRemoveFieldAnnotation::class, name = "AddOrRemoveFieldAnnotation"),
   JsonSubTypes.Type(ChangeOperationReturnType::class, name = "ChangeOperationReturnType"),
   JsonSubTypes.Type(EditMemberDescription::class, name = "EditMemberDescription"),
   JsonSubTypes.Type(ChangeInheritedType::class, name = "ChangeInheritedType"),
   JsonSubTypes.Type(AddOrRemoveHttpEndpointAnnotation::class, name = "AddOrRemoveHttpEndpointAnnotation"),
   JsonSubTypes.Type(AddOrRemoveWebsocketEndpointAnnotation::class, name = "AddOrRemoveWebsocketEndpointAnnotation"),
)
abstract class SchemaEditOperation {
   abstract fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, SourceEditResult>

   abstract val editKind: EditKind

   /**
    * Indicates if this edit should be applied by first
    * loading the current state from the source repository.
    *
    * true when we're applying an edit to existing source.
    * false when we're creating / appending new source.
    */
   abstract val loadExistingState: Boolean

   abstract fun calculateAffectedTypes():List<Pair<SchemaMemberKind, QualifiedName>>

   protected fun buildCompiler(sourcePackage: SourcePackage, source: TaxiDocument): Compiler {
      val sourceCode = sourcePackage.sources.asTaxiSource()
      return Compiler(sourceCode, importSources = listOf(source))
   }

   /**
    * Adds imports at the top of the file containing the provided token.
    * Respects existing imports, and handles where no imports exist
    */
   protected fun addImports(typeNamesToImport: List<String>, token: ParserRuleContext):SourceEdit {
      // only want to import if it doesn't exist already right?
      val range = getInsertionLocationToAppendImport(token)
      return SourceEdit(
         sourceName = token.source().sourceName,
         range = range,
         newText = typeNamesToImport.joinToString(prefix = "\n", separator = "\n", postfix = "\n\n") { "import $it" }
      )
   }

   private fun getInsertionLocationToAppendImport(token:ParserRuleContext):EditRange {
      val rootToken = token.searchUpForRule(listOf(SingleNamespaceDocumentContext::class.java, TaxiParser.MultiNamespaceDocumentContext::class.java))
      val lastImportLocation = when (rootToken) {
         is SingleNamespaceDocumentContext -> rootToken.importDeclaration().lastOrNull()
         is MultiNamespaceDocumentContext -> rootToken.importDeclaration().lastOrNull()
         else -> error("Expected either a SingleNamespaceDocumentContext or a MultiNamespaceDocumentContext")
      }
      return if (lastImportLocation == null) {
         CharacterPositionRange.PREPEND
      } else {
         lastImportLocation.asCharacterInsertionPoint(EditPosition.AfterPosition)
      }

   }


   protected fun applyEditAndCompile(
      edits: List<SourceEdit>,
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, SourceEditResult> {
      val updatedSources = edits.fold(sourcePackage) { acc, edit -> applyEdit(edit, acc) }
      val (compilerMessages, updatedCompiled) = buildCompiler(updatedSources, taxiDocument)
         .compileWithMessages()

      return if (compilerMessages.errors().isNotEmpty()) {
         CompilationException(compilerMessages.errors()).left()
      } else {
         SourceEditResult(
            updatedSources,
            updatedCompiled,
            edits.map { it.sourceName }.toSet()
         ).right()
      }
   }

   private fun applyEdit(edit: SourceEdit, sourcePackage: SourcePackage): SourcePackage {
      val expectSource = edit.range.splicesExistingText

      val versionedSource = sourcePackage.sources.firstOrNull { it.name == edit.sourceName }
      if (versionedSource == null) {
         if (expectSource) {
            error("Cannot edit source file ${edit.sourceName} as it is not found in this source package")
         }
         return sourcePackage.copy(
            sources = sourcePackage.sourcesWithPackageIdentifier + VersionedSource(
               edit.sourceName,
               sourcePackage.identifier.version,
               edit.newText
            )
         )
      }

      val updatedContent = edit.range.applyTo(edit.newText, versionedSource.content)

      val updatedVersionedSource = versionedSource.copy(content = updatedContent)
      val mutableSources = sourcePackage.sources.toMutableList()

      mutableSources[sourcePackage.sources.indexOf(versionedSource)] = updatedVersionedSource
      return sourcePackage.copy(sources = mutableSources)
   }
}

fun ParserRuleContext.asCharacterPositionRange(): CharacterPositionRange {
   return CharacterPositionRange(this.start.startIndex, this.stop.stopIndex)
}

fun ParserRuleContext.asCharacterInsertionPoint(position: EditPosition): CharacterPositionRange {
   return if (position == EditPosition.AfterPosition) {
      return CharacterPositionRange(this.stop.stopIndex + 1, this.stop.stopIndex + 1)
   } else {
      return CharacterPositionRange(this.start.startIndex + position.delta, this.start.startIndex + position.delta)
   }

}

enum class EditPosition(val delta: Int) {
   AtPosition(0),
   BeforePosition(-1),
   AfterPosition(1)
}

sealed class EditRange(
   val splicesExistingText: Boolean
) {
   abstract fun applyTo(newText: String, other: String): String
}

object Prepend : EditRange(false) {
   override fun applyTo(newText: String, other: String): String = newText + other
}

object Append : EditRange(false) {
   override fun applyTo(newText: String, other: String): String = other + newText
}

object Replace : EditRange(false) {
   override fun applyTo(newText: String, other: String): String = newText

}

data class CharacterPositionRange(val startPosition: Int, val endPosition: Int) : EditRange(true) {
   companion object {
      val PREPEND = CharacterPositionRange(-1, -1)
      val APPEND = CharacterPositionRange(Int.MAX_VALUE, Int.MAX_VALUE)
   }

   override fun applyTo(newText: String, other: String): String {
      val contentBeforeReplacement = if (this == PREPEND) "" else other.substring(0, startPosition)

      val contentAfterReplacement = if (endPosition == other.length) "" else other.substring(endPosition + 1)

      return contentBeforeReplacement + newText + contentAfterReplacement
   }
}

data class SourceEdit(
   val sourceName: String,
   val range: EditRange,
   val newText: String
)

/**
 * Returned after a SourceEdit has been applied
 */
data class SourceEditResult(
   val sourcePackage: SourcePackage,
   val taxiDocument: TaxiDocument,
   val touchedFileNames: Set<String>
)
