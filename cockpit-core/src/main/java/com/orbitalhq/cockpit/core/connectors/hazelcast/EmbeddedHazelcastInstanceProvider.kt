package com.orbitalhq.cockpit.core.connectors.hazelcast

import com.hazelcast.config.Config
import com.hazelcast.config.MapConfig
import com.hazelcast.config.MapStoreConfig
import com.hazelcast.config.ReliableTopicConfig
import com.hazelcast.config.RingbufferConfig
import com.hazelcast.config.XmlConfigBuilder
import com.hazelcast.config.YamlConfigBuilder
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.spring.context.SpringManagedContext
import com.hazelcast.topic.TopicOverloadPolicy
import com.orbitalhq.connectors.hazelcast.HazelcastBuilder
import com.orbitalhq.connectors.hazelcast.OperationParamCompactSerializer
import com.orbitalhq.connectors.hazelcast.QualifiedNameCompactSerializer
import com.orbitalhq.pipelines.jet.api.transport.hazelcast.HazelcastTopicSinkSpec
import com.orbitalhq.pipelines.jet.streams.StreamStateManagerHazelcastConfig
import com.orbitalhq.pipelines.jet.streams.StreamStatusMapStore
import mu.KotlinLogging

private val logger = KotlinLogging.logger {  }

/**
 * Provides the Hazelcast Instance embedded with Orbital. Streaming Server functionality uses this embedded instances
 * to run streaming queries as Hazelcast Jet jobs.
 */
class EmbeddedHazelcastInstanceProvider {
    private fun decorateHazelcastConfig(mapStore: StreamStatusMapStore,
                                        springManagedContext: SpringManagedContext,
                                        config: Config,
                                        licenseKeyFn: () -> String?): Config {
        config.managedContext = springManagedContext
        logger.info { "setting the mapstore config for ${StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME}" }
        /**
         * We need to set our custom map / topic configurations here before instantiating the HazelcastInstance.
         */
        config.addMapConfig(getMapStoreConfig(mapStore))
        config.addReliableTopicConfig(getReliableTopicConfigForJetJobs(config))
        config.addRingBufferConfig(getRingBufferConfigForJetJobs(config))
        config.jetConfig.isEnabled = true
        licenseKeyFn().let { licenseKey ->
            config.licenseKey = licenseKey
        }
        val serializationConfig = config.serializationConfig
        serializationConfig.compactSerializationConfig.addSerializer(QualifiedNameCompactSerializer)
        serializationConfig.compactSerializationConfig.addSerializer(OperationParamCompactSerializer)
        HazelcastBuilder.customSerializers().forEach { serializerConfig ->
            serializationConfig.addSerializerConfig(serializerConfig)
        }
            //.addSerializerConfig(HazelcastBuilder.serializationConfig(schemaStore))
        return config
    }

    /**
     * Reliable Topic configuration to pass streaming query results back to Orbital for UI consumption.
     */
    private fun getReliableTopicConfigForJetJobs(config: Config): ReliableTopicConfig {
        return config.getReliableTopicConfig(HazelcastTopicSinkSpec.reliableTopicPrefix + "*")
            .setTopicOverloadPolicy(TopicOverloadPolicy.DISCARD_OLDEST)
    }

    /**
     * RingBuffer configuration for the Streaming Result ReliableTopic.
     */
    private fun getRingBufferConfigForJetJobs(config: Config): RingbufferConfig {
        return config.getRingbufferConfig(HazelcastTopicSinkSpec.reliableTopicPrefix + "*")
            .setTimeToLiveSeconds(10)
    }

    private fun getMapStoreConfig(mapStore: StreamStatusMapStore): MapConfig {
        val mapStoreConfig = MapStoreConfig().apply {
            isEnabled = true
            implementation = mapStore
            writeDelaySeconds = 0
        }

        val streamStatusMapConfig = MapConfig(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME)
        streamStatusMapConfig.setMapStoreConfig(mapStoreConfig)
        return streamStatusMapConfig
    }
    fun instance(
        mapStore: StreamStatusMapStore,
        springManagedContext: SpringManagedContext,
        hazelcastPort: Int = 25701,
        clusterName: String = "orbital",
        configYamlPath: String? = null,
        configXmlPath: String? = null,
        licenseKeyFn: () -> String? = { null }
    ): HazelcastInstance {
        return when {
            configXmlPath != null -> {
                logger.info { "Creating Hazelcast config from xml file at $configXmlPath" }
                val xmlCfg = XmlConfigBuilder(configXmlPath).build()
                Hazelcast.newHazelcastInstance(decorateHazelcastConfig(mapStore, springManagedContext, xmlCfg, licenseKeyFn))
            }

            configYamlPath != null -> {
                logger.info { "Creating Hazelcast config from yaml file at $configYamlPath" }
                val yamlCfg = YamlConfigBuilder(configYamlPath).build()
                Hazelcast.newHazelcastInstance(decorateHazelcastConfig(mapStore, springManagedContext, yamlCfg, licenseKeyFn))
            }

            else -> {
                logger.info { "hazelcast config yaml path is not provided, setting up multicast config with port: $hazelcastPort and cluster name $clusterName" }
                val config = Config()
                config.clusterName = clusterName
                config.networkConfig.port = hazelcastPort
                config.networkConfig.isPortAutoIncrement = true
                config.jetConfig.isEnabled = true
                config.networkConfig.join.autoDetectionConfig.setEnabled(true)
                return Hazelcast.newHazelcastInstance(decorateHazelcastConfig(mapStore, springManagedContext, config, licenseKeyFn))
            }
        }
    }
}