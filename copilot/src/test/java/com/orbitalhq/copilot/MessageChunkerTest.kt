package com.orbitalhq.copilot

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MessageChunkerTest : DescribeSpec({
   describe("Splitting a message into chunks") {
      it("should break a message into seperate parts") {
         val chunker = MessageChunker()
         val message = """Sure, here is the query to find the duration and review score for the movie 'Gladiator':

```taxi
find { Movie(Title == 'Gladiator') } as {
  duration : DurationInMinutes
  reviewScore : Rating
}
```"""
         val chunked = chunker.chunkMessage(message, OpenAiChatMessage.Role.user)
         chunked.chunks.shouldHaveSize(2)

         chunked.chunks[0].kind.shouldBe(MessageChunk.MessageChunkKind.Chat)
         chunked.chunks[0].shouldBeInstanceOf<ChatMessageChunk>()
            .message.shouldBe("""Sure, here is the query to find the duration and review score for the movie 'Gladiator':""")

         chunked.chunks[1].kind.shouldBe(MessageChunk.MessageChunkKind.Query)
         chunked.chunks[1].shouldBeInstanceOf<QueryMessageChunk>()
            .taxi.shouldBe("""find { Movie(Title == 'Gladiator') } as {
  duration : DurationInMinutes
  reviewScore : Rating
}""")
      }


      it("should break a message with multiple query blocks into seperate parts") {
         val chunker = MessageChunker()
         val message = """Sure, here's a sample query':

```taxi
find { Movie(Title == 'Gladiator') }
```

Here's a more complex example:

```taxi
find { Movie(Title == 'Gladiator') } as {
  duration : DurationInMinutes
}
```

Let me know if you need anyhting else.
"""
         val chunked = chunker.chunkMessage(message, OpenAiChatMessage.Role.user)
         chunked.chunks.shouldHaveSize(5)

         chunked.chunks[0].shouldBe(ChatMessageChunk("Sure, here's a sample query':"))
         chunked.chunks[1].shouldBe(QueryMessageChunk("find { Movie(Title == 'Gladiator') }"))
         chunked.chunks[2].shouldBe(ChatMessageChunk("Here's a more complex example:"))
         chunked.chunks[3].shouldBe(QueryMessageChunk("""find { Movie(Title == 'Gladiator') } as {
  duration : DurationInMinutes
}"""))
         chunked.chunks[4].shouldBe(ChatMessageChunk("Let me know if you need anyhting else."))
      }

   }
})
