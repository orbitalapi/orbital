package io.vyne.pipelines.runner.transport

import com.winterbe.expekt.should
import io.vyne.pipelines.jet.api.transport.CsvRecordContentProvider
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

import org.junit.Test

class CsvRecordContentProviderTest {
   @Test
   fun csvRecordContentProviderAsStringShouldSpitRawValues() {
      val csvRecord = CSVParser.parse("foo,bar", CSVFormat.DEFAULT).records.first()
      CsvRecordContentProvider(csvRecord, emptySet()).asString().should.equal("foo, bar")
   }
}
