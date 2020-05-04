// https://github.com/reactor/reactor-kafka/blob/master/src/test/java/reactor/kafka/cluster/EmbeddedKafkaCluster.java
package io.vyne.pipelines.runner.transport.kafka

import kafka.server.KafkaConfig
import kafka.server.KafkaServer
import kafka.utils.TestUtils
import kafka.utils.`ZKStringSerializer$`
import kafka.zk.EmbeddedZookeeper
import org.I0Itec.zkclient.ZkClient
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.network.ListenerName
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.utils.SystemTime
import org.junit.Assert
import scala.Option
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ServerSocketFactory

/*
 * Copyright (c) 2016-2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


class EmbeddedKafkaCluster(private val numBrokers: Int) {
   private var zookeeper: EmbeddedZookeeper? = null
   private var zkClient: ZkClient? = null
   private val brokers: MutableList<EmbeddedKafkaBroker> = ArrayList()
   private val started = AtomicBoolean()
   fun start() {
      if (started.compareAndSet(false, true)) {
         try {
            zookeeper = EmbeddedZookeeper()
            val zkConnect = "127.0.0.1:" + zookeeper!!.port()
            zkClient = ZkClient(zkConnect, 5000, 5000, `ZKStringSerializer$`.`MODULE$`)
            for (i in 0 until numBrokers) {
               val ss = ServerSocketFactory.getDefault().createServerSocket(0)
               val brokerPort = ss.localPort
               ss.close()
               val props = TestUtils.createBrokerConfig(i,
                  zkConnect,
                  true,
                  true,
                  brokerPort,
                  Option.apply(null),
                  Option.apply(null),
                  Option.apply(null),
                  true, false, 0, false, 0, false, 0,
                  Option.apply(null),
                  1,
                  false)
               props[KafkaConfig.MinInSyncReplicasProp()] = "1"
               props[KafkaConfig.TransactionsTopicReplicationFactorProp()] = "1"
               props[KafkaConfig.TransactionsTopicMinISRProp()] = "1"
               brokers.add(EmbeddedKafkaBroker(props))
            }
         } catch (e: IOException) {
            throw RuntimeException(e)
         }
      }
   }

   fun zkClient(): ZkClient? {
      return zkClient
   }

   fun bootstrapServers(): String {
      check(brokers.isNotEmpty()) { "Brokers have not been started" }
      val builder = StringBuilder()
      for (i in brokers.indices) {
         if (i != 0) builder.append(',')
         builder.append("127.0.0.1:")
         builder.append(brokers[i].server.boundPort(ListenerName("PLAINTEXT")))
         builder.append(",")
      }
      return builder.toString()
   }

   fun kafkaServer(brokerId: Int): KafkaServer {
      return brokers[brokerId].server
   }

   fun startBroker(brokerId: Int) {
      val broker = brokers[brokerId]
      broker.start()
   }

   fun shutdownBroker(brokerId: Int) {
      val broker = brokers[brokerId]
      broker.shutdown()
   }

   fun restartBroker(brokerId: Int) {
      val broker = brokers[brokerId]
      val maxRetries = 50
      for (i in 0 until maxRetries) {
         try {
            broker.start()
            break
         } catch (e: Exception) {
            Thread.sleep(500)
         }
      }
      waitForBrokers()
   }

   fun waitForBrokers() {
      val maxRetries = 50
      for (i in 0 until maxRetries) {
         try {
            bootstrapServers()
            break
         } catch (e: Exception) {
            Thread.sleep(500)
         }
      }
   }

   fun waitForTopic(topic: String?) {
      val props: MutableMap<String, Any> = HashMap()
      props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers()
      props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.java
      props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.java
      props[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 1000
      val producer = KafkaProducer<ByteArray, ByteArray>(props)
      val maxRetries = 10
      var done = false
      var i = 0
      while (i < maxRetries && !done) {
         val partitionInfo = producer.partitionsFor(topic)
         done = !partitionInfo.isEmpty()
         for (info in partitionInfo) {
            if (info.leader() == null || info.leader().id() < 0) done = false
         }
         i++
      }
      producer.close()
      Assert.assertTrue("Timed out waiting for topic", done)
   }

   internal class EmbeddedKafkaBroker(props: Properties?) {
      var server: KafkaServer
      fun start() {
         server.startup()
      }

      fun shutdown() {
         server.shutdown()
         server.awaitShutdown()
      }

      init {
         server = TestUtils.createServer(KafkaConfig(props), SystemTime())
      }
   }

}
