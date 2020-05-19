package io.vyne.schemaServer

import org.eclipse.jgit.api.CloneCommand
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class GitSynchController(val synch: GitSynch) {
   @GetMapping("/gitCloneRepos")
   fun gitCloneRepos() {
      synch.cloneRepos(CloneCommand())
   }
}
