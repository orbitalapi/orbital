package io.vyne.schemaServerCore.git

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@ConditionalOnProperty(
   name = ["taxi.gitCloningJobEnabled"],
   havingValue = "true",
   matchIfMissing = false
)
@RestController
class GitSyncController(val syncTask: GitSyncTask) {
   @GetMapping("/git/poll")
   fun forceGitPoll() {
      syncTask.sync()
   }
}
