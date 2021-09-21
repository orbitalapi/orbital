package io.vyne.schemaServer.editor

import io.vyne.schemaServer.file.FileSystemSchemaRepository

/**
 * A wrapper around another existing FileSystemSchemaRepository, which becomes
 * the repository where changes are written to, if received from the API
 */
class ApiEditorRepository(val fileRepository: FileSystemSchemaRepository) {
}
