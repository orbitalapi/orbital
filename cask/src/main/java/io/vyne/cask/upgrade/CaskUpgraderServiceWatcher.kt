package io.vyne.cask.upgrade

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.query.CaskDAO
import io.vyne.utils.log
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class CaskUpgraderServiceWatcher(
   private val caskConfigRepository: CaskConfigRepository,
   private val caskDAO: CaskDAO,
   private val upgraderService: CaskUpgraderService
) {

   @Async
   @EventListener
   fun onUpgradeWorkDetected(event: CaskUpgradesRequiredEvent) {
      log().info("Received CaskUpgradesRequiredEvent - looking for work")
      checkForUpgrades()
   }

   @Async
   @EventListener(value = [ContextRefreshedEvent::class])
   fun checkForUpgradesOnStartup() {
      log().info("Cask upgrader service watcher started - checking for work")
      checkForUpgrades()
      dropReplacedCasks()
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
      caskConfigRepository.findAllByStatus(CaskStatus.MIGRATING).forEach { config ->
         log().info("Queuing ${config.tableName} for upgrading")
         queueUpgradeAsync(config)
      }
   }

   @Async()
   fun queueUpgradeAsync(config: CaskConfig) {
      upgraderService.upgrade(config)
   }

}

class CaskUpgradesRequiredEvent

data class CaskUpgradeCompletedEvent(val replacedTableName: String)
