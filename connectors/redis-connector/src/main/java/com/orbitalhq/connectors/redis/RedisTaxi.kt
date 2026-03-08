package com.orbitalhq.connectors.redis

import com.orbitalhq.VyneTypes
import com.orbitalhq.schemas.fqn

object RedisTaxi {
   object Annotations {
      internal val namespace = "${VyneTypes.NAMESPACE}.redis"
      val RedisServiceAnnotation = "${namespace}.RedisService"
      val RedisKey = "${namespace}.RedisKey".fqn()
      val RedisTTL = "${namespace}.RedisTTL".fqn()
      val RedisUpsertOperation = "${namespace}.RedisUpsertOperation".fqn()
      val RedisDeleteOperation = "${namespace}.RedisDeleteOperation".fqn()
      val RedisStreamName = "${namespace}.RedisStreamName".fqn()
      val RedisPubSubChannel = "${namespace}.RedisPubSubChannel".fqn()
   }

   val schema = """
namespace ${Annotations.namespace} {
   annotation RedisService {
      connectionName : String
   }

   annotation RedisKey {
      // Key pattern using placeholders, e.g., "user:{userId}" or "order:{orderId}"
      pattern : String
   }

   annotation RedisTTL {
      // Time-to-live in seconds
      seconds : Int
   }

   annotation RedisUpsertOperation {
      // Optional TTL override for this specific operation
      ttlSeconds : Int?
   }

   annotation RedisDeleteOperation {
      // Optional key pattern for delete operations, e.g., "user:*"
      keyPattern : String?
   }

   annotation RedisStreamName {
      // Name of the Redis Stream
      name : String
   }

   annotation RedisPubSubChannel {
      // Pub/Sub channel name or pattern
      channel : String
   }
}
      """.trimIndent()
}
