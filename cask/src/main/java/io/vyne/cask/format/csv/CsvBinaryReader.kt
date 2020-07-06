package io.vyne.cask.format.csv

import io.vyne.cask.format.unPad
import io.vyne.utils.log
import reactor.core.publisher.Flux
import java.io.BufferedReader
import java.nio.file.Path
import java.util.*

class CsvBinaryReader {
   fun readAllValuesAtColumn(input: Path, columnIndices: Set<Any>): Flux<Map<CsvColumnIndex, String>> {
      val sortedIndices: MutableList<Int> = mutableListOf()
      val reader = input.toFile().bufferedReader()
      val header = parseHeader(reader)
      val columnReader = ColumnReader(reader, header)

      columnIndices.forEach {
         when (it) {
            is Int -> sortedIndices.add(it)
            is String -> sortedIndices.add(header.columnMap?.get(it) ?: throw NullPointerException("Column map is empty or doesn't contain a column with name: $it"))
            else -> log().error("Indices can be type of either Int or String.")
         }
      }

      return Flux.generate<Map<CsvColumnIndex, String>> { sink ->
         if (columnReader.isCompleted) {
            sink.complete()
         } else {
            val rowData = sortedIndices.sorted().toSortedSet().mapNotNull { columnIndex ->
               columnReader.moveToColumn(columnIndex)
               val result = columnReader.readAsIndexedPair()
               result
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

   fun readAsIndexedPair(): Pair<CsvColumnIndex, String>? {
      if (complete) {
         return null
      }
      val columnValue = CharArray(header.bytesPerColumn)
      val readBytes = reader.read(columnValue, 0, header.bytesPerColumn)
      if (readBytes == -1) {
         complete = true
         return null
      }
      val columnName = header.columnMap?.entries?.firstOrNull { it.value == currentColumnIdx }?.key
      val pair =
         if(columnName == null) {
            CsvColumnIndex(currentColumnIdx)
         } else {
            CsvColumnIndex(currentColumnIdx, columnName.unPad())
         } to String(columnValue).unPad()
      currentColumnIdx++
      return pair
   }

   fun moveToEndOfRow() {
      skipForward((recordsPerRow - currentColumnIdx))
      currentColumnIdx = 0
      currentRowIdx++
   }

   val isCompleted: Boolean
      get() {
         return complete
      }
}

data class CsvColumnIndex(val index: Int, val name: String? = null)
