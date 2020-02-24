package io.vyne.search.embedded

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.BaseDirectory
import org.apache.lucene.store.FSDirectory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.nio.file.Paths

@Import(VyneEmbeddedSearchConfiguration::class)
annotation class EnableVyneEmbeddedSearch

@Configuration
@ComponentScan(basePackageClasses = [EnableVyneEmbeddedSearch::class])
class VyneEmbeddedSearchConfiguration {

   @Bean
   fun searchRepository(@Value("\${vyne.search.directory:./search}") searchIndexPath: String, configFactory: ConfigFactory = DefaultConfigFactory()): SearchIndexRepository {
      val directory = FSDirectory.open(Paths.get(searchIndexPath));


      return SearchIndexRepository(directory,configFactory)
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

