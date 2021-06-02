package io.vyne.schemas.taxi

import com.winterbe.expekt.should
import org.junit.Test

class TaxiSchemaTest {


   @Test
   fun `calling from() with compiler errors returns empty schema`() {
      // This behaviour is important, otherwise the query server can fail to start
      val schema = TaxiSchema.from("i am invalid")
      schema.should.not.be.`null`
   }
}
