package io.vyne.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class ByteArrayInOutStream: ByteArrayOutputStream {
   constructor(): super()
   constructor(size: Int): super(size)

   /**
    * Creates a new ByteArrayInputStream that uses the internal byte array buffer
    * of this ByteArrayInOutStream instance as its buffer array. The initial value
    * of pos is set to zero and the initial value of count is the number of bytes
    * that can be read from the byte array. The buffer array is not copied. This
    * instance of ByteArrayInOutStream can not be used anymore after calling this
    * method.
    * @return the ByteArrayInputStream instance
    */
   fun getInputStream(): ByteArrayInputStream {
      // create new ByteArrayInputStream that respects the current count
      val `in` = ByteArrayInputStream(buf, 0, count)

      // set the buffer of the ByteArrayOutputStream
      // to null so it can't be altered anymore
      buf = null
      return `in`
   }
}
