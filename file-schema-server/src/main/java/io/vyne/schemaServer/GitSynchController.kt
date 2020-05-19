package io.vyne.schemaServer

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class GitSynchController(val synch: GitSynch) {
   @GetMapping("/gitSynch")
   fun gitSynch() {
      synch.synch()
   }
}
