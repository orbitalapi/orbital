package io.vyne.models.csv

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
//import io.vyne.testVyne
import org.junit.Test
/*
class TradeRecordsCsvParserTest  {

   @Test
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

 */
