package io.vyne.search.embedded

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LuceneSearchIndexerTest {
   @Rule
   @JvmField
   val tempDir = TemporaryFolder()

   lateinit var indexer: SearchIndexer
   lateinit var repository: SearchIndexRepository
   @Before
   fun setup() {
      repository = VyneEmbeddedSearchConfiguration().searchRepository(tempDir.root.canonicalPath)
      indexer = SearchIndexer(repository)
      log().info("Search index at ${tempDir.root.canonicalPath}")

      val src = """
[[ This is person, a human being ]]
type Person {
   firstName : FirstName as String
}

[[ It probably barks ]]
type Animal {
   breed : Breed as String
}
      """.trimIndent()
      indexer.onSchemaSetChanged(SchemaSetChangedEvent(null, SchemaSet.just(src)))
   }

   @Test
   fun searchResultsMatchOnTypeDoc() {
      val results = repository.search("human")
      results.should.have.size(1)
      results.first().matches.should.have.size(1)
      results.first().matches.first().field.should.equal(SearchField.TYPEDOC)
   }

   @Test
   fun searchResultsMatchOnPartialWordsInTypeDoc() {
      val results = repository.search("hum")
      results.should.have.size(1)
      results.first().matches.should.have.size(1)
      results.first().matches.first().field.should.equal(SearchField.TYPEDOC)
   }

   @Test
   fun searchResultsMatchOnTypeName() {
      val results = repository.search("animal")
      results.should.have.size(1)
      results.first().matches.should.have.size(2)
      results.first().matches.map { it.field }.should.have.elements(SearchField.NAME,SearchField.QUALIFIED_NAME)
   }

   @Test
   fun searchResultsMatchOnFieldName() {
      val results = repository.search("breed")
      results.should.have.size(2)
      // TODO : Think about how the fields are represented in search results.
      // the current approach sucks
//      results.first().matches.should.have.size(2)
//      results.first().matches.map { it.field }.should.have.elements(SearchField.NAME,SearchField.QUALIFIED_NAME)
   }
}
