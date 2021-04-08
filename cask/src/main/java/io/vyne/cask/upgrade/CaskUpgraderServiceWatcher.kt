package io.vyne.cask.upgrade

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.ControlSchemaPollEvent
import io.vyne.schemas.Schema
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.function.Supplier

@Component
class CaskUpgraderServiceWatcher(
   private val caskConfigRepository: CaskConfigRepository,
   private val caskDAO: CaskDAO,
   private val upgraderService: CaskUpgraderService,
   private val eventPublisher: ApplicationEventPublisher,
   @Value("\${spring.datasource.hikari.maximum-pool-size:5}") val dbConnectionPoolSize: Int = 5
) {

   @Async
   @EventListener
   fun onUpgradeWorkDetected(event: CaskUpgradesRequiredEvent) {
      log().info("Received CaskUpgradesRequiredEvent - looking for work")
      checkForUpgrades()
   }

   @EventListener
   fun dropReplacedCasks(event: CaskUpgradeCompletedEvent) {
      dropReplacedCasks()
   }

   private fun dropReplacedCasks() {
      caskConfigRepository.findAllByStatus(CaskStatus.REPLACED).forEach { config ->
         log().info("Dropping replaced cask ${config.tableName}")
         caskDAO.deleteCask(config, true)
      }
   }

   fun calculateUpgradeThreadPoolSize(numberOfCasksToUpgrade: Int): Int {
      return 1.coerceAtLeast((dbConnectionPoolSize / 2)).coerceAtMost(numberOfCasksToUpgrade)
   }

   private fun checkForUpgrades() {
      val casksThatAreUpgraded = caskConfigRepository.findAllByStatus(CaskStatus.MIGRATING)
      if (casksThatAreUpgraded.isEmpty()) {
         log().info("No Cask to upgrade!")
         return
      }
      val threadPoolSize = calculateUpgradeThreadPoolSize(casksThatAreUpgraded.size)
      log().info("Creating an Upgrade Executor Pool with Size $threadPoolSize")
      val migrationExecutorService = Executors.newFixedThreadPool(threadPoolSize, ThreadFactoryBuilder().setNameFormat("CaskUpgrade-%d").build())
      val upgradeTasks = casksThatAreUpgraded.map { config ->
         log().info("Queuing ${config.tableName} for upgrading")
         CompletableFuture.supplyAsync(Supplier<Unit> {
            try {
               upgraderService.upgrade(config)
            } catch (e: Exception) {
               log().error("Error in upgrading Cask $config", e)
            }
         }, migrationExecutorService)
      }
      CompletableFuture.allOf(*upgradeTasks.toTypedArray()).thenApply {
         try {
            migrationExecutorService.shutdown()
         } catch (e: Exception) {
            log().error("Error in shutting down migration thread pool", e)
         }
         eventPublisher.publishEvent(ControlSchemaPollEvent(true))
      }
   }
}

class CaskUpgradesRequiredEvent

data class CaskUpgradeCompletedEvent(val replacedTableName: String)
