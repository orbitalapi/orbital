package io.vyne.licensing

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.security.KeyFactory
import java.security.Signature

object Signing {
   val keyFactory = KeyFactory.getInstance("RSA")
   val signer:Signature = Signature.getInstance("SHA1WithRSA")
   val objectMapper = jacksonObjectMapper()
      .findAndRegisterModules()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
}
