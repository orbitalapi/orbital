package io.vyne.search

import io.vyne.search.embedded.SearchIndexRepository
import io.vyne.search.embedded.SearchResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchController(val searchIndexRepository: SearchIndexRepository) {
   @GetMapping("/search", params = ["query"])
   fun search(@RequestParam("query") query: String): List<SearchResult> {
      return searchIndexRepository.search(query)
   }
}
