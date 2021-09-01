package io.vyne.cask.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component

enum class FindOneMatchesManyBehaviour {
   RETURN_FIRST,
   THROW_ERROR
}

enum class QueryMatchesNoneBehaviour {
   RETURN_EMPTY,
   THROW_404
}

/**
 * General principles here:
 *  - Use enums that are self-describing versus a series of true/false flags
 *  - Populate with reasonable defaults
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "cask.query-options")
data class CaskQueryOptions(
   /**
    * When executing a findOne() query and multiple records match,
    * will pick the first record if this is set to true.
    * If this is set to false, will error (allowing Vyne to find
    * other endpoints to invoke)
    */
   val findOneMatchesManyBehaviour: FindOneMatchesManyBehaviour = FindOneMatchesManyBehaviour.THROW_ERROR,

   val queryMatchesNoneBehaviour: QueryMatchesNoneBehaviour = QueryMatchesNoneBehaviour.THROW_404
)

/**
 * General principles here:
 *  - Use enums that are self-describing versus a series of true/false flags
 *  - Populate with reasonable defaults
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "cask.dispatcher")
data class CaskQueryDispatcherConfiguration(
   /**
    * Size of query dispatcher thread pool size - ultimately limits the number of concurrent queries to
    * postgres
    */
   val queryDispatcherPoolSize: Int = 20,


)
