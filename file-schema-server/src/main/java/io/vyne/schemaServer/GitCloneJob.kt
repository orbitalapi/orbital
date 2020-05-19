package io.vyne.schemaServer

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class GitCloneJob(
   @Value("\${taxi.gitCloningJobEnabled:false}") val jobEnabled: Boolean,
   val synch: GitSynch) {

   @Scheduled(fixedRateString = "\${taxi.gitCloningJobPeriodMs:300000}")
   fun run() {
      if (jobEnabled && !synch.isInProgress()) {
         synch.synch()
      }
   }
}
