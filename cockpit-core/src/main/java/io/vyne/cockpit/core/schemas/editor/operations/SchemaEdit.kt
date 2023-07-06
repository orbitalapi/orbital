package io.vyne.cockpit.core.schemas.editor.operations

import io.vyne.PackageIdentifier

data class SchemaEdit(
   /**
    * This makes the request really heavy.
    * At the moment, keeping this as-is, as it allows us
    * to do round-trips on edits where the user is modifying new
    * source that isn't present on the server uet.
    */
   val packageIdentifier: PackageIdentifier,
   val edits: List<SchemaEditOperation>,
   val dryRun: Boolean = true
)
