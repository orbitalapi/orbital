package io.vyne.cask.websocket

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.function.IntPredicate

class MockDataBuffer (val inputStream: ByteArrayInputStream): DataBuffer {
    override fun asByteBuffer(): ByteBuffer {
        TODO("Not yet implemented")
    }

    override fun asByteBuffer(index: Int, length: Int): ByteBuffer {
        TODO("Not yet implemented")
    }

    override fun indexOf(predicate: IntPredicate, fromIndex: Int): Int {
        TODO("Not yet implemented")
    }

    override fun writePosition(): Int {
        TODO("Not yet implemented")
    }

    override fun writePosition(writePosition: Int): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(predicate: IntPredicate, fromIndex: Int): Int {
        TODO("Not yet implemented")
    }

    override fun toString(index: Int, length: Int, charset: Charset): String {
        TODO("Not yet implemented")
    }

    override fun readableByteCount(): Int {
        return inputStream.available()
    }

    override fun write(b: Byte): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun write(source: ByteArray): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun write(source: ByteArray, offset: Int, length: Int): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun write(vararg buffers: DataBuffer?): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun write(vararg buffers: ByteBuffer?): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun capacity(): Int {
        TODO("Not yet implemented")
    }

    override fun capacity(capacity: Int): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun factory(): DataBufferFactory {
        TODO("Not yet implemented")
    }

    override fun slice(index: Int, length: Int): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun asOutputStream(): OutputStream {
        TODO("Not yet implemented")
    }

    override fun readPosition(): Int {
        TODO("Not yet implemented")
    }

    override fun readPosition(readPosition: Int): DataBuffer {
        TODO("Not yet implemented")
    }

    override fun asInputStream(): InputStream {
        return inputStream
    }

    override fun asInputStream(releaseOnClose: Boolean): InputStream {
        TODO("Not yet implemented")
    }

    override fun writableByteCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getByte(index: Int): Byte {
        TODO("Not yet implemented")
    }

    override fun read(): Byte {
        TODO("Not yet implemented")
    }

    override fun read(destination: ByteArray): DataBuffer {
        inputStream.read(destination)
        return this
    }

    override fun read(destination: ByteArray, offset: Int, length: Int): DataBuffer {
        TODO("Not yet implemented")
    }
}