package io.vyne.cask.websocket

import com.winterbe.expekt.should
import org.junit.Test
import java.net.URI

class RequestUtilsTest {

   @Test
   fun testUriQueryParameters() {
      URI("ws://localhost:8800/cask/TypeName").queryParams().should.be.empty
      URI("ws://localhost:8800/cask/TypeName?debug").queryParams()
         .should.be.equal(mapOf(Pair("debug", listOf(null))))
      URI("ws://localhost:8800/cask/TypeName?debug=").queryParams()
         .should.be.equal(mapOf(Pair("debug", listOf(""))))
      URI("ws://localhost:8800/cask/TypeName?debug=true").queryParams()
         .should.be.equal(mapOf(Pair("debug", listOf("true"))))
      URI("ws://localhost:8800/cask/TypeName?debug=true&contentType").queryParams()
         .should.be.equal(mapOf(Pair("debug", listOf("true")), Pair("contentType", listOf(null))))
      URI("ws://localhost:8800/cask/TypeName?debug=true&contentType=application/json").queryParams()
         .should.be.equal(mapOf(Pair("debug", listOf("true")), Pair("contentType", listOf("application/json"))))
   }
}
