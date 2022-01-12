package io.vyne.queryService.schemas.editor

import io.vyne.schemas.Service
import io.vyne.schemas.Type
import lang.taxi.CompilationMessage

data class SchemaSubmissionResult(
    val types: List<Type>,
    val services: List<Service>,
    val messages: List<CompilationMessage>,
    val taxi: String,
    /**
     * Indicates if these changes were actually committed or not
     */
    val dryRun: Boolean
)
