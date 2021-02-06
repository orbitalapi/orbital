package io.vyne.cask.streams

import io.vyne.utils.log
import org.apache.commons.io.IOUtils
import org.junit.Test
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory

class DataBufferTest {

   @Test
   fun foo() {
      val source =
         "I'm baby seitan biodiesel food truck fixie pickled pour-over fingerstache adaptogen small batch vexillologist four dollar toast scenester messenger bag quinoa YOLO. Trust fund biodiesel photo booth distillery kale chips, synth fam squid literally paleo deep v. You probably haven't heard of them hammock actually woke +1 stumptown truffaut retro. Mustache vice truffaut, plaid migas pork belly prism knausgaard tbh viral."
      val sourceStream = IOUtils.toInputStream(source)

      val f = DataBufferUtils.readInputStream({ sourceStream }, DefaultDataBufferFactory(), 1024)
      val shared = f
         .share()
         .replay()

      shared.subscribe {
         it.asByteBuffer().
         log().info("From 1: ${IOUtils.toString(it.asInputStream())}")
      }
      shared.subscribe {
         log().info("From 2: ${IOUtils.toString(it.asInputStream())}")
      }
      shared.connect()
   }
}
