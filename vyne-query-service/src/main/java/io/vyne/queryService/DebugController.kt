package io.vyne.queryService

import kotlinx.coroutines.debug.DebugProbes
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset

@RestController
class DebugController {
   @GetMapping("/api/debug")
   fun dumpCoroutines():String {
      val baos = ByteArrayOutputStream()
      val stream = PrintStream(baos, true, Charset.defaultCharset())
      DebugProbes.dumpCoroutines(stream)
      return baos.toString(Charset.defaultCharset())
   }
}
