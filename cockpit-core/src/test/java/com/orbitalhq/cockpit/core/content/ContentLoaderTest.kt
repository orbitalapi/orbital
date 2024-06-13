package com.orbitalhq.cockpit.core.content

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

class ContentLoaderTest {
   @Test
   fun `will call next loader if first errors`() {
      val loader = ContentLoader(
         ContentSettings(),
         listOf(
            ErroringContentRepo(),
            DefaultContentRepository(
               listOf(cardData)
            )
         )
      )
      val loaded = loader.loadContent(ContentRequest("0.1.0","landing"))
         .block()
      loaded.shouldNotBeNull()
         .shouldHaveSize(1)
         .single()
         .shouldBe(cardData)
   }

   @Test
   fun `will call next loader if first errors within a mono`() {
      val loader = ContentLoader(
         ContentSettings(),
         listOf(
            MonoErroringContentRepo(),
            DefaultContentRepository(
               listOf(cardData)
            )
         )
      )
      val loaded = loader.loadContent(ContentRequest("0.1.0","landing"))
         .block()
      loaded.shouldNotBeNull()
         .shouldHaveSize(1)
         .single()
         .shouldBe(cardData)
   }

   @Test
   fun `returns empty list if everything fails`() {
      val loader = ContentLoader(
         ContentSettings(),
         listOf(
            MonoErroringContentRepo(),
         )
      )
      val loaded = loader.loadContent(ContentRequest("0.1.0","landing"))
         .block()
      loaded.shouldNotBeNull()
         .shouldBeEmpty()
   }

   @Test
   fun `does not call repos again after result returned`() {
      val erroringRepo = MonoErroringContentRepo()
      val loader = ContentLoader(
         ContentSettings(),
         listOf(
            erroringRepo,
            DefaultContentRepository(
               listOf(cardData)
            )
         )
      )
      val loaded = loader.loadContent(ContentRequest("0.1.0","landing"))
         .block()

      erroringRepo.callCount.shouldBe(1)

      val loadedSecondTime = loader.loadContent(ContentRequest("0.1.0","landing"))
         .block()

      // Should still be 1 - we haven't tried again
      // content was served from cache
      erroringRepo.callCount.shouldBe(1)
      loaded.shouldBe(loadedSecondTime)
   }
}

private class ErroringContentRepo : ContentRepository {
   var callCount = 0
   override fun loadContent(request: ContentRequest): Mono<List<ContentCardData>> {
      callCount++
      error("This is supposed to blow up")
   }
}
private class MonoErroringContentRepo : ContentRepository {
   var callCount = 0
   override fun loadContent(request: ContentRequest): Mono<List<ContentCardData>> {
      callCount++
      return Mono.fromCallable {
         error("This is supposed to blow up")
      }
   }

}

val cardData = ContentCardData(
   title = "test", "Test", "iconUrl", ContentCardAction(
      "text", emptyList()
   )
)
