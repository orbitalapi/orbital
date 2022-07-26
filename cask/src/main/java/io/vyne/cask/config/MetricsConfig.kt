package io.vyne.cask.config

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.vyne.cask.query.generators.OperationAnnotation
import io.vyne.utils.log
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTags
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange
import java.util.regex.Pattern

@Configuration
@EnableScheduling
class MetricsConfig : WebFluxTagsProvider {

    val forwardSlashesPattern: Pattern = Pattern.compile("//+")

    @Bean
    fun timedAspect(registry: MeterRegistry): TimedAspect? {
        capturePercentilesForAllTimers(registry)
        return TimedAspect(registry)
    }

    private fun capturePercentilesForAllTimers(registry: MeterRegistry) {
        log().info("Configuring Metrics Registry to capture percentiles for all timers.")
        registry.config().meterFilter(
            object : MeterFilter {
                override fun configure(id: Meter.Id, config: DistributionStatisticConfig): DistributionStatisticConfig {
                    // https://github.com/micrometer-metrics/micrometer-docs/blob/master/src/docs/concepts/histogram-quantiles.adoc
                    // all timers will be created with percentiles
                    // individual filtering can be done via (id.name.startsWith("reactor.onNext.delay"))
                    return if (id.type == Meter.Type.TIMER) {
                        DistributionStatisticConfig.builder()
                            .percentiles(0.5, 0.9, 0.95, 0.99)
                            .build()
                            .merge(config)
                    } else config
                }
            })
    }

    override fun httpRequestTags(exchange: ServerWebExchange, ex: Throwable?): MutableIterable<Tag> {

        return mutableListOf(
           WebFluxTags.method(exchange),
           WebFluxTags.exception(ex), WebFluxTags.status(exchange),
           WebFluxTags.outcome(exchange, ex), Tag.of("uri", getPathInfo(exchange!!))
        )
    }

    fun getPathInfo(exchange: ServerWebExchange): String {
        val path = exchange.request.path.value()
        val uri = if (StringUtils.hasText(path)) path else "/"
        val singleSlashes = forwardSlashesPattern.matcher(uri).replaceAll("/")

        val normalisedUrl = removeTrailingSlash(singleSlashes)

        return if (normalisedUrl.contains("findSingleBy") ||
            normalisedUrl.contains("findOneBy") ||
            normalisedUrl.contains("findMultipleBy")
        ) {

            val lastSlash = normalisedUrl.lastIndexOf("/")
            normalisedUrl.substring(0, lastSlash) + "/{param}"

        } else if (normalisedUrl.contains(OperationAnnotation.Between.annotation) ) {
            val index = normalisedUrl.indexOf(OperationAnnotation.Between.annotation)
            normalisedUrl.substring(0, index) + OperationAnnotation.Between.annotation + "/{start}/{end}"
        } else if (normalisedUrl.contains(OperationAnnotation.After.annotation) ) {
            val index = normalisedUrl.indexOf(OperationAnnotation.Between.annotation)
            normalisedUrl.substring(0, index) + OperationAnnotation.After.annotation + "/{param}"
        } else if (normalisedUrl.contains(OperationAnnotation.Before.annotation) ) {
            val index = normalisedUrl.indexOf(OperationAnnotation.Before.annotation)
            normalisedUrl.substring(0, index) + OperationAnnotation.Before.annotation + "/{param}"
        } else {
            return normalisedUrl
        }

    }

    fun removeTrailingSlash(text: String): String {
        return if (!StringUtils.hasLength(text)) {
            text
        } else {
            if (text.endsWith("/")) text.substring(0, text.length - 1) else text
        }
    }
}
