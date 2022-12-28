package io.vyne.history.chronicle.replay;

import ch.streamly.domain.WrappedValue;

class ValueToDelay<T> implements WrappedValue<T> {
   private final long delay;
   private final T value;

   ValueToDelay(long delay, T value) {
      this.delay = delay;
      this.value = value;
   }

   @Override
   public T value() {
      return value;
   }

   long delay() {
      return delay;
   }
}
