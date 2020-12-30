package io.vyne.jwt.auth.keycloack

import org.keycloak.platform.PlatformProvider
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class SimplePlatformProvider: PlatformProvider {
   private var shutdownHook: Runnable? = null
   override fun onStartup(startupHook: Runnable?) {
      startupHook?.run()
   }

   override fun onShutdown(shutdownHook: Runnable?) {
      this.shutdownHook = shutdownHook
   }

   override fun exit(p0: Throwable?) {
      thread {
         exitProcess(1)
      }.start()
   }
}
