package io.vyne.history.chronicle.replay;

import ch.streamly.domain.Timed;
import ch.streamly.domain.TimedValue;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.function.Predicate;

import static java.time.Duration.ofMillis;

/**
 * A transformer that takes a source flux and replays the values with their original timing.
 * It is also possible to specify a time acceleration factor to increase or decrease the replay speed.
 *
 * @param <T> data type
 */
public class ReplayWithOriginalTiming<T> implements Function<Flux<T>, Publisher<T>> {
   private final Function<T, Long> timestampExtractor;
   private final double timeAcceleration;
   private final Timed<T> TOKEN = new TimedValue<>(0, null);

   /**
    * @param timestampExtractor extracts the epoch time in ms from the values.
    */
   public ReplayWithOriginalTiming(Function<T, Long> timestampExtractor) {
      this(timestampExtractor, 1);
   }

   /**
    * @param timestampExtractor extracts the epoch time in ms from the values.
    * @param timeAcceleration   time acceleration factor.
    */
   public ReplayWithOriginalTiming(Function<T, Long> timestampExtractor, double timeAcceleration) {
      this.timestampExtractor = timestampExtractor;
      this.timeAcceleration = timeAcceleration;
   }

   @Override
   public Publisher<T> apply(Flux<T> source) {
      Flux<Timed<T>> timedFlux = source.map(v -> new TimedValue<>(timestampExtractor.apply(v), v));
      return timedFlux.scan(new TimedValuePair<>(TOKEN, TOKEN),
            (acc, val) -> new TimedValuePair<>(acc.second, val))
         .filter(filterFirstValue())
         .map(calculateDelay())
         .delayUntil(applyDelay())
         .map(ValueToDelay::value);
   }

   private Predicate<TimedValuePair<T>> filterFirstValue() {
      return tvp -> tvp.second != TOKEN;
   }

   private Function<TimedValuePair<T>, ValueToDelay<T>> calculateDelay() {
      return tvp -> {
         long timeDifference = Double.valueOf(tvp.timeDifference() / timeAcceleration).longValue();
         if (timeDifference < 0 || tvp.first == TOKEN) {
            timeDifference = 0;
         }
         return new ValueToDelay<>(timeDifference, tvp.second.value());
      };
   }

   private Function<ValueToDelay<T>, Publisher<?>> applyDelay() {
      return vtd -> Flux.just(TOKEN).delayElements(ofMillis(vtd.delay()));
   }

   private static class TimedValuePair<T> {
      private final Timed<T> first;
      private final Timed<T> second;

      private TimedValuePair(Timed<T> first, Timed<T> second) {
         this.first = first;
         this.second = second;
      }

      private long timeDifference() {
         return second.time() - first.time();
      }

      @Override
      public String toString() {
         return "TimedValuePair{" +
            "first=" + first +
            ", second=" + second +
            '}';
      }
   }
}
