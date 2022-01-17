package io.vyne.search.embedded

import com.google.common.base.Stopwatch
import io.vyne.schemaApi.SchemaSourceProvider
import lang.taxi.utils.log
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit



@RestController
class SearchController(val searchIndexRepository: SearchIndexRepository, private val schemaProvider: SchemaSourceProvider) {
   @GetMapping("/api/search", params = ["query"])
   fun search(@RequestParam("query") query: String): List<SearchResult> {
      val stopWatch = Stopwatch.createStarted()
      val result: List<SearchResult> = searchIndexRepository.search(query, schemaProvider.schema())
      log().info("Search for term $query took ${stopWatch.elapsed(TimeUnit.MILLISECONDS)}ms and found ${result.size} results")
      return result
   }
}
