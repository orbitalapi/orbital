package io.vyne.monitoring

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


@ComponentScan(basePackageClasses = [EnableCloudMetrics::class])
@Configuration
annotation class EnableCloudMetrics {
}
