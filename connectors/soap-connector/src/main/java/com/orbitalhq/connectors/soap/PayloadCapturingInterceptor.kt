package com.orbitalhq.connectors.soap

import org.apache.cxf.message.Message
import org.apache.cxf.phase.AbstractPhaseInterceptor
import org.apache.cxf.phase.Phase
import org.apache.cxf.transport.http.Address
import org.apache.cxf.transport.http.HTTPConduit
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

data class OutboundSoapRequest(
   val method: String,
   val url: String,
   val payload: String
)

class OutboundPayloadCapturingInterceptor : AbstractPhaseInterceptor<Message>(Phase.PRE_STREAM) {
   companion object {
      private val threadLocalPayload = ThreadLocal<OutboundSoapRequest>()
      fun getCapturedPayload(): OutboundSoapRequest = threadLocalPayload.get()

      fun resetCapturedPayload() = threadLocalPayload.remove()
   }

   override fun handleMessage(message: Message) {
      val address = message[HTTPConduit.KEY_HTTP_CONNECTION_ADDRESS] as Address
      val method = message[Message.HTTP_REQUEST_METHOD] as String
      val outputStream = message.getContent(OutputStream::class.java)
      val newOutputStream = CaptureOutputStream(outputStream, threadLocalPayload, address.url.toExternalForm(), method)
      message.setContent(OutputStream::class.java, newOutputStream)
   }
}

data class InboundSoapResponse(
   val resultCode: Int,
   val payload: String
)

class InboundPayloadCapturingInterceptor : AbstractPhaseInterceptor<Message>(Phase.RECEIVE) {
   companion object {
      private val threadLocalPayload = ThreadLocal<InboundSoapResponse>()
      fun getCapturedPayload(): InboundSoapResponse = threadLocalPayload.get()
      fun resetCapturedPayload() = threadLocalPayload.remove()
   }

   override fun handleMessage(message: Message) {
      val responseCode = message[Message.RESPONSE_CODE] as Int
      val inputStream = message.getContent(InputStream::class.java)
      val decoratedStream = CaptureInputStream(inputStream, threadLocalPayload, responseCode)
      message.setContent(InputStream::class.java, decoratedStream)
   }
}

private class CaptureInputStream(
   private val original: InputStream,
   private val threadLocalPayload: ThreadLocal<InboundSoapResponse>,
   private val responseCode: Int
) : InputStream() {
   private val capture = ByteArrayOutputStream()

   override fun read(): Int {
      val b = original.read()
      if (b != -1) {
         capture.write(b)
      }
      return b
   }

   override fun read(b: ByteArray): Int {
      val len = original.read(b)
      if (len > 0) {
         capture.write(b, 0, len)
      }
      return len
   }

   override fun read(b: ByteArray, off: Int, len: Int): Int {
      val lenRead = original.read(b, off, len)
      if (lenRead > 0) {
         capture.write(b, off, lenRead)
      }
      return lenRead
   }

   override fun skip(n: Long): Long = original.skip(n)

   override fun available(): Int = original.available()

   override fun close() {
      original.close()
      capture.close()
      val capturedMessage = getCapturedMessage()
      threadLocalPayload.set(
         InboundSoapResponse(
            responseCode, capturedMessage
         )
      )
   }

   override fun mark(readlimit: Int) {
      original.mark(readlimit)
   }

   override fun reset() {
      original.reset()
   }

   override fun markSupported(): Boolean = original.markSupported()

   fun getCapturedMessage(): String {
      return capture.toString()
   }
}

private class CaptureOutputStream(
   private val original: OutputStream,
   private val threadLocalPayload: ThreadLocal<OutboundSoapRequest>,
   private val url: String,
   private val method: String
) : OutputStream() {
   private val capture = ByteArrayOutputStream()

   override fun write(b: Int) {
      original.write(b)
      capture.write(b)
   }

   override fun write(b: ByteArray) {
      original.write(b)
      capture.write(b)
   }

   override fun write(b: ByteArray, off: Int, len: Int) {
      original.write(b, off, len)
      capture.write(b, off, len)
   }

   override fun flush() {
      original.flush()
   }

   override fun close() {
      original.close()
      val message = capture.toString()
      threadLocalPayload.set(
         OutboundSoapRequest(
            method,
            url,
            message
         )
      )
   }

}
