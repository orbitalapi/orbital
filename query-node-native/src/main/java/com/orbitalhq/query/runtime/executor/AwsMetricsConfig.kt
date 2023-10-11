package com.orbitalhq.query.runtime.executor

import com.orbitalhq.monitoring.aws.BaseAwsMetricsConfig
import org.springframework.context.annotation.Configuration

@Configuration
class AwsMetricsConfig : BaseAwsMetricsConfig(requireAwsRegionEnvVar = true) {
}
