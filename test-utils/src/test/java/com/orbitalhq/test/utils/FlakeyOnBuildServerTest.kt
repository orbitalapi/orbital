package com.orbitalhq.test.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

@Disabled("here for local testing to ensure that the flags work")
class FlakeyOnBuildServerTest {

   @Test
   @FlakeyOnBuildServer
   fun shouldPass() {
      fail("Shouldn't be run")
   }
}
