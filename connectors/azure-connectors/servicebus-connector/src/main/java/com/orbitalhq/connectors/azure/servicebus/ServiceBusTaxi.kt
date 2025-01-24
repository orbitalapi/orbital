package com.orbitalhq.connectors.azure.servicebus

import com.orbitalhq.VyneTypes
import com.orbitalhq.annotations.AnnotationWrapper
import com.orbitalhq.connections.ConnectionUsageMetadataRegistry
import com.orbitalhq.connections.ConnectionUsageRegistration
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTaxi.Annotations.ServiceBusService.Companion.BatchDurationAttribute
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTaxi.Annotations.ServiceBusService.Companion.BatchMaximumSizeInBytesAttribute
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTaxi.Annotations.ServiceBusService.Companion.BatchSizeAttribute
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTaxi.Annotations.ServiceBusService.Companion.batchDurationAttributeName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTaxi.Annotations.ServiceBusService.Companion.batchMaximumSizeInBytesAttributeName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTaxi.Annotations.ServiceBusService.Companion.batchSizeAttributeName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTaxi.Annotations.ServiceBusTopicPublicationOperation.Companion.topicMetadataName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTaxi.Annotations.ServiceBusTopicSubscriptionOperation.Companion.subscriptionMetadataName
import com.orbitalhq.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

object ServiceBusTaxi {
    fun registerConnectionUsage() {
        ConnectionUsageMetadataRegistry.register(
            ConnectionUsageRegistration(Annotations.ServiceBusService.NAME.fqn(), "connection")
        )
    }

    val schema = """
namespace  ${Annotations.namespace} {
   annotation ${Annotations.ServiceBusService.NAME.fqn().name} {
      connectionName : ConnectionName inherits String
   }
   
   type $BatchSizeAttribute inherits Int
   type $BatchDurationAttribute inherits Int
   type $BatchMaximumSizeInBytesAttribute inherits Int

  type SubscriptionName inherits String
  type TopicName inherits String
  annotation ${Annotations.ServiceBusTopicSubscriptionOperation.NAME.fqn().name} {
      $topicMetadataName : TopicName
      $subscriptionMetadataName: SubscriptionName
   }
   
   annotation ${Annotations.ServiceBusTopicPublicationOperation.NAME.fqn().name} {
      $topicMetadataName : TopicName
      $batchSizeAttributeName: $BatchSizeAttribute?
      $batchDurationAttributeName: $BatchDurationAttribute?
      $batchMaximumSizeInBytesAttributeName: $BatchMaximumSizeInBytesAttribute?
   }
   
  annotation ${Annotations.ServiceBusQueueOperation.NAME.fqn().name} {
      queue : QueueName inherits String
      $batchSizeAttributeName: $BatchSizeAttribute?
      $batchDurationAttributeName: $BatchDurationAttribute?
      $batchMaximumSizeInBytesAttributeName: $BatchMaximumSizeInBytesAttribute?
   }
}
"""
    object Annotations {
        internal val namespace = "${VyneTypes.NAMESPACE}.azure.servicebus"
        data class ServiceBusService(val connectionName: String) : AnnotationWrapper {
            companion object {
                val NAME = "$namespace.ServiceBusService"
                const val BatchSizeAttribute = "BatchSize"
                const val BatchDurationAttribute = "BatchDuration"
                const val batchSizeAttributeName = "batchSize"
                const val batchDurationAttributeName = "batchDuration"
                const val batchMaximumSizeInBytesAttributeName = "maximumSizeInBytes"
                const val BatchMaximumSizeInBytesAttribute = "MaximumSizeInBytes"
                const val ConnectionAttribute = "connectionName"
                fun from(annotation: Annotation): ServiceBusService {
                    require(annotation.qualifiedName == NAME) { "Annotation name should be $NAME" }
                    return ServiceBusService(
                        annotation.parameters[ConnectionAttribute] as String
                    )
                }

            }

            override fun asAnnotation(schema: TaxiDocument): Annotation {
                return Annotation(
                    type = schema.annotation(NAME),
                    parameters = mapOf(
                        ConnectionAttribute to connectionName
                    )
                )
            }

            val parameterMap = mapOf(ConnectionAttribute to connectionName)
            fun asMetadata(): com.orbitalhq.schemas.Metadata {
                return com.orbitalhq.schemas.Metadata(
                    NAME.fqn(),
                    parameterMap
                )
            }
        }

