package com.orbitalhq.schemaServer.core.file.packages

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.utils.files.FileSystemChangeEvent
import com.orbitalhq.utils.files.ReactiveFileSystemMonitor
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

class FileSystemPackageLoaderCachingTest {

   @Test
   fun `should serve cached content until file system chagnes`() {
      val fileMonitor: ReactiveFileSystemMonitor = mock { }
      val fileSystemEventSink = Sinks.many().unicast().onBackpressureBuffer<List<FileSystemChangeEvent>>()
      whenever(fileMonitor.startWatching()).thenReturn(fileSystemEventSink.asFlux())

      val adaptor: SchemaSourcesAdaptor = mock { }
      val packageMetadata = PackageMetadata.from("com.foo", "test", "1.0.0")
      whenever(adaptor.buildMetadata(any())).thenReturn(Mono.just(packageMetadata))
      whenever(adaptor.convert(any(), any())).thenReturn(Mono.just(SourcePackage(packageMetadata, emptyList(), emptyMap())))

      val loader = FileSystemPackageLoader(
         FileProjectSpec(Paths.get(".")),
         adaptor, fileMonitor
      )

      val loaded = loader.loadNow().block(Duration.ofMillis(250))
      verify(adaptor, times(1)).convert(any(), any())

      // Trigger second load
      val loaded2 = loader.loadNow().block(Duration.ofMillis(250))!!
      loaded2.shouldBe(loaded)
      // should not have been called again (ie., 1 time total)
      verify(adaptor, times(1)).convert(any(), any())

      // Now, trigger a file system change
      fileSystemEventSink.tryEmitNext(listOf(FileSystemChangeEvent(Path.of(".", "foo"), FileSystemChangeEvent.FileSystemChangeEventType.FileChanged)))

      // and try to reload
      loader.loadNow().block(Duration.ofMillis(250))!!
      // The adaptor should've been called this time
      verify(adaptor, times(2)).convert(any(), any())
   }
}
