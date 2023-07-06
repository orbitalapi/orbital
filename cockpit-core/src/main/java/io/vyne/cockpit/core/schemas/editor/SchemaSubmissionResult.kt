package io.vyne.cockpit.core.schemas.editor

import io.vyne.SourcePackage
import io.vyne.cockpit.core.schemas.editor.operations.SchemaEditOperation
import io.vyne.schemas.PartialSchema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import lang.taxi.CompilationMessage

data class SchemaSubmissionResult(
   override val types: Set<Type>,
   override val services: Set<Service>,
   val messages: List<CompilationMessage>,
   val taxi: String,
   /**
    * Indicates if these changes were actually committed or not
    */
   val dryRun: Boolean,

   /**
    * Contains the source package after the edit was performed.
    * If this was a dryRun, then these changes have not been committed,
    * and may be lost.
    */
   val sourcePackage: SourcePackage,
   /**
    * If an edit operation was performed as a dry run, the
    * edits are returned here.
    *
    * Allows things like imports (where the edits are originated server-side)
    * to be communicated back to the UI
    *
    */
   val pendingEdits: List<SchemaEditOperation>
) : PartialSchema
