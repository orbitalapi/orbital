package io.vyne.cask

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.BaseUnits
import io.micrometer.core.instrument.binder.MeterBinder
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.query.CaskDAO
import java.util.function.Supplier

class CaskTableMetrics(
   private val caskDAO: CaskDAO,
   private val caskConfigRepository: CaskConfigRepository): MeterBinder {

   override fun bindTo(registry: MeterRegistry) {
      val configs = caskConfigRepository.findAllByStatus(CaskStatus.ACTIVE)
      configs.forEach {
         val count: Supplier<Number> = Supplier { caskDAO.countCaskRecords(it.tableName) }

         Gauge.builder("cask.${it.tableName}.size", count)
            .tag("table", it.tableName)
            .description("Number of rows in ${it.tableName}")
            .baseUnit(BaseUnits.ROWS)
            .register(registry)
      }
   }
}
