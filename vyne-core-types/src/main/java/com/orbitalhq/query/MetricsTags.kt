package com.orbitalhq.query



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


