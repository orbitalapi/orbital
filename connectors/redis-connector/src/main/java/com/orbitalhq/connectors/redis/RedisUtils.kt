package com.orbitalhq.connectors.redis

import com.orbitalhq.connectors.redis.RedisTaxi
import com.orbitalhq.models.AttributeName
import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn

fun findKeyField(type: Type): Pair<AttributeName, Field> {
   val keyField = type.getAttributesWithAnnotation("Id".fqn())
   return when (keyField.size) {
      1 -> keyField.entries.single().let { (k, v) -> k to v }
      else -> error("Cannot persist type ${type.qualifiedName.longDisplayName} to Redis, as there are ${keyField.size} fields with an @Id annotation - expected exactly one.")
   }
}

fun getRedisKeyPattern(type: Type): String {
   if (!type.hasMetadata(RedisTaxi.Annotations.RedisKey)) {
      error("Cannot persist type ${type.qualifiedName.longDisplayName} to Redis, as it does not have a @${RedisTaxi.Annotations.RedisKey.longDisplayName} annotation.")
   }
   val metadata = type.getMetadata(RedisTaxi.Annotations.RedisKey)
   return metadata.params["pattern"] as String
}

fun getTTLSeconds(type: Type): Int? {
   if (!type.hasMetadata(RedisTaxi.Annotations.RedisTTL)) {
      return null
   }
   val metadata = type.getMetadata(RedisTaxi.Annotations.RedisTTL)
   return metadata.params["seconds"] as? Int
}

/**
 * Build a Redis key from a pattern and a key value.
 * For example:
 * - pattern: "user:{userId}", keyValue: "123" -> "user:123"
 * - pattern: "order:{orderId}", keyValue: "456" -> "order:456"
 */
fun buildRedisKey(pattern: String, keyValue: Any): String {
   // Simple implementation: replace {anything} with the key value
   return pattern.replace(Regex("\\{[^}]+\\}"), keyValue.toString())
}

/**
 * Extract the scan pattern from a key pattern.
 * For example:
 * - pattern: "user:{userId}" -> "user:*"
 * - pattern: "order:{orderId}" -> "order:*"
 */
fun getScanPattern(pattern: String): String {
   return pattern.replace(Regex("\\{[^}]+\\}"), "*")
}
