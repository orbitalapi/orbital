package com.orbitalhq.schemaServer.core.git

import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import mu.KotlinLogging
import reactor.core.publisher.Flux

object GitFileLoaderStatus {
   private val logger = KotlinLogging.logger {}

   /**
    * Merges a stream of git status messages and the status messages from the underlying file loader,
    * and produces a single stream of events, without duplicates
    */
   fun build(gitStatusMessages:Flux<LoaderStatus>, fileStatusMessages: Flux<LoaderStatus>):Flux<LoaderStatus> {
      return Flux.combineLatest(listOf(gitStatusMessages, fileStatusMessages)) { updates: Array<Any> ->
         val gitStatusMessage: LoaderStatus = updates[0] as LoaderStatus
         val fileStatusMessage: LoaderStatus = updates[1] as LoaderStatus

         val mergedStatus = when {
            gitStatusMessage.state == LoaderStatus.LoaderState.ERROR -> gitStatusMessage
            fileStatusMessage.state == LoaderStatus.LoaderState.ERROR -> fileStatusMessage
            gitStatusMessage == LoaderStatus.STARTING || fileStatusMessage == LoaderStatus.STARTING -> LoaderStatus.STARTING
            gitStatusMessage == LoaderStatus.OK && fileStatusMessage == LoaderStatus.OK -> fileStatusMessage
            else -> {
               logger.warn { "Unhandled branch in determining the statue of the git loader: GitLoaderStatus = ${gitStatusMessage}; FileLoaderStatus = ${fileStatusMessage}" }
               LoaderStatus.error("An internal error occurred, could not determine the current state (GitState = $gitStatusMessage; FileState = $fileStatusMessage)")
            }
         }
         mergedStatus
      }
         .distinctUntilChanged()
   }
}
