package io.vyne.pipelines.jet.transform

import io.vyne.spring.VyneProvider
import org.springframework.stereotype.Component

/**
 * Class that makes the VyneFactory available
 * at startup as a static value, to work around Hazelcast Jet limitations
 */
@Component
class StaticVyneFactory(vyneFactory: VyneProvider) {
   init {
      StaticVyneFactory._vyneFactory = vyneFactory
   }

   companion object {
      private var _vyneFactory: VyneProvider? = null

      val vyneFactory: VyneProvider
         get() {
            return _vyneFactory ?: error("VyneFactory has not been initialized")
         }


   }
}
