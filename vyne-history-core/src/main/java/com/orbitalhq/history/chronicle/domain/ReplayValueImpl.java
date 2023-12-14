package com.orbitalhq.history.chronicle.domain;

import java.util.Objects;

/**
 * Default implementation of a {@link ReplayValue}
 * @param <T> data type
 */
public class ReplayValueImpl<T> implements ReplayValue<T>{
   private final boolean isLoopRestart;
   private final T value;

   ReplayValueImpl(T value) {
      this.isLoopRestart = false;
      this.value = value;
   }

   ReplayValueImpl(boolean isLoopRestart, T value) {
      this.isLoopRestart = isLoopRestart;
      this.value = value;
   }

   @Override
   public boolean isLoopRestart() {
      return isLoopRestart;
   }

   @Override
   public T value() {
      return value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      ReplayValueImpl<?> that = (ReplayValueImpl<?>) o;
      return isLoopRestart == that.isLoopRestart &&
         Objects.equals(value, that.value);
   }

   @Override
   public int hashCode() {
      return Objects.hash(isLoopRestart, value);
   }

   @Override
   public String toString() {
      return "ReplayValueImpl{" +
         "isLoopRestart=" + isLoopRestart +
         ", value=" + value +
         '}';
   }
}
