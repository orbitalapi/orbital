package io.vyne.search.embedded

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.search.ControlledRealTimeReopenThread
import org.apache.lucene.search.SearcherFactory
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.store.FSDirectory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.io.IOException
import java.nio.file.Paths


@Import(VyneEmbeddedSearchConfiguration::class)
annotation class EnableVyneEmbeddedSearch

@Configuration
@ComponentScan(basePackageClasses = [EnableVyneEmbeddedSearch::class])
class VyneEmbeddedSearchConfiguration {
   @Bean
   fun configFactory():ConfigFactory = DefaultConfigFactory()

   @Bean
   @Throws(IOException::class)
   fun indexWriter(@Value("\${vyne.search.directory:./search}") searchIndexPath: String, configFactory: ConfigFactory): IndexWriter {
      val directory = FSDirectory.open(Paths.get(searchIndexPath))
      val indexWriterConfig = IndexWriterConfig(configFactory().config().analyzer)
      val indexWriter = IndexWriter(directory, indexWriterConfig)
      // Clear index
      indexWriter.deleteAll()
      indexWriter.commit()
      return indexWriter
   }

   @Bean
   @Throws(IOException::class)
   fun searcherManager(indexWriter: IndexWriter): SearcherManager {
      val searchManager = SearcherManager(indexWriter, false, false, SearcherFactory())
      val cRTReopenThead: ControlledRealTimeReopenThread<*> = ControlledRealTimeReopenThread(
         indexWriter,
         searchManager,
         5.0,
         0.025)
      cRTReopenThead.isDaemon = true
      cRTReopenThead.name = "Update IndexReader Thread"
      cRTReopenThead.start()
      return searchManager
   }
}

interface ConfigFactory {
    fun config():IndexWriterConfig
}
class DefaultConfigFactory : ConfigFactory{
   override fun config():IndexWriterConfig {
      val analyzer = StandardAnalyzer()
      val config = IndexWriterConfig(analyzer)
      config.openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND
      return config
   }
}