        data class ServiceBusTopicSubscriptionOperation(val topic: String, val subscription: String) : AnnotationWrapper {
            companion object {
                const val topicMetadataName = "topic"
                const val subscriptionMetadataName = "subscription"
                val NAME = "$namespace.ServiceBusTopicSubscriptionOperation"
                fun from(annotation: Annotation): ServiceBusTopicSubscriptionOperation {
                    return from(annotation.parameters)
                }

                fun from(annotation: com.orbitalhq.schemas.Metadata): ServiceBusTopicSubscriptionOperation {
                    return from(annotation.params)
                }

                private fun from(parameters: Map<String, Any?>): ServiceBusTopicSubscriptionOperation {
                    return ServiceBusTopicSubscriptionOperation(
                        topic = parameters[topicMetadataName] as String,
                        subscription = parameters[subscriptionMetadataName] as String
                    )
                }
            }

            fun asMetadata(): com.orbitalhq.schemas.Metadata {
                return com.orbitalhq.schemas.Metadata(
                    name = NAME.fqn(),
                    params = parameterMap
                )
            }

            private val parameterMap: Map<String, Any> = mapOf(
                topicMetadataName to topic,
                subscriptionMetadataName to subscription
            )

            override fun asAnnotation(schema: TaxiDocument): Annotation {
                return Annotation(
                    type = schema.annotation(NAME),
                    parameters = parameterMap
                )
            }
        }

        data class ServiceBusTopicPublicationOperation(val topic: String,
                                                       val batchSize: Int? = null,
                                                       val batchDuration: Long? = null) : AnnotationWrapper {
            companion object {
                const val topicMetadataName = "topic"
                val NAME = "$namespace.ServiceBusTopicPublicationOperation"
                fun from(annotation: Annotation): ServiceBusTopicPublicationOperation {
                    return from(annotation.parameters)
                }

                fun from(annotation: com.orbitalhq.schemas.Metadata): ServiceBusTopicPublicationOperation {
                    return from(annotation.params)
                }

                private fun from(parameters: Map<String, Any?>): ServiceBusTopicPublicationOperation {
                    return ServiceBusTopicPublicationOperation(
                        topic = parameters[topicMetadataName] as String,
                        batchSize = parameters[batchSizeAttributeName]?.let { it as Int },
                        batchDuration = parameters[batchDurationAttributeName]?.let {
                            (it as Int).toLong()
                        }
                    )
                }
            }

            fun asMetadata(): com.orbitalhq.schemas.Metadata {
                return com.orbitalhq.schemas.Metadata(
                    name = NAME.fqn(),
                    params = parameterMap
                )
            }

            private val parameterMap: Map<String, Any> = mapOf(
                topicMetadataName to topic,

            )

            val isBatch = batchDuration != null && batchSize != null
            val cacheSpec: ServiceBusCacheSpec? = if (isBatch) ServiceBusCacheSpec(batchSize!!, batchDuration!!) else null

            override fun asAnnotation(schema: TaxiDocument): Annotation {
                return Annotation(
                    type = schema.annotation(NAME),
                    parameters = parameterMap
                )
            }
        }


        data class ServiceBusQueueOperation(val queue: String, val batchSize: Int? = null, val batchDuration: Long? = null) : AnnotationWrapper {
            companion object {
                const val queueMetadataName = "queue"
                val NAME = "$namespace.ServiceBusQueueOperation"
                fun from(annotation: Annotation): ServiceBusQueueOperation {
                    return from(annotation.parameters)
                }

                fun from(annotation: com.orbitalhq.schemas.Metadata): ServiceBusQueueOperation {
                    return from(annotation.params)
                }

                private fun from(parameters: Map<String, Any?>): ServiceBusQueueOperation {
                    return ServiceBusQueueOperation(
                        queue = parameters[queueMetadataName] as String,
                        batchSize = parameters[batchSizeAttributeName]?.let { it as Int },
                        batchDuration = parameters[batchDurationAttributeName]?.let {
                            (it as Int).toLong()
                        }
                    )
                }
            }

            val isBatch = batchDuration != null && batchSize != null

            val cacheSpec: ServiceBusCacheSpec? = if (isBatch) ServiceBusCacheSpec(batchSize!!, batchDuration!!) else null

            fun asMetadata(): com.orbitalhq.schemas.Metadata {
                return com.orbitalhq.schemas.Metadata(
                    name = NAME.fqn(),
                    params = parameterMap
                )
            }

            private val parameterMap: Map<String, Any> = mapOf(
                queueMetadataName to queue
            )

            override fun asAnnotation(schema: TaxiDocument): Annotation {
                return Annotation(
                    type = schema.annotation(NAME),
                    parameters = parameterMap
                )
            }
        }


        data class ServiceBusCacheSpec(val batchSize: Int, val batchDuration: Long)

        val imports: String = listOf(
            ServiceBusService.NAME,
            ServiceBusTopicSubscriptionOperation.NAME,
            ServiceBusTopicPublicationOperation.NAME,
            ServiceBusQueueOperation.NAME).joinToString("\n") { "import $it" }
    }
}
