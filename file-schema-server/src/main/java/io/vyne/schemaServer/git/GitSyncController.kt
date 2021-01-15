package io.vyne.schemaServer.git

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@ConditionalOnProperty(
   name = ["taxi.git-sync-enabled"],
   havingValue = "true",
   matchIfMissing = false
)
@RestController
class GitSyncController(val syncTask: GitSyncTask) {
   @PostMapping("/git/sync")
   fun gitSync() {
      syncTask.sync()
   }
}
