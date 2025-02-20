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
