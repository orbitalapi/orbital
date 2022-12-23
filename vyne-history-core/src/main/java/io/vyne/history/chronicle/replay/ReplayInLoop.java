package io.vyne.history.chronicle.replay;

import ch.streamly.domain.ReplayValue;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.LongStream;

/**
 * A transformer that takes a source flux and replays it in a loop.
 * The values are wrapped in a {@link ReplayValue} object to indicate when the loop restarts.
 * This information can be used by the application to perform some actions when the loop restarts (clear caches, etc.)
 * It is possible to specify a delay before each loop restart.
 * Please note that if you add other operators in the reactive stream after this transformer, you might not see the
 * impact of the restart delay since it will be applied as soon as items are requested on the subscription.
 *
 * @param <T> data type
 */
public class ReplayInLoop<T> implements Function<Flux<T>, Publisher<ReplayValue<T>>> {
   private final Duration delayBeforeRestart;

   public ReplayInLoop(Duration delayBeforeRestart) {
      this.delayBeforeRestart = delayBeforeRestart;
   }

   @Override
   public Publisher<ReplayValue<T>> apply(Flux<T> source) {
      Flux<Flux<ReplayValue<T>>> fluxLoop = Flux.create(
         sink -> sink.onRequest(req -> LongStream.range(0, req).forEach(i -> sink.next(wrapValues(source))))
      );
      return Flux.concat(fluxLoop.limitRate(1)
      ); // limit the rate to avoid creating too many source flux in advance.
   }

   private Flux<ReplayValue<T>> wrapValues(Flux<T> source) {
      AtomicBoolean firstValueSent = new AtomicBoolean(false);
      return source
         .delaySubscription(delayBeforeRestart)
         .map(wrapAsReplayValue(firstValueSent));
   }

   private Function<T, ReplayValue<T>> wrapAsReplayValue(AtomicBoolean firstValueSent) {
      return val -> {
         if (!firstValueSent.getAndSet(true)) {
            return ReplayValue.newLoopRestartValue(val);
         }
         return ReplayValue.newValue(val);
      };
   }
}
