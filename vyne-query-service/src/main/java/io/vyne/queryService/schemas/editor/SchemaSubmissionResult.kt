package io.vyne.queryService.schemas.editor

import io.vyne.schemas.*
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
