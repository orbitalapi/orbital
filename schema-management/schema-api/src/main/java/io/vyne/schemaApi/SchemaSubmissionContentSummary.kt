package io.vyne.schemaApi

import io.vyne.schemas.QualifiedName


/**
 * Contains a summary of information that was found in the submitted VersionedSourceSubmission.
 * This is intended for the UI, so is a lighter weight class.
 *
 * This class should ultimately be replaced with something that's centric to the taxi project id, rather
 * than the publisher id.
 * Also, the responsibility for creating this sits outside of the SchemaStore - which is wrong.
 * However, currently SchemaStore doesn't track publisher ids, or taxi project ids (preferred).
 * We need to refactor to that, where the SchemaStore isn't just a flattened collection of source files, but
 * is a collection of schema projects.  Until then, we're using this.
 */
data class SourceSubmissionContentSummary(
   val publisherId: String,
   val sourcesCount: Int,
   val typeNames: List<QualifiedName>,
   val serviceNames: List<QualifiedName>
)
