package io.vyne.schemaServer.file

import mu.KotlinLogging

/**
 * FileSystemMonitors are responsible
 * for detecting changes on the file system (in a specific path), and
 * then notifying a provided FileSystemSchemaRepository to reload
 * its sources
 */
interface FileSystemMonitor {
   fun start()
   fun stop()
   val repository:FileSystemSchemaRepository
}

private val logger = KotlinLogging.logger {}

