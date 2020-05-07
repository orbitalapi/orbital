package io.vyne.pipelines.runner

import com.cacib.cemaforr.common.record.MatrixRecord
import com.cacib.cemaforr.common.record.ReasonType
import org.apache.avro.io.DatumWriter
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.Instant


fun main(args: Array<String>) {

   var json = """{"firstName": "test","lastName": "test","age": "27"}"""

   var matrix = MatrixRecord.newBuilder()
      .setId("ID")
      //.setSentTimeUtc(Instant.now().toEpochMilli()) FIXME
      .setCorrelationId("CORR ID")
      .setProducerComponent("Component")
      .setError(null)
      .setReason(ReasonType.NORMAL)
      .setValid(true)
      .setDataTcType("tcType")
      .setData(json)
      .setDataFnType("xml").build()
   println(matrix)

   var bytes = serialise(matrix)

   File("matrix.avro").writeBytes(bytes)

   var newMatrix = deserialize(File("matrix.avro").readBytes())

   println(newMatrix)
}

fun serialise(data: MatrixRecord): ByteArray {

   try {

      val baos = ByteArrayOutputStream()

      var schema = data.getSchema()
      val outputDatumWriter: DatumWriter<MatrixRecord> = SpecificDatumWriter<MatrixRecord>(schema)
      val encoder = EncoderFactory.get().binaryEncoder(baos, null)
      outputDatumWriter.write(data, encoder)
      encoder.flush()

       return baos.toByteArray()

   } catch (e: IOException) {
      println("Serialization error:" + e.message)
      throw RuntimeException("ERROR")
   }
}

fun deserialize(bytes: ByteArray): MatrixRecord {
   try {
      var schema = MatrixRecord.getClassSchema()
      val bais = ByteArrayInputStream(bytes)
      val d = DecoderFactory.get().binaryDecoder(bais, null)
      val reader = SpecificDatumReader(MatrixRecord::class.java)
      return  reader.read(null, d) as MatrixRecord
   } catch (e: IOException) {
      println("Deserialization error:" + e.message)
      throw RuntimeException("ERROR")
   }
}
