package io.vyne.cask.format.csv

import com.google.common.base.Stopwatch
import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.utils.log
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.kotlin.test.test
import java.io.ByteArrayInputStream
import java.io.File
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class CsvBinaryWriterTest {

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
                .expectNext(mapOf(0 to "1"))
                .expectNext(mapOf(0 to "2"))
                .expectNext(mapOf(0 to "3"))
                .thenCancel()
                .verify()

        val firstNames = CsvBinaryReader().readAllValuesAtColumn(file.toPath(), setOf(0, 1)).collectList().block()!!
        firstNames.should.equal(listOf(
                mapOf(0 to "1", 1 to "Stephen"),
                mapOf(0 to "2", 1 to "Jason"),
                mapOf(0 to "3", 1 to "Stephen")))
    }

    @Test
    fun canIngestLargeFile() {
        val resource = Resources.getResource("Coinbase_BTCUSD_1h.csv").toURI()

        // Ingest it a few times to get an average performance
        val writer = CsvBinaryWriter(bytesPerColumn = 30)
       Benchmark.benchmark("canIngestLargeFile") {
          val file = folder.newFile()
          writer.convert(File(resource).inputStream(), file.toPath()).collectList().block()
       }
    }
}

object Benchmark {
    fun <T> warmup(name: String, warmup: Int = 10, process: (Stopwatch) -> T): List<T> {
        log().info("Starting warmup for $name")
        val results = (0..warmup).map { count ->
            val stopWatch = Stopwatch.createStarted()
            val result = process(stopWatch)
            log().info("$name warmup $count of $warmup completed in ${stopWatch.elapsed(TimeUnit.MILLISECONDS)}ms")
            result
        }
        log().info("Warmup finished.")
        return results
    }

    fun benchmark(name: String, warmup: Int = 10, iterations: Int = 50, process: (Stopwatch) -> Any) {
       warmup(name, warmup, process)
        val executions = (0..iterations).map { count ->
            val stopWatch = Stopwatch.createStarted()
            val result = process(stopWatch)
            val elapsed = stopWatch.elapsed(TimeUnit.MILLISECONDS)
            log().info("$name run $count of $iterations completed in ${elapsed}ms")
            elapsed to result
        }
        val durations = executions.map { it.first }
        val collectionSize = executions.mapNotNull { if (it.second is Collection<*>) (it.second as Collection<*>).size else null }
        val avgSize = if (collectionSize.isNotEmpty()) " returning an average of ${collectionSize.average().roundToInt()} entries" else ""
        log().info("Completed with average import time of ${durations.average().toBigDecimal().setScale(2,RoundingMode.HALF_EVEN)}ms$avgSize")
    }
}
