package com.orbitalhq.query

fun tagsOf() = MetricsTagBuilder()

class MetricsTagBuilder {
   companion object {
      const val QUERY_NAME_KEY = "queryStream"
   }
   private val tags = mutableListOf<Pair<String,String>>()

   private fun append(tag:Pair<String,String>):MetricsTagBuilder {
      tags.add(tag)
      return  this
   }
   fun queryStream(name: String):MetricsTagBuilder = append(QUERY_NAME_KEY to name)

   fun tags():MetricTags = MetricTags(tags)
}

data class MetricTags(
   val tags:List<Pair<String,String>>
) {
   companion object {
      /**
       * Disables metrics reporting. Use for internal / nested queries.
       * This is the default value in most places, to make sure we conciously opt-in
       * to capturing metrics
       */
      val NONE = MetricTags(emptyList())
   }
}


