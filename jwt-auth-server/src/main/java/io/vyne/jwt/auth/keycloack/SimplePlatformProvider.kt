package io.vyne.jwt.auth.keycloack

import org.keycloak.Config
import org.keycloak.platform.PlatformProvider
import java.io.File
import kotlin.concurrent.thread
import kotlin.io.path.createTempDirectory
import kotlin.system.exitProcess

open class SimplePlatformProvider : PlatformProvider {
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

   override fun getTmpDirectory(): File {
      return createTempDirectory("vyne-auth-server").toFile()
   }

   override fun getScriptEngineClassLoader(scriptProviderConfig: Config.Scope?): ClassLoader? {
      return null
   }
}
