package io.vyne.cask.ingest

interface StreamSource {
   fun sequence(): Sequence<InstanceAttributeSet>
   val messageId: String
}
