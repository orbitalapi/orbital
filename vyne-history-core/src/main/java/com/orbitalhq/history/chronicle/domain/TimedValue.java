package com.orbitalhq.history.chronicle.domain;

import java.util.Objects;

/**
 * Default implementation of a {@link Timed} value.
 *
 * @param <T>
 */
public class TimedValue<T> implements Timed<T> {
   private final long time;
   private final T value;

   public TimedValue(long time, T value) {
      this.time = time;
      this.value = value;
   }

   @Override
   public T value() {
      return value;
   }

   @Override
   public long time() {
      return time;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      TimedValue<?> that = (TimedValue<?>) o;
      return time == that.time &&
         Objects.equals(value, that.value);
   }

   @Override
   public int hashCode() {
      return Objects.hash(time, value);
   }

   @Override
   public String toString() {
      return "TimedValue{" +
         "time=" + time +
         ", value=" + value +
         '}';
   }
}
