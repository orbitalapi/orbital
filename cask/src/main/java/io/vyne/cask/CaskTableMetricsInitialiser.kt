package io.vyne.cask

import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.binder.BaseUnits
import io.micrometer.core.instrument.config.MeterFilter
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.query.CaskDAO
import io.vyne.utils.log
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.function.Supplier
import java.util.stream.Collectors

@Component
class CaskTableMetricsUpdater(
   private val registry: MeterRegistry,
   private val caskDAO: CaskDAO,
   private val caskConfigRepository: CaskConfigRepository) {

   companion object {
      private var suppliers: Map<String, Supplier<Number>> = emptyMap()
   }

   @EventListener(ApplicationReadyEvent::class)
   fun onApplicationReady() {
      this.caskGauges()
   }

   private fun caskGauges() {
      log().info("registering cask gauges")

      suppliers = caskConfigRepository.findAllByStatus(CaskStatus.ACTIVE)
         .map { it.tableName to Supplier {
            try {
               caskDAO.countCaskRecords(it.tableName) as Number
            } catch (e: Exception) {
               log().info("Error in fetching ${it.tableName} metrics")
               0
            }
         } }
         .toMap()

      registry.forEachMeter {
         if(it.id.name == "cask.size" && !suppliers.containsKey(it.id.tags.first().value)) {
            registry.remove(it.id)
         }
      }

      suppliers.forEach { supplier ->
         log().info("Supplier: ${supplier.key} - ${supplier.value.get()}")

         registry.forEachMeter {
            it.id.name
         }

         val id = Gauge.builder("cask.size", supplier.value)
            .tag("tableCount", supplier.key)
            .description("Number of rows in ${supplier.key}")
            .baseUnit(BaseUnits.ROWS)
            .register(registry).id
         log().info("meter id = $id")
      }
   }
}
