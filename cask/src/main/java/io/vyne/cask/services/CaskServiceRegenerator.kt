package io.vyne.cask.services

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.vyne.utils.log
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class CaskServiceRegenerationRunner {
   private val executor = Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("CaskServiceRegenerator-%d").build())
   fun regenerate(regenerateFunction: () -> Unit) {
      executor.submit(object: Runnable {
         override fun run() {
            try {
               regenerateFunction()
            } catch (e: Exception) {
               log().warn("Cask regenaration task failed ${e.message}")
            }
         }
      })
   }
}
