package io.vyne.search.embedded

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.util.concurrent.MoreExecutors
import com.winterbe.expekt.should
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths

@Ignore
class LuceneSearchIndexerTest {
   @Rule
   @JvmField
   val tempDir = TemporaryFolder()

   lateinit var indexer: SearchIndexer
   lateinit var repository: SearchIndexRepository
   @Before
   fun setup() {
      val directory = FSDirectory.open(Paths.get(tempDir.root.canonicalPath))
      val indexWriterConfig = IndexWriterConfig(DefaultConfigFactory().config().analyzer)
      val indexWriter = IndexWriter(directory, indexWriterConfig)
      repository = SearchIndexRepository(indexWriter, VyneEmbeddedSearchConfiguration().searcherManager(indexWriter), DefaultConfigFactory())
      indexer = SearchIndexer(repository, MoreExecutors.newDirectExecutorService())
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

type TradeRecord {}
      """.trimIndent()
      indexer.onSchemaSetChanged(SchemaSetChangedEvent(null, SchemaSet.just(src)))
   }

   @Test
   @Ignore
   fun searchResultsMatchOnTypeDoc() {
      val results = repository.search("human")
      results.should.have.size(1)
      results.first().matches.should.have.size(1)
      results.first().matches.first().field.should.equal(SearchField.TYPEDOC)
   }

   @Test
   @Ignore
   fun searchResultsMatchOnName() {
      val results = repository.search("Pers")
      results.should.have.size(1)
      results.first().matches.should.have.size(5)
   }

   @Test
   @Ignore
   fun whenNameIsLongishThenSearchingMatchesWithAFewLetters() {
      // Observed that matches weren't being made on TradeRecord when searching for Tra
      val results = repository.search("tra")
      results.find { it.qualifiedName.fullyQualifiedName == "TradeRecord" }.should.not.be. `null`
   }

   @Test
   @Ignore
   fun searchResultsMatchOnPartialWordsInTypeDoc() {
      val results = repository.search("hum")
      results.should.have.size(1)
      results.first().matches.should.have.size(1)
      results.first().matches.first().field.should.equal(SearchField.TYPEDOC)
   }

   @Test
   @Ignore
   fun searchResultsMatchOnTypeName() {
      val results = repository.search("animal")
      results.should.have.size(1)
      results.first().matches.should.have.size(4)
   }

   @Test
   @Ignore
   fun searchResultsMatchOnFieldName() {
      val results = repository.search("breed")
      results.should.have.size(2)
      // TODO : Think about how the fields are represented in search results.
      // the current approach sucks
//      results.first().matches.should.have.size(2)
//      results.first().matches.map { it.field }.should.have.elements(SearchField.NAME,SearchField.QUALIFIED_NAME)
   }
}
