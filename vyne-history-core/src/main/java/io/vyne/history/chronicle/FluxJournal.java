package io.vyne.history.chronicle;

import ch.streamly.domain.Timed;
import io.vyne.history.chronicle.replay.ReplayFlux;

/**
 * @author mgabriel.
 */
public interface FluxJournal<T> extends FluxStore<T, Timed<T>> {

   /**
    * @return a Flux that can be used to replay the history with multiple strategies. The history timestamps are the ones assigned by the journal.
    */
   default ReplayFlux<Timed<T>> replayHistory() {
      return replayHistory(Timed::time);
   }
}
