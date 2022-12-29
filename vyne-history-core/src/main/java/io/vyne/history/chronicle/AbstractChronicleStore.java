package io.vyne.history.chronicle;

import io.vyne.history.chronicle.replay.ReplayFlux;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycle;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueStore;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.File;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Implementation of a {@link FluxStore} backed by a Chronicle Queue.
 * This store respects the backpressure on the data streams it produces.
 *
 * @author mgabriel.
 */
public abstract class AbstractChronicleStore<I, O> implements FluxStore<I, O> {
   private static final Logger LOGGER = LoggerFactory.getLogger(AbstractChronicleStore.class);
   protected final Function<byte[], I> deserializer;
   private final Function<I, byte[]> serializer;
   private final SingleChronicleQueue queue;
   private final RollCycle rollCycle;
   private SingleChronicleQueueStore wireStore;

   protected <S extends AbstractChronicleStore<I, O>, B extends AbstractChronicleStoreBuilder<B, S, I>> AbstractChronicleStore(
      AbstractChronicleStoreBuilder<B, S, I> builder) {
      serializer = builder.serializer;
      deserializer = builder.deserializer;
      rollCycle = builder.rollCycle;
      this.queue = createQueue(builder.path);
   }

   //package private for testing
   SingleChronicleQueue createQueue(String path) {
      return SingleChronicleQueueBuilder.binary(path).rollCycle(rollCycle).build();
   }

   void close() {
      queue.close();
   }

   @Override
   public Disposable store(Publisher<I> toStore) {
      ExcerptAppender appender = queue.acquireAppender();
      return Flux.from(toStore)
         .doOnError(err -> LOGGER.error("Error received", err))
         .subscribe(v -> storeValue(appender, v));
   }

   private void storeValue(ExcerptAppender appender, I v) {
      byte[] bytesToStore = serializeValue(v);
      appender.writeBytes(b -> b.writeInt(bytesToStore.length).write(bytesToStore));
   }

   protected byte[] serializeValue(I v) {
      return serializer.apply(v);
   }

   @Override
   public void store(I item) {
      ExcerptAppender appender = queue.acquireAppender();
      storeValue(appender, item);
   }

   @Override
   public Flux<O> retrieveAll(boolean deleteAfterRead) {
      return Flux.create(sink -> launchTailer(sink, ReaderType.ALL, deleteAfterRead));
   }

   private void launchTailer(FluxSink<O> sink, ReaderType readerType, boolean deleteAfterRead) {
      launchTailer(sink, queue, readerType, deleteAfterRead, false);
   }

   private void launchTailer(FluxSink<O> sink, SingleChronicleQueue queue, ReaderType readerType, boolean deleteAfterRead, boolean isEnd) {
      String path = queue.file().getAbsolutePath();
      Thread t = new Thread(
         () -> readTailer(isEnd ? queue.createTailer().toEnd() : queue.createTailer(), sink, readerType, deleteAfterRead),
         "ChronicleStoreRetrieve_" + path);
      t.setDaemon(true);
      t.start();
   }

   private void readTailer(ExcerptTailer tailer, FluxSink<O> sink,
                           ReaderType readerType, boolean deleteAfterRead) {
      int previousCycle = 0;
      try {
         while (!sink.isCancelled()) {
            if (sink.requestedFromDownstream() > 0) {
               boolean present = tailer.readBytes(b ->
                  sink.next(deserializeValue(b)));
               if (!present) {
                  if (readerType == ReaderType.ONLY_HISTORY) {
                     sink.complete();
                  } else {
                     waitMillis(10); // wait for values to appear on the queue
                  }
               }
            } else {
               waitMillis(100); // wait for requests
            }
            int cycle = rollCycle.toCycle(tailer.index());
            if (cycle != previousCycle) {
               if (deleteAfterRead) {
                  deleteFile(previousCycle);
               }
               previousCycle = cycle;
            }
         }
      } catch (Exception e) {
         LOGGER.error("Error while tailing on queue {}", tailer.queue().file().getAbsolutePath(), e);
      }
   }

   protected abstract O deserializeValue(BytesIn rawData);

   private void waitMillis(long time) {
      try {
         MILLISECONDS.sleep(time);
      } catch (InterruptedException e) {
         //interrupt can happen when the flux is cancelled
         Thread.currentThread().interrupt();
      }
   }

   private void deleteFile(int previousCycle) {
      wireStore = queue.storeForCycle(previousCycle, 0, false, wireStore);
      if (wireStore != null) {
         File file = wireStore.file();
         if (file != null) {
            deleteWireStore(file);
         } else {
            LOGGER.error("Could not find file for cycle {}", previousCycle);
         }
      } else {
         LOGGER.trace("wirestore is null for cycle {}", previousCycle);
      }
   }

   private void deleteWireStore(File file) {
      try {
         wireStore.close();
         wireStore = null;
         boolean deleted = file.delete();
         logDeletionResult(file, deleted);
      } catch (Exception e) {
         LOGGER.error("Could not delete file {}", file.getAbsolutePath(), e);
      }
   }

   private void logDeletionResult(File file, boolean deleted) {
      if (deleted) {
         LOGGER.trace("file {} deleted after read", file.getAbsolutePath());
      } else {
         LOGGER.error("Could not delete file {}", file.getAbsolutePath());
      }
   }

   @Override
   public Flux<O> retrieveHistory() {
      return Flux.create(sink -> launchTailer(sink, ReaderType.ONLY_HISTORY, false));
   }

   @Override
   public Flux<O> retrieveNewValues() {
      return Flux.create(sink -> launchTailer(sink, queue, ReaderType.ALL, false, true));
   }

   @Override
   public ReplayFlux<O> replayHistory(Function<O, Long> timestampExtractor) {
      Flux<O> historySource = Flux.defer(this::retrieveHistory);
      return new ReplayFlux<>(historySource, timestampExtractor);
   }

   private enum ReaderType {
      ALL,
      ONLY_HISTORY
   }

   public abstract static class AbstractChronicleStoreBuilder<B extends AbstractChronicleStoreBuilder<B, R, T>, R extends AbstractChronicleStore, T> {
      private String path;
      private Function<T, byte[]> serializer;
      private Function<byte[], T> deserializer;
      private RollCycle rollCycle = RollCycles.DAILY;

      protected AbstractChronicleStoreBuilder() {
      }

      /**
       * @param path path were the Chronicle Queue will store the files.
       *             This path should not be a network file system (see <a href="https://github.com/OpenHFT/Chronicle-Queue">the Chronicle queue documentation for more detail</a>
       * @return this builder
       */
      public B path(String path) {
         this.path = path;
         return getThis();
      }

      protected abstract B getThis();

      /**
       * @param serializer data serializer
       * @return this builder
       */
      public B serializer(Function<T, byte[]> serializer) {
         this.serializer = serializer;
         return getThis();
      }

      /**
       * @param deserializer data deserializer
       * @return this builder
       */
      public B deserializer(Function<byte[], T> deserializer) {
         this.deserializer = deserializer;
         return getThis();
      }

      /**
       * @param rollCycle roll cycle for the files
       * @return this builder
       */
      public B rollCycle(RollCycle rollCycle) {
         this.rollCycle = rollCycle;
         return getThis();
      }

      public abstract R build();
   }
}
