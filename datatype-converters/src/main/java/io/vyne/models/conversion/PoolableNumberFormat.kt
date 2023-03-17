package io.vyne.models.conversion

import stormpot.Allocator
import stormpot.Poolable
import stormpot.Slot
import java.io.Closeable
import java.text.NumberFormat
import java.util.*

/**
 * NumberFormat is really expdensive to build
 * (see NumberFormat.getInstance() )
 *
 * Also, it turns out it's not thread safe - we've seen errors when
 * running in parallel.
 *
 *
 */
class PoolableNumberFormat(private val slot: Slot) : Poolable, Closeable {
   val numberFormat: NumberFormat = NumberFormat.getInstance(Locale.ENGLISH)
   override fun release() {
      slot.release(this)
   }

   override fun close() {
      this.release()
   }
}

class NumberFormatAllocator : Allocator<PoolableNumberFormat> {
   override fun allocate(slot: Slot): PoolableNumberFormat = PoolableNumberFormat(slot)

   override fun deallocate(poolable: PoolableNumberFormat?) {
   }

}
