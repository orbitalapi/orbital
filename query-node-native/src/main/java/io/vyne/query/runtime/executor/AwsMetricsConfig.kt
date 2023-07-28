package io.vyne.query.runtime.executor

import io.vyne.monitoring.aws.BaseAwsMetricsConfig
import org.springframework.context.annotation.Configuration

@Configuration
class AwsMetricsConfig : BaseAwsMetricsConfig(requireAwsRegionEnvVar = true) {
}
