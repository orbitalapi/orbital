package io.vyne.cask.format.csv

import io.vyne.cask.format.unPad
import reactor.core.publisher.Flux
import java.io.BufferedReader
import java.nio.file.Path
import java.util.*

class CsvBinaryReader {
    fun readAllValuesAtColumn(input: Path, columnIndices: Set<Int>): Flux<Map<Int, String>> {
        val sortedIndices = if (columnIndices is SortedSet<*>) columnIndices as SortedSet<Int> else columnIndices.sorted().toSortedSet()
        val reader = input.toFile().bufferedReader()
        val header = parseHeader(reader)
        val columnReader = ColumnReader(reader, header)
        return Flux.generate<Map<Int,String>> { sink ->
            if (columnReader.isCompleted) {
                sink.complete()
            } else {
                val rowData = sortedIndices.mapNotNull { columnIndex ->
                    columnReader.moveToColumn(columnIndex)
                    columnReader.readAsIndexedPair()
                }.toMap()

                columnReader.moveToEndOfRow()
                sink.next(rowData)
            }
        }.filter { it.isNotEmpty() }
    }

    private fun parseHeader(reader: BufferedReader): Header {
        val headerChars = CharArray(Header.HEADER_SIZE_BYTES)
        reader.read(headerChars, 0, Header.HEADER_SIZE_BYTES)
        return Header.parse(headerChars)
    }
//    return generateSequence
//    {
//        // Skip from the start of the row to the column we care about
//        val skipped = reader.skip((columnIndex * header.bytesPerColumn).toLong())
//        // Read our column
//        val column = CharArray(header.bytesPerColumn)
//        val readBytes = reader.read(column, 0, header.bytesPerColumn)
//        if (readBytes == -1) {
//            return@generateSequence null
//        }
//        val result = String(column)
//        // Now skip to the end of our row, so we're always reading from
//        // the start
//        val columnsToSkip = header.recordsPerRow - (columnIndex + 1)
//        reader.skip((columnsToSkip * header.bytesPerColumn).toLong())
//        0 to result.unPad()
//    }
}

private class ColumnReader(private val reader: BufferedReader, private val header: Header) {
    private var complete = false
    private var currentRowIdx: Int = 0
    private var currentColumnIdx: Int = 0
    private val recordsPerRow = header.recordsPerRow
    fun moveToColumn(column: Int) {
        require(column >= currentColumnIdx) { "Cannot read backwards, and column $column is less than the current columnIndex of $currentColumnIdx.  To read the next row, first call moveToEndOfRow()" }
        require(column < recordsPerRow) { "Column $column is out of bounds, expected max of ${header.recordsPerRow - 1}" }
        val columnsToSkip = column - currentColumnIdx
        skipForward(columnsToSkip)
    }

    private fun skipForward(columnsToSkip: Int) {
        reader.skip((columnsToSkip * header.bytesPerColumn).toLong())
        currentColumnIdx += columnsToSkip
    }

    fun readAsIndexedPair(): Pair<Int, String>? {
        if (complete) {
            return null
        }
        val columnValue = CharArray(header.bytesPerColumn)
        val readBytes = reader.read(columnValue, 0, header.bytesPerColumn)
        if (readBytes == -1) {
            complete = true
            return null
        }
        val pair = currentColumnIdx to String(columnValue).unPad()
        currentColumnIdx++
        return pair
    }

    fun moveToEndOfRow() {
        skipForward((recordsPerRow - currentColumnIdx) )
        currentColumnIdx = 0
        currentRowIdx++
    }

    val isCompleted: Boolean
        get() {
            return complete
        }
}
