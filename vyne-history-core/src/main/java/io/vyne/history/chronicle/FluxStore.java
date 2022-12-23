package io.vyne.history.chronicle;

import io.vyne.history.chronicle.replay.ReplayFlux;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * Reactive store used to store and replay a Flux.
 *
 * @param <I> input data type
 * @param <O> input data type
 */
public interface FluxStore<I, O> {

   /**
    * Stores all items of the given stream until the stream completes or the returned {@link Disposable} is disposed.
    * Any error received on the stream will stop the storage.
    *
    * @param toStore data stream to store.
    * @return a disposable that can be used to stop the storage process.
    */
   Disposable store(Publisher<I> toStore);

   /**
    * Stores one item.
    *
    * @param item item to store.
    */
   void store(I item);

   /**
    * @return all values present in the store and new values being stored in this FluxStore.
    */
   default Flux<O> retrieveAll() {
      return retrieveAll(false);
   }

   /**
    * @param deleteAfterRead if true, the file storing the data on disk will be deleted once it has been read.
    * @return all values present in the store and new values being stored in this FluxStore.
    */
   Flux<O> retrieveAll(boolean deleteAfterRead);

   /**
    * @return all values present in the store and completes the stream.
    */
   Flux<O> retrieveHistory();

   /**
    * @return the stream of new values being stored in this FluxStore (history is ignored).
    */
   Flux<O> retrieveNewValues();

   /**
    * @param timestampExtractor a function to extract the epoch time from the values.
    * @return a Flux that can be used to replay the history with multiple strategies.
    */
   ReplayFlux<O> replayHistory(Function<O, Long> timestampExtractor);

}
