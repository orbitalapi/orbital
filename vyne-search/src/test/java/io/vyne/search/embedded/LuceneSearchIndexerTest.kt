package io.vyne.search.embedded

import com.google.common.util.concurrent.MoreExecutors
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.schemaApi.SchemaSet
import io.vyne.schemaConsumerApi.SchemaStore
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Schema
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
import reactor.core.publisher.Sinks
import java.nio.file.Paths


class LuceneSearchIndexerTest {
   @Rule
   @JvmField
   val tempDir = TemporaryFolder()

   lateinit var indexer: SearchIndexer
   lateinit var repository: SearchIndexRepository
   lateinit var schema: Schema

   @Before
   fun setup() {
      val directory = FSDirectory.open(Paths.get(tempDir.root.canonicalPath))
      val indexWriterConfig = IndexWriterConfig(DefaultConfigFactory().config().analyzer)
      val indexWriter = IndexWriter(directory, indexWriterConfig)
      val mockSchemaStore = mock<SchemaStore>()
      val schemaSetChangedEventSink = Sinks.many().replay().latest<SchemaSetChangedEvent>()
      whenever(mockSchemaStore.schemaChanged).thenReturn(schemaSetChangedEventSink.asFlux())
      repository = SearchIndexRepository(indexWriter, VyneEmbeddedSearchConfiguration().searcherManager(indexWriter), DefaultConfigFactory())
      indexer = SearchIndexer(mockSchemaStore , repository, MoreExecutors.newDirectExecutorService())
         .apply {
            afterPropertiesSet()
         }
      log().info("Search index at ${tempDir.root.canonicalPath}")

      val src = """
[[ This is person, a human being ]]
type Person {
   firstName : FirstName as String
}

[[ It probably barks ]]
type Animal {
   @Indexed
   breed : Breed as String
}


type TradeRecord {}

@Foo
type TradeId inherits String
service ConsumedService {
   operation consumedOperation()
}

service SampleService {
   operation allTradeIds(): TradeId[]
   operation byTradeId(id: TradeId): TradeRecord
   operation findAllBreeds(): Breed[]
   operation consumeBreed(breed: Breed)
}


      """.trimIndent()
      val schemaSet = SchemaSet.just(src)
      schema = schemaSet.schema
      schemaSetChangedEventSink.tryEmitNext(SchemaSetChangedEvent(null, schemaSet))
   }

   @Test
   @Ignore
   fun searchResultsMatchOnTypeDoc() {
      val results = repository.search("human", schema)
      results.should.have.size(1)
      results.first().matches.should.have.size(1)
      results.first().matches.first().field.should.equal(SearchField.TYPEDOC)
   }

   @Test
   @Ignore
   fun searchResultsMatchOnName() {
      val results = repository.search("Pers", schema)
      results.should.have.size(1)
      results.first().matches.should.have.size(5)
   }

   @Test
   @Ignore
   fun whenNameIsLongishThenSearchingMatchesWithAFewLetters() {
      // Observed that matches weren't being made on TradeRecord when searching for Tra
      val results = repository.search("tra", schema)
      results.find { it.qualifiedName.fullyQualifiedName == "TradeRecord" }.should.not.be. `null`
   }

   @Test
   @Ignore
   fun searchResultsMatchOnPartialWordsInTypeDoc() {
      val results = repository.search("hum", schema)
      results.should.have.size(1)
      results.first().matches.should.have.size(1)
      results.first().matches.first().field.should.equal(SearchField.TYPEDOC)
   }

   @Test
   fun searchResultsMatchOnTypeName() {
      val results = repository.search("traderid", schema)
      results.should.have.size(1)
      results.first().producers.first().should.equal(
         OperationNames.qualifiedName("SampleService","allTradeIds")
      )
      results.first().consumers.first().should.equal(
         OperationNames.qualifiedName("SampleService","byTradeId")
      )
      results.first().metadata.first().name.fullyQualifiedName.should.equal("Foo")
   }

   @Test
   fun searchAnnotationResultsMatchOnAnnotationName() {
      val results = repository.search("@index", schema)
      results.should.have.size(1)
      results.first().producers.first().should.equal(
         OperationNames.qualifiedName("SampleService","findAllBreeds")
      )
      results.first().consumers.first().should.equal(
         OperationNames.qualifiedName("SampleService","consumeBreed")
      )
   }

   @Test
   @Ignore
   fun searchResultsMatchOnFieldName() {
      val results = repository.search("breed", schema)
      results.should.have.size(2)
      // TODO : Think about how the fields are represented in search results.
      // the current approach sucks
//      results.first().matches.should.have.size(2)
//      results.first().matches.map { it.field }.should.have.elements(SearchField.NAME,SearchField.QUALIFIED_NAME)
   }
}
