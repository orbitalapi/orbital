package com.orbitalhq.history.rest

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass

@ConditionalOnMissingClass("com.orbitalhq.historyServer.AnalyticsServerApp")
annotation class ExcludeFromAnalyticsServer {
}
