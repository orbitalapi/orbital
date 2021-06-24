package io.vyne.cask.format.csv

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.utils.Benchmark
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.kotlin.test.test
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Paths
import java.security.SecureRandom
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.bufferedWriter
import kotlin.random.Random

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

    @Test
    @ExperimentalPathApi
    fun createLargeData() {

        val writer = Paths.get("/Users/anthonycowan/TestLargeSchema2.csv").bufferedWriter(Charsets.UTF_8, 100000 )

        val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT
            .withHeader("attrS1",
                "attrS2",
                "attrS3",
                "attrS4",
                "attrS5",
                "attrS6",
                "attrS7",
                "attrS8",
                "attrS9",
                "attrS10",
                "attrS11",
                "attrS12",
                "attrS13",
                "attrS14",
                "attrS15",
                "attrS16",
                "attrS17",
                "attrS18",
                "attrS19",
                "attrS20",
                "attrS21",
                "attrS22",
                "attrS23",
                "attrS24",
                "attrS25",
                "attrS26",
                "attrS27",
                "attrS28",
                "attrS29",
                "attrS30",
                "attrS31",
                "attrS32",
                "attrS33",
                "attrS34",
                "attrS35",
                "attrS36",
                "attrS37",
                "attrS38",
                "attrS39",
                "attrS40",
                "attrS41",
                "attrS42",
                "attrS43",
                "attrS44",
                "attrS45",
                "attrS46",
                "attrS47",
                "attrS48",
                "attrS49",
                "attrS50",
                "attrS51",
                "attrS52",
                "attrS53",
                "attrS54",
                "attrS55",
                "attrS56",
                "attrS57",
                "attrS58",
                "attrS59",
                "attrS60",
                "attrS61",
                "attrS62",
                "attrS63",
                "attrS64",
                "attrS65",
                "attrS66",
                "attrS67",
                "attrS68",
                "attrS69",
                "attrS70",
                "attrS71",
                "attrS72",
                "attrS73",
                "attrS74",
                "attrS75",
                "attrS76",
                "attrS77",
                "attrS78",
                "attrS79",
                "attrS80",
                "attrS81",
                "attrS82",
                "attrS83",
                "attrS84",
                "attrS85",
                "attrS86",
                "attrS87",
                "attrS88",
                "attrS89",
                "attrS90",
                "attrS91",
                "attrS92",
                "attrS93",
                "attrS94",
                "attrS95",
                "attrS96",
                "attrS97",
                "attrS98",
                "attrS99",
                "attrS100",
                "attrS101",
                "attrS102",
                "attrS103",
                "attrS104",
                "attrS105",
                "attrS106",
                "attrS107",
                "attrS108",
                "attrS109",
                "attrS110",
                "attrS111",
                "attrS112",
                "attrS113",
                "attrS114",
                "attrS115",
                "attrS116",
                "attrS117",
                "attrS118",
                "attrS119",
                "attrS120"))

        for(x in 1..100000) {
            val testLargeSchema = TestLargeSchema()

            val record = Arrays.asList(
                testLargeSchema.attrS1,
                testLargeSchema.attrS2,
                testLargeSchema.attrS3,
                testLargeSchema.attrS4,
                testLargeSchema.attrS5,
                testLargeSchema.attrS6,
                testLargeSchema.attrS7,
                testLargeSchema.attrS8,
                testLargeSchema.attrS9,
                testLargeSchema.attrS10,
                testLargeSchema.attrS11,
                testLargeSchema.attrS12,
                testLargeSchema.attrS13,
                testLargeSchema.attrS14,
                testLargeSchema.attrS15,
                testLargeSchema.attrS16,
                testLargeSchema.attrS17,
                testLargeSchema.attrS18,
                testLargeSchema.attrS19,
                testLargeSchema.attrS20,
                testLargeSchema.attrS21,
                testLargeSchema.attrS22,
                testLargeSchema.attrS23,
                testLargeSchema.attrS24,
                testLargeSchema.attrS25,
                testLargeSchema.attrS26,
                testLargeSchema.attrS27,
                testLargeSchema.attrS28,
                testLargeSchema.attrS29,
                testLargeSchema.attrS30,
                testLargeSchema.attrS31,
                testLargeSchema.attrS32,
                testLargeSchema.attrS33,
                testLargeSchema.attrS34,
                testLargeSchema.attrS35,
                testLargeSchema.attrS36,
                testLargeSchema.attrS37,
                testLargeSchema.attrS38,
                testLargeSchema.attrS39,
                testLargeSchema.attrS40,
                testLargeSchema.attrS41,
                testLargeSchema.attrS42,
                testLargeSchema.attrS43,
                testLargeSchema.attrS44,
                testLargeSchema.attrS45,
                testLargeSchema.attrS46,
                testLargeSchema.attrS47,
                testLargeSchema.attrS48,
                testLargeSchema.attrS49,
                testLargeSchema.attrS50,
                testLargeSchema.attrS51,
                testLargeSchema.attrS52,
                testLargeSchema.attrS53,
                testLargeSchema.attrS54,
                testLargeSchema.attrS55,
                testLargeSchema.attrS56,
                testLargeSchema.attrS57,
                testLargeSchema.attrS58,
                testLargeSchema.attrS59,
                testLargeSchema.attrS60,
                testLargeSchema.attrS61,
                testLargeSchema.attrS62,
                testLargeSchema.attrS63,
                testLargeSchema.attrS64,
                testLargeSchema.attrS65,
                testLargeSchema.attrS66,
                testLargeSchema.attrS67,
                testLargeSchema.attrS68,
                testLargeSchema.attrS69,
                testLargeSchema.attrS70,
                testLargeSchema.attrS71,
                testLargeSchema.attrS72,
                testLargeSchema.attrS73,
                testLargeSchema.attrS74,
                testLargeSchema.attrS75,
                testLargeSchema.attrS76,
                testLargeSchema.attrS77,
                testLargeSchema.attrS78,
                testLargeSchema.attrS79,
                testLargeSchema.attrS80,
                testLargeSchema.attrS81,
                testLargeSchema.attrS82,
                testLargeSchema.attrS83,
                testLargeSchema.attrS84,
                testLargeSchema.attrS85,
                testLargeSchema.attrS86,
                testLargeSchema.attrS87,
                testLargeSchema.attrS88,
                testLargeSchema.attrS89,
                testLargeSchema.attrS90,
                testLargeSchema.attrS91,
                testLargeSchema.attrS92,
                testLargeSchema.attrS93,
                testLargeSchema.attrS94,
                testLargeSchema.attrS95,
                testLargeSchema.attrS96,
                testLargeSchema.attrS97,
                testLargeSchema.attrS98,
                testLargeSchema.attrS99,
                testLargeSchema.attrS100,
                testLargeSchema.attrS101,
                testLargeSchema.attrS102,
                testLargeSchema.attrS103,
                testLargeSchema.attrS104,
                testLargeSchema.attrS105,
                testLargeSchema.attrS106,
                testLargeSchema.attrS107,
                testLargeSchema.attrS108,
                testLargeSchema.attrS109,
                testLargeSchema.attrS110,
                testLargeSchema.attrS111,
                testLargeSchema.attrS112,
                testLargeSchema.attrS113,
                testLargeSchema.attrS114,
                testLargeSchema.attrS115,
                testLargeSchema.attrS116,
                testLargeSchema.attrS117,
                testLargeSchema.attrS118,
                testLargeSchema.attrS119,
                testLargeSchema.attrS120
            )

            csvPrinter.printRecord(record)
        }
        csvPrinter.flush();
        csvPrinter.close();
    }

    data class TestLargeSchema (

                                val attrS1: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS2: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS3: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS4: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS5: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS6: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS7: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS8: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS9: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS10: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS11: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS12: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS13: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS14: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS15: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS16: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS17: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS18: String= RandomStringUtils.randomAlphanumeric(15),
                                val attrS19: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS20: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS21: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS22: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS23: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS24: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS25: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS26: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS27: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS28: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS29: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS30: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS31: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS32: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS33: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS34: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS35: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS36: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS37: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS38: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS39: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS40: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS41: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS42: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS43: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS44: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS45: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS46: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS47: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS48: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS49: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS50: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS51: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS52: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS53: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS54: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS55: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS56: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS57: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS58: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS59: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS60: String = RandomStringUtils.randomAlphanumeric(15),
                                val attrS61: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS62: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS63: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS64: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS65: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS66: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS67: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS68: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS69: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS70: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS71: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS72: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS73: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS74: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS75: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS76: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS77: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS78: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS79: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS80: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS81: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS82: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS83: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS84: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS85: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS86: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS87: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS88: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS89: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS90: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS91: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS92: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS93: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS94: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS95: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS96: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS97: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS98: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS99: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS100: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS101: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS102: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS103: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS104: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS105: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS106: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS107: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS108: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS109: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS110: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS111: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS112: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS113: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS114: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS115: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS116: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS117: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS118: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS119: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE),
                                val attrS120: Double = Random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE)
    )


}

