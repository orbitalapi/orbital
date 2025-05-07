package com.orbitalhq.query

fun tagsOf() = MetricsTagBuilder()

class MetricsTagBuilder {
   companion object {
      const val QUERY_NAME_KEY = "queryStream"
   }
   private val tags = mutableListOf<Pair<String,String>>()

   private fun append(tag:Pair<String,String>): MetricsTagBuilder {
      tags.add(tag)
      return  this
   }
   fun queryStream(name: String): MetricsTagBuilder = append(QUERY_NAME_KEY to name)

   fun tags(): MetricTags = MetricTags(tags)
}


// MP: 1-May-25: This is part of a suboptimal solution,
// let's get rid of it asap
// Need a way of monitoring the error stream from the
// QueryResult that only monitors the "outer" query stream
enum class EmitMetrics(val results: Boolean, val errors: Boolean) {
   None(false, false),
   ResultCounts(true,false),
   ErrorCounts(false, true),
   ResultsAndErrors(true, true)
}
