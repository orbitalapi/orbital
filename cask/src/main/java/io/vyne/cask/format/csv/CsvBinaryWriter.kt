package io.vyne.cask.format.csv

import io.vyne.cask.format.byteArrayOfLength
import io.vyne.cask.format.unPad
import io.vyne.cask.timed
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import reactor.core.publisher.Flux
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path


class CsvBinaryWriter(private val bytesPerColumn: Int = 15, private val format: CSVFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader()) {

    fun convert(input: InputStream, outputPath: Path): Flux<CSVRecord> {
        if (Files.exists(outputPath) && Files.size(outputPath) > 0) {
            error("$outputPath already exists")
        }

        if (!Files.exists(outputPath)) {
            Files.createFile(outputPath)
        }

        return Flux.create<CSVRecord> { emitter ->
            FileOutputStream(outputPath.toFile()).use { outputStream ->
                var header: Header? = null
                format.parse(input.bufferedReader())
                        .forEach { record ->
                           timed("CsvBinaryWriter.parse") {
                              if (header == null) {
                                 header = writeHeader(outputStream, record)
                              }
                              require(record.size() == header!!.recordsPerRow) { "Record ${record.recordNumber} has invalid number of columns.  Expected ${header!!.recordsPerRow} but got ${record.size()}" }
                              record.forEach { columnValue ->
                                 // TODO : The strategy here is to capture that we've overflowed on a specific column,
                                 // and then add the adjusted offsets to a header that we take into account when reading
                                 require(columnValue.length <= bytesPerColumn) { "Overflow not yet supported.  '$columnValue' had lenght of ${columnValue.length} which exceeds max column size of ${bytesPerColumn}" }

                                 outputStream.write(columnValue.byteArrayOfLength(bytesPerColumn))
                              }
                              emitter.next(record)
                           }
                        }
                emitter.complete()
            }
        }

    }

    private fun writeHeader(outputStream: FileOutputStream, record: CSVRecord): Header {
        val header = Header(record.size(), bytesPerColumn)
        outputStream.write(header.asBytes())
        return header
    }
}

data class Header(val recordsPerRow: Int, val bytesPerColumn: Int) {
    companion object {
        const val HEADER_SIZE_BYTES = 100
        fun parse(chars: CharArray): Header {
            return parse(String(chars))
        }

        private fun parse(input: String): Header {
            val (recordsPerRow, bytesPerColumn) = input.split("|")
            return Header(recordsPerRow.unPad().toInt(), bytesPerColumn.unPad().toInt())
        }

    }

    fun asBytes(): ByteArray {
        val content = "$recordsPerRow|$bytesPerColumn"
        return content.byteArrayOfLength(HEADER_SIZE_BYTES)
    }
}
