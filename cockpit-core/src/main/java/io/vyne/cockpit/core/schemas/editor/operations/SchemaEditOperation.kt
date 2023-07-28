package io.vyne.cockpit.core.schemas.editor.operations

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.asTaxiSource
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.SchemaMemberKind
import lang.taxi.CompilationException
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.errors
import org.antlr.v4.runtime.ParserRuleContext

enum class EditKind(
   /**
    * Order that edits should be applied.
    * Lower values are applied first
    */
   val precedence: Int
) {
   CreateOrReplace(0),
   ChangeFieldType(10),
   ChangeOperationParameterType(11)
}

@JsonTypeInfo(
   use = JsonTypeInfo.Id.NAME,
   include = JsonTypeInfo.As.EXISTING_PROPERTY,
   property = "editKind"
)
@JsonSubTypes(
   JsonSubTypes.Type(ChangeFieldType::class, name = "ChangeFieldType"),
   JsonSubTypes.Type(CreateOrReplaceSource::class, name = "CreateOrReplace"),
   JsonSubTypes.Type(ChangeOperationParameterType::class, name = "ChangeOperationParameterType"),
)
abstract class SchemaEditOperation {
   abstract fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, Pair<SourcePackage, TaxiDocument>>

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


   protected fun applyEditAndCompile(
      edits: List<SourcePackageEdit>,
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, Pair<SourcePackage, TaxiDocument>> {
      val updatedSources = edits.fold(sourcePackage) { acc, edit -> applyEdit(edit, acc) }
      val (compilerMessages, updatedCompiled) = buildCompiler(updatedSources, taxiDocument)
         .compileWithMessages()

      return if (compilerMessages.errors().isNotEmpty()) {
         CompilationException(compilerMessages.errors()).left()
      } else {
         (updatedSources to updatedCompiled).right()
      }
   }

   private fun applyEdit(edit: SourcePackageEdit, sourcePackage: SourcePackage): SourcePackage {
      val expectSource = edit.range.splicesExistingText

      val versionedSource = sourcePackage.sources.firstOrNull { it.name == edit.sourceName }
      if (versionedSource == null) {
         if (expectSource) {
            error("Cannot edit source file ${edit.sourceName} as it is not found in this source package")
         }
         return sourcePackage.copy(
            sources = sourcePackage.sources + VersionedSource(
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
      val contentBeforeReplacement = other.substring(0, startPosition)
      val contentAfterReplacement = other.substring(endPosition + 1)

      return contentBeforeReplacement + newText + contentAfterReplacement
   }
}

data class SourcePackageEdit(
   val sourceName: String,
   val range: EditRange,
   val newText: String
)
