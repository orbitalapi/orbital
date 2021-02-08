package io.vyne.cask.io

import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList

/**
 * Allows a single InputStream to have multiple consumers.
 * The inputstream buffers until the slower reader has caught up.
 *
 * Construct, and then split() to create multiple consumers
 * Taken from https://stackoverflow.com/a/30262036
 *
 */
object TimeLog {
   var totalTime: Long = 0L;
}

internal class SplittableInputStream private constructor(
   private val multiSource: MultiplexedSource,
   private val myId: Int
) : InputStream() {
   companion object {
      fun from(source: InputStream): SplittableInputStream {
         val multiSource = MultiplexedSource(source)
         val myId = multiSource.addSource(-1)
         return SplittableInputStream(multiSource, myId)
      }
   }

   // Almost an input stream: The read-method takes an id.
   internal class MultiplexedSource(private val source: InputStream) {
      // Read positions of each SplittableInputStream
      private val readPositions: MutableList<Int> = ArrayList()

      // Data to be read by the SplittableInputStreams
      var buffer = IntArray(MIN_BUF)

      // Last valid position in buffer
      private var writePosition = 0

      // Add a multiplexed reader. Return new reader id.
      fun addSource(splitId: Int): Int {
         readPositions.add(if (splitId == -1) 0 else readPositions[splitId])
         return readPositions.size - 1
      }

      // Make room for more data (and drop data that has been read by
      // all readers)
      private fun readjustBuffer() {
         val from: Int = Collections.min(readPositions) //xbatchTimed("from/min") { readPositions.min() ?: 0 }
         val to: Int = Collections.max(readPositions) //xbatchTimed("to/max")  { readPositions.max() ?: 0 }
         // Performance: Copying the buffers is (relatively) expensive
         // So only buffer in chunks of BUFFERED_CHUNK_SIZE
         if (to % BUFFER_CHUNK_SIZE != 0) return
         val newLength = Math.max((to - from) * 2, MIN_BUF)
         val newBuf = IntArray(newLength)
         System.arraycopy(buffer, from, newBuf, 0, to - from)
         for (i in readPositions.indices) readPositions[i] = readPositions[i] - from
         writePosition -= from
         buffer = newBuf
         buffer = buffer

      }

      // Read and advance position for given reader
      fun read(readerId: Int): Int {

         // Enough data in buffer?
         if (readPositions[readerId] >= writePosition) {
            readjustBuffer()
            buffer[writePosition++] = source.read()
         }
         val pos = readPositions[readerId]
         val b = buffer[pos]
         if (b != -1) readPositions[readerId] = pos + 1
         return b
      }

      companion object {
         const val MIN_BUF = 4096
         const val BUFFER_CHUNK_SIZE = 2048
      }


   }


   // Returns a new InputStream that will read bytes from this position
   // onwards.
   fun split(): SplittableInputStream {
      return SplittableInputStream(multiSource, myId)
   }

   override fun read(): Int {
      return multiSource.read(myId)
   }
}
