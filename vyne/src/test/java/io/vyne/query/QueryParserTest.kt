package io.vyne.query

import com.winterbe.expekt.expect
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Ignore
import org.junit.Test

class QueryParserTest {

   val schema = TaxiSchema.from("""
namespace vyne.example
type Invoice {
   clientId : ClientId
   items : Item[]
}
type Item {
   itemId : ClientId as String
   price : Price as Decimal
}
type Client {
   clientId : ClientId as String
   name : ClientName as String
   isicCode : IsicCode as String
}
"""
   )

   val queryParser = QueryParser(schema)
   @Test
   fun given_aSingleTypeName_then_aSetOfSingleTypeIsReturned() {
      val result = queryParser.parse("vyne.example.Invoice")
      expect(result).to.have.size(1)
      expect(result.first().type.name.fullyQualifiedName).to.equal("vyne.example.Invoice")
   }

   @Test
   @Ignore("Not implemented.  See https://gitlab.com/osmosis-platform/osmosis/issues/2")
   fun given_aQueryObject_then_itIsParsed() {
      TODO()
   }
}
