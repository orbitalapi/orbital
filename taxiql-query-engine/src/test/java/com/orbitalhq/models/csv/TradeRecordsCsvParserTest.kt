package com.orbitalhq.models.csv

import com.google.common.io.Resources
import com.winterbe.expekt.should
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.testVyne
import org.junit.Ignore
import org.junit.Test

class TradeRecordsCsvParserTest  {

   @Test
   @Ignore("Destructured objects are not currently supported")
   fun canParseCsvTradeRecords() {
      val schema = Resources.getResource("csv/csv-demo.taxi").readText()
      val (vyne,_) = testVyne(schema)
      val csv = Resources.getResource("csv/trade-records.csv").readText()
      val parsedResult = TypedInstance.from(vyne.schema.type("CsvTradeRecordList"), csv, vyne.schema, source = Provided)
      require(parsedResult is TypedCollection) {"Expected TypedCollection"}
      parsedResult.should.have.size(2)
      val firstRecord = parsedResult.first() as TypedObject
      firstRecord["dealtAmount"].should.not.be.`null`
   }
}
