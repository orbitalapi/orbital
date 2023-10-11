package com.orbitalhq.queryService

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import com.orbitalhq.query.QueryHolder
import org.junit.Test

class RegressionPackProviderTest {
   @Test
   fun `Query serialisation`() {
      val queryHolder = QueryHolder("""find { foo[] }""")
      val objectMapper = jacksonObjectMapper()
      val jsonStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(queryHolder)

   }
}
