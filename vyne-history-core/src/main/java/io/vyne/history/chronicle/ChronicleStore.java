package io.vyne.history.chronicle;

import net.openhft.chronicle.bytes.BytesIn;

import java.util.function.Function;

/**
 * Implementation of a {@link FluxStore} backed by a Chronicle Queue.
 * This store respects the backpressure on the data streams it produces.
 *
 * @author mgabriel.
 */
public class ChronicleStore<T> extends AbstractChronicleStore<T, T> {

   /**
    * @param path         path were the Chronicle Queue will store the files.
    *                     This path should not be a network file system (see <a href="https://github.com/OpenHFT/Chronicle-Queue">the Chronicle queue documentation for more detail</a>
    * @param serializer   data serializer
    * @param deserializer data deserializer
    */
   public ChronicleStore(String path, Function<T, byte[]> serializer,
                         Function<byte[], T> deserializer) {
      super(ChronicleStore.<T>newBuilder()
         .path(path)
         .serializer(serializer)
         .deserializer(deserializer));
   }

   //package private for testing
   ChronicleStore(ChronicleStoreBuilder<T> builder) {
      super(builder);
   }

   /**
    * @param <BT> data type.
    * @return a ChronicleStore builder.
    */
   public static <BT> ChronicleStoreBuilder<BT> newBuilder() {
      return new ChronicleStoreBuilder<>();
   }

   @Override
   protected T deserializeValue(BytesIn rawData) {
      int size = rawData.readInt();
      byte[] bytes = new byte[size];
      rawData.read(bytes);
      return deserializer.apply(bytes);
   }

   public static final class ChronicleStoreBuilder<T>
      extends AbstractChronicleStoreBuilder<ChronicleStoreBuilder<T>, ChronicleStore<T>, T> {

      private ChronicleStoreBuilder() {
         super();
      }

      @Override
      protected ChronicleStoreBuilder<T> getThis() {
         return this;
      }

      @Override
      public ChronicleStore<T> build() {
         return new ChronicleStore<>(this);
      }
   }
}
