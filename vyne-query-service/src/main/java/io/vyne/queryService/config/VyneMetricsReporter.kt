package io.vyne.queryService.config

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.stream.Collectors

@Component
class VyneMetricsReporter(private val meterRegistry: MeterRegistry)  {

    //val caskRowCounts: MultiGauge = MultiGauge.builder("cask.row.counts").description("Number of rows per cask").baseUnit("rows").register(meterRegistry)

    //val caskCount: Gauge = Gauge.builder("cask.count") { caskService.getCasks().size }
    //    .description("Number of casks")
    //    .register(meterRegistry)

    //@Scheduled(initialDelay = 60000, fixedRate = 60000)
    //fun updateCaskRowCounts() {
    //    caskRowCounts.register(
    //
    //        caskService.getCasks().stream().map {
    //            MultiGauge.Row.of(
    //                Tags.of("cask", it.qualifiedTypeName), caskDAO.countCaskRecords(it.tableName)
    //            )
    //        }.collect(Collectors.toList())
    //    )
    //}

}