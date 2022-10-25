package io.vyne.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mu.KotlinLogging
import java.nio.file.*
import kotlin.io.path.absolutePathString

abstract class ChangeWatchingConfigFileRepository<T : Any>(
   private val configFilePath: Path,
   fallback: Config = ConfigFactory.systemEnvironment()
) : BaseHoconConfigFileRepository<T>(configFilePath, fallback) {

   private var watcherThread: Thread? = null
   private val registeredWatchKeys = mutableListOf<WatchKey>()

   private val logger = KotlinLogging.logger {}

   fun stopWatching() {
      this.watcherThread?.interrupt()
   }

   open fun filePathChanged(changedPath: Path) {
      logger.info { "Detected file change in services config file at $changedPath. Invalidating cache and will reload service on next request" }
      invalidateCache()
   }

   fun watchForChanges() {
      synchronized(this) {

         this.watcherThread = Thread {
            val canonicalParentPath = configFilePath.toFile().canonicalFile.parentFile.toPath()
            logger.info("Starting to watch $canonicalParentPath")
            val watchService = FileSystems.getDefault().newWatchService()
            registeredWatchKeys.add(
               canonicalParentPath.register(
                  watchService,
                  StandardWatchEventKinds.ENTRY_CREATE,
                  StandardWatchEventKinds.ENTRY_DELETE,
                  StandardWatchEventKinds.ENTRY_MODIFY,
                  StandardWatchEventKinds.OVERFLOW
               )
            )

            try {
               while (true) {
                  val key = watchService.take()
                  key.pollEvents()
                     .mapNotNull {
                        it.context() as? Path
                     }
                     .filter { changedPath ->
                        val changedPathString = canonicalParentPath.resolve(changedPath).absolutePathString()
                        changedPathString == configFilePath.absolutePathString()
                     }
                     .distinctBy { changedPath -> changedPath.absolutePathString() }
                     .forEach { changedPath -> filePathChanged(changedPath) }
                  key.reset()
               }
            } catch (e: ClosedWatchServiceException) {
               logger.warn(e) { "Watch service was closed. ${e.message}" }
            } catch (e: Exception) {
               logger.error(e) { "Error in watch service: ${e.message}" }
            }
         }
         watcherThread!!.start()
      }
   }
}
