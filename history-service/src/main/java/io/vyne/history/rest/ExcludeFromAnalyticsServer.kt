package io.vyne.history.rest

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass

@ConditionalOnMissingClass("io.vyne.historyServer.AnalyticsServerApp")
annotation class ExcludeFromAnalyticsServer {
}
