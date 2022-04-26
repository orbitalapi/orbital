package io.vyne.schema.rsocket

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.CompositeByteBuf
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownMimeType


object RSocketRoutes {
   const val SCHEMA_SUBMISSION = "request.schemaSubmission"
   const val SCHEMA_UPDATES = "schema.updates"

   fun schemaSubmissionRouteMetadata(): ByteBuf {
      return routeMetadata(SCHEMA_SUBMISSION)
   }

   fun schemaUpdatesRouteMetadata(): ByteBuf {
      return routeMetadata(SCHEMA_UPDATES)
   }

   private fun routeMetadata(route: String): CompositeByteBuf {
      // See:
      // https://stackoverflow.com/a/62776146
      val metadataByteBuffer: CompositeByteBuf = ByteBufAllocator.DEFAULT.compositeBuffer()
      val routingMetadata = TaggingMetadataCodec.createRoutingMetadata(ByteBufAllocator.DEFAULT, listOf(route))

      CompositeMetadataCodec.encodeAndAddMetadata(
         metadataByteBuffer,
         ByteBufAllocator.DEFAULT,
         WellKnownMimeType.MESSAGE_RSOCKET_ROUTING,
         routingMetadata.content
      )
      return metadataByteBuffer
   }
}
