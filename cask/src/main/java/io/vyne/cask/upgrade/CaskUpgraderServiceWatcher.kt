package io.vyne.cask.upgrade

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.ControlSchemaPollEvent
import io.vyne.schemas.Schema
import io.vyne.utils.log
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class CaskUpgraderServiceWatcher(
   private val caskConfigRepository: CaskConfigRepository,
   private val caskDAO: CaskDAO,
   private val upgraderService: CaskUpgraderService,
   private val eventPublisher: ApplicationEventPublisher
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
         caskDAO.deleteCask(config.tableName, true)
      }
   }

   private fun checkForUpgrades() {
     val upgradeTasks = caskConfigRepository.findAllByStatus(CaskStatus.MIGRATING).map { config ->
         log().info("Queuing ${config.tableName} for upgrading")
         CompletableFuture.supplyAsync {
            upgraderService.upgrade(config)
         }
      }
      CompletableFuture.allOf(*upgradeTasks.toTypedArray()).thenApply {
         eventPublisher.publishEvent(ControlSchemaPollEvent(true))
      }
   }
}

class CaskUpgradesRequiredEvent

data class CaskUpgradeCompletedEvent(val replacedTableName: String)
