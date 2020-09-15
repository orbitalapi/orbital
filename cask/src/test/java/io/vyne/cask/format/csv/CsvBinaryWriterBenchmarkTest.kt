package io.vyne.cask.format.csv

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.utils.Benchmark
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.kotlin.test.test
import java.io.ByteArrayInputStream
import java.io.File

class CsvBinaryWriterBenchmarkTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()


    @Test
    fun canReadAndWriteCsvRecords() {
        val csv = """
id,firstName,lastName,country
1,Stephen,Sondheim,USA
2,Jason,Robert Brown,USA
3,Stephen,Schwartz,USA
        """.trimIndent()
        val writer = CsvBinaryWriter()
        val file = folder.newFile()
        writer.convert(ByteArrayInputStream(csv.toByteArray()), file.toPath()).subscribe()


        val reader = CsvBinaryReader().readAllValuesAtColumn(file.toPath(), setOf(0))

        reader.test()
                .expectSubscription()
                .expectNext(mapOf(CsvColumnIndex(0, "id") to "1"))
                .expectNext(mapOf(CsvColumnIndex(0, "id") to "2"))
                .expectNext(mapOf(CsvColumnIndex(0, "id") to "3"))
                .thenCancel()
                .verify()

        val firstNames = CsvBinaryReader().readAllValuesAtColumn(file.toPath(), setOf(0, 1)).collectList().block()!!
        firstNames.should.equal(listOf(
                mapOf(CsvColumnIndex(0, "id") to "1", CsvColumnIndex(1, "firstName") to "Stephen"),
                mapOf(CsvColumnIndex(0, "id") to "2", CsvColumnIndex(1, "firstName") to "Jason"),
                mapOf(CsvColumnIndex(0, "id") to "3", CsvColumnIndex(1, "firstName") to "Stephen")))
    }

    @Test
    fun canIngestLargeFile() {
        val resource = Resources.getResource("Coinbase_BTCUSD_1h.csv").toURI()

        val writer = CsvBinaryWriter(bytesPerColumn = 30, shouldLogIndividualWriteTime = false)
          val file = folder.newFile()
          writer.convert(File(resource).inputStream(), file.toPath()).collectList().block()
    }
}

