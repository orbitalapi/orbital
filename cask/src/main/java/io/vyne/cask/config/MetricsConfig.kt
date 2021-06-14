package io.vyne.cask.config

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.vyne.utils.log
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTags
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange
import java.util.regex.Pattern

@Configuration
class MetricsConfig : WebFluxTagsProvider {

    val forwardSlashesPattern: Pattern = Pattern.compile("//+")
    val caskDate = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d%3A\\d\\d%3A\\d\\dZ")

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
            WebFluxTags.outcome(exchange), Tag.of("uri", getPathInfo(exchange!!))
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

        } else {
            return normalisedUrl.replace(caskDate, "{param}")
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