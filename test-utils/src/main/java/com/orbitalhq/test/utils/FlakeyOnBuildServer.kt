package com.orbitalhq.test.utils

import org.junit.jupiter.api.condition.DisabledIf


@DisabledIf("com.orbitalhq.test.utils.BuildServer#isOnBuildServer", disabledReason = "Test is flakey on the build server, but should be run locally")
annotation class FlakeyOnBuildServer

object BuildServer {
   @JvmStatic
   fun isOnBuildServer(): Boolean {
      return System.getProperty("env.buildServer") == "true"
   }
}
