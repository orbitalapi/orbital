package io.vyne.history.chronicle;

import ch.streamly.domain.Timed;
import ch.streamly.domain.TimedValue;
import net.openhft.chronicle.bytes.BytesIn;

import java.util.function.Function;

/**
 * Implementation of a {@link FluxJournal} backed by a Chronicle Queue.
 * This journal respects the backpressure on the data streams it produces.
 * All values saved in the journal are timed with the current time.
 *
 * @author mgabriel.
 */
public class ChronicleJournal<T> extends AbstractChronicleStore<T, Timed<T>> implements FluxJournal<T> {

   /**
    * @param path         path were the Chronicle Queue will store the files.
    *                     This path should not be a network file system (see <a href="https://github.com/OpenHFT/Chronicle-Queue">the Chronicle queue documentation for more detail</a>
    * @param serializer   data serializer
    * @param deserializer data deserializer
    */
   public ChronicleJournal(String path, Function<T, byte[]> serializer,
                           Function<byte[], T> deserializer) {
      super(ChronicleJournal.<T>newBuilder()
         .path(path)
         .serializer(serializer)
         .deserializer(deserializer));
   }

   private ChronicleJournal(ChronicleJournalBuilder<T> builder) {
      super(builder);
   }

   /**
    * @param <BT> data type.
    * @return a ChronicleStore builder.
    */
   public static <BT> ChronicleJournalBuilder<BT> newBuilder() {
      return new ChronicleJournalBuilder<>();
   }

   private static long fromByteArray(byte[] bytes) {
      return (bytes[0] & 0xFFL) << 56
         | (bytes[1] & 0xFFL) << 48
         | (bytes[2] & 0xFFL) << 40
         | (bytes[3] & 0xFFL) << 32
         | (bytes[4] & 0xFFL) << 24
         | (bytes[5] & 0xFFL) << 16
         | (bytes[6] & 0xFFL) << 8
         | (bytes[7] & 0xFFL);
   }

   private static byte[] toByteArray(long value) {
      return new byte[]{
         (byte) (value >> 56),
         (byte) (value >> 48),
         (byte) (value >> 40),
         (byte) (value >> 32),
         (byte) (value >> 24),
         (byte) (value >> 16),
         (byte) (value >> 8),
         (byte) value
      };
   }

   @Override
   protected byte[] serializeValue(T v) {
      byte[] val = super.serializeValue(v);
      byte[] time = toByteArray(getCurrentTime());
      byte[] result = new byte[val.length + time.length];
      System.arraycopy(time, 0, result, 0, time.length);
      System.arraycopy(val, 0, result, time.length, val.length);
      return result;
   }

   @Override
   protected Timed<T> deserializeValue(BytesIn rawData) {
      int size = rawData.readInt();
      byte[] bytes = new byte[size];
      rawData.read(bytes);
      byte[] time = new byte[8];
      byte[] val = new byte[bytes.length - 8];
      System.arraycopy(bytes, 0, time, 0, time.length);
      System.arraycopy(bytes, 8, val, 0, val.length);
      T value = deserializer.apply(val);
      long receptionTime = fromByteArray(time);
      return new TimedValue<>(receptionTime, value);

   }

   //package private for testing
   long getCurrentTime() {
      return System.currentTimeMillis();
   }

   public static final class ChronicleJournalBuilder<T>
      extends AbstractChronicleStoreBuilder<ChronicleJournalBuilder<T>, ChronicleJournal<T>, T> {
      private ChronicleJournalBuilder() {
         super();
      }

      @Override
      protected ChronicleJournalBuilder<T> getThis() {
         return this;
      }

      @Override
      public ChronicleJournal<T> build() {
         return new ChronicleJournal<>(this);
      }
   }
}
