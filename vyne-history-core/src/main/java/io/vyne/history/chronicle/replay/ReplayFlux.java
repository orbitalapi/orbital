package io.vyne.history.chronicle.replay;

import ch.streamly.domain.ReplayValue;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.util.annotation.NonNull;

import java.time.Duration;
import java.util.function.Function;

/**
 * A flux that can be used to replay historical values with different strategies.
 *
 * @param <T> data type
 */
public class ReplayFlux<T> extends Flux<T> implements Scannable {
   private final Flux<T> source;
   private final Function<T, Long> timestampExtractor;

   /**
    * @param source             the source flux.
    * @param timestampExtractor extracts the epoch time in ms from the values.
    */
   public ReplayFlux(Flux<T> source, Function<T, Long> timestampExtractor) {
      this.source = source;
      this.timestampExtractor = timestampExtractor;
   }

   @Override
   public void subscribe(@NonNull CoreSubscriber<? super T> actual) {
      source.subscribe(actual);
   }

   @Override
   public Object scanUnsafe(@NonNull Attr attr) {
      return getScannable().scanUnsafe(attr);
   }

   private Scannable getScannable() {
      return (Scannable) source;
   }

   /**
    * @return a flux that will replay the values with their original timing
    * (e.g. if the values were received with a 1 second interval, the returned flux will emit at a 1 second interval).
    */
   public ReplayFlux<T> withOriginalTiming() {
      return new ReplayFlux<>(source.transform(new ReplayWithOriginalTiming<>(timestampExtractor)), timestampExtractor);
   }

   /**
    * @param acceleration time acceleration
    * @return a flux that will replay the values with a time acceleration applied to their original timing
    * (e.g. if the values were received with a 2 second interval, and the time acceleration is 2, then the returned flux will emit at a 1 second interval).
    */
   public ReplayFlux<T> withTimeAcceleration(double acceleration) {
      return new ReplayFlux<>(source.transform(new ReplayWithOriginalTiming<>(timestampExtractor, acceleration)), timestampExtractor);
   }

   /**
    * @return a flux that will replay the values in a loop.
    */
   public Flux<ReplayValue<T>> inLoop() {
      return inLoop(Duration.ofMillis(0));
   }

   /**
    * @param delayBeforeLoopRestart duration to wait before each loop.
    * @return a flux that will replay the values in a loop.
    */
   public Flux<ReplayValue<T>> inLoop(Duration delayBeforeLoopRestart) {
      return source.transform(new ReplayInLoop<>(delayBeforeLoopRestart));
   }
}
