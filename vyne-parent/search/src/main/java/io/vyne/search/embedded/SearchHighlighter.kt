package io.vyne.search.embedded

import org.apache.lucene.search.Query
import org.apache.lucene.search.highlight.Highlighter
import org.apache.lucene.search.highlight.QueryScorer
import org.apache.lucene.search.highlight.SimpleHTMLFormatter
import org.apache.lucene.search.highlight.SimpleSpanFragmenter

object SearchHighlighter  {
   //   private String getHighlightedField(Query query, Analyzer analyzer, String fieldName, String fieldValue) throws IOException, InvalidTokenOffsetsException {
//      Formatter formatter = new SimpleHTMLFormatter("<span class="\"MatchedText\"">", "</span>");
//      QueryScorer queryScorer = new QueryScorer(query);
//      Highlighter highlighter = new Highlighter(formatter, queryScorer);
//      highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, Integer.MAX_VALUE));
//      highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
//      return highlighter.getBestFragment(this.analyzer, fieldName, fieldValue);
//   }
   fun newHighlighter(query:Query):Highlighter {
      val formatter = SimpleHTMLFormatter("<span class='matchedText'>","</span>")
      val queryScorer = QueryScorer(query)
      val highlighter = Highlighter(formatter,queryScorer)
      highlighter.textFragmenter = SimpleSpanFragmenter(queryScorer, Int.MAX_VALUE)
      highlighter.maxDocCharsToAnalyze = Int.MAX_VALUE
      return highlighter
   }
}
