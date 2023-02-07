package io.vyne.cockpit.core.schemas.editor

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
   val dryRun: Boolean
) : PartialSchema
