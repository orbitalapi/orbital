package com.orbitalhq.cockpit.core.schemas.editor

import com.orbitalhq.SourcePackage
import com.orbitalhq.cockpit.core.schemas.editor.operations.SchemaEditOperation
import com.orbitalhq.schemas.PartialSchema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import lang.taxi.CompilationMessage

data class SchemaSubmissionResult(
   override val types: Set<Type>,
   override val services: Set<Service>,
   val messages: List<CompilationMessage>,
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
