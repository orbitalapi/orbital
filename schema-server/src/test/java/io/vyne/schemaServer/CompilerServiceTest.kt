package io.vyne.schemaServer

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockitokotlin2.verify
import io.vyne.VersionedSource
import io.vyne.schema.publisher.SchemaPublisherTransport
import org.junit.Test

class CompilerServiceTest {

   val schemaPublisherMock: SchemaPublisherTransport = mock()
   val compilerService = CompilerService(schemaPublisherMock)

   @Test
   fun `does not recompile if nothing has changed`() {

      // given
      val original = listOf(VersionedSource.sourceOnly("original"))
      compilerService.recompile("id1", original)

      // expect
      verify(schemaPublisherMock, times(1)).submitSchemas(original)

      // when the same schemas are sent
      compilerService.recompile("id1", original)

      // then there is no further call
      verify(schemaPublisherMock, times(1)).submitSchemas(original)

      // when different schemas are sent
      val updated = listOf(VersionedSource.sourceOnly("updated"))
      compilerService.recompile("id1", updated)

      // then there is a further call
      verify(schemaPublisherMock, times(1)).submitSchemas(updated)

      // when schemas with a different identifier are sent
      val differentId = listOf(VersionedSource.sourceOnly("differentId"))
      compilerService.recompile("id2", differentId)

      // then there is a further call
      verify(schemaPublisherMock, times(1)).submitSchemas(listOf(
         VersionedSource.sourceOnly("updated"),
         VersionedSource.sourceOnly("differentId"),
      ))
   }
}
