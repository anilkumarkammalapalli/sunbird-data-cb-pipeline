package org.ekstep.dp

import java.util

import com.google.gson.Gson
import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.Codecs._
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.scala.OutputTag
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.sunbird.dp.core.{BaseJobConfig, FlinkKafkaConnector}
import collection.JavaConverters._
import org.sunbird.dp.functions.TestStreamFunc
import org.sunbird.dp.util.FlinkUtil
//import org.apache.kafka.common.serialization.{Serde, Serdes}
//import org.apache.kafka.streams.StreamsBuilder
//import org.apache.kafka.streams.kstream.{Consumed, KStream, Produced}
import org.scalatest.Matchers


class BaseProcessFunTestSpec extends FlatSpec with Matchers with BeforeAndAfterAll with EmbeddedKafka {


  val flinkCluster = new MiniClusterWithClientResource(new MiniClusterResourceConfiguration.Builder()
    .setNumberSlotsPerTaskManager(1)
    .setNumberTaskManagers(1)
    .build)
  override def beforeAll(): Unit = {
    try {
      super.beforeAll()
      //EmbeddedKafka.start()
      println("===Is Running ===" + EmbeddedKafka.isRunning)
      flinkCluster.before()
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
    }
  }

  override def afterAll(): Unit = {
    try {
      super.afterAll()
      //EmbeddedKafka.stop()
      println("===Is Running ===" + EmbeddedKafka.isRunning)
      flinkCluster.after()
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
    }

  }

  val config = ConfigFactory.load("test.conf");
  val bsConfig: BaseJobConfig = new BaseJobConfig(config, "base-job");
  val kafkaConnector = new FlinkKafkaConnector(bsConfig)

  "TestFlinkProcess" should "able to process the event" in {
    println("===Is Running ===" + EmbeddedKafka.isRunning)

    val userDefinedConfig = EmbeddedKafkaConfig(kafkaPort = 9092, zooKeeperPort = 2181)
    withRunningKafkaOnFoundPort(userDefinedConfig) { implicit actualConfig =>
      println("Kafka is starting.." + EmbeddedKafka.isRunning)
    }


    implicit val env: StreamExecutionEnvironment = FlinkUtil.getExecutionContext(bsConfig)
    implicit val mapTypeInfo: TypeInformation[util.Map[String, AnyRef]] = TypeExtractor.getForClass(classOf[util.Map[String, AnyRef]])
    val kafkaMapConsumer = kafkaConnector.kafkaMapSource("k8s.telemetry.unique.flink")
    lazy val testMapStreamTag: OutputTag[util.Map[String, AnyRef]] = OutputTag[util.Map[String, AnyRef]]("test-stream-tag")
    val mapStream: SingleOutputStreamOperator[util.Map[String, AnyRef]] =
      env.addSource(kafkaMapConsumer, "telemetry-raw-events-consumer")
        .rebalance().keyBy(key => key.get("partition").asInstanceOf[Integer])
        .process(new TestStreamFunc(bsConfig)).name("TestFun")
    mapStream.getSideOutput(testMapStreamTag).addSink(kafkaConnector.kafkaMapSink("sunbirddev.telemetry.sink")).name("kafka-telemetry-failed-events-producer")
  }
}

class TestClass extends SourceFunction[util.Map[String, AnyRef]] {
  override def run(ctx: SourceContext[util.Map[String, AnyRef]]) {
    val EVENT_WITH_MESSAGE_ID: String =
      """
        |{"id":"sunbird.telemetry","ver":"3.0","ets":1529500243591,"params":{"msgid":"3fc11963-04e7-4251-83de-18e0dbb5a684","requesterId":"","did":"a3e487025d29f5b2cd599a8817ac16b8f3776a63","key":""},"events":[{"eid":"LOG","ets":1529499971358,"ver":"3.0","mid":"LOG:5f3c177f90bd5833deade577cc28cbb6","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"content-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/content/composite/v1/search"},{"protocol":"https"},{"method":"POST"},{}]}},{"eid":"LOG","ets":1529499971432,"ver":"3.0","mid":"LOG:17ffd4c05d66e0aa0ed0c1b337192eae","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"learner-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/learner/user/v1/read/159e93d1-da0c-4231-be94-e75b0c226d7c?fields=completeness,missingFields,lastLoginTime"},{"protocol":"https"},{"method":"GET"},{}]}},{"eid":"LOG","ets":1529499971436,"ver":"3.0","mid":"LOG:3946ef96e11ada0bec3722f68007850d","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"learner-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/learner/data/v1/role/read"},{"protocol":"https"},{"method":"GET"},{}]}},{"eid":"LOG","ets":1529499971438,"ver":"3.0","mid":"LOG:746341bbfac5363693478dff90e22123","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"learner-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/learner/course/v1/user/enrollment/list/159e93d1-da0c-4231-be94-e75b0c226d7c"},{"protocol":"https"},{"method":"GET"},{}]}},{"eid":"START","ets":1529499971560,"ver":"3.0","mid":"START:21e01edc45ab176abfd316bc52a8a544","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"user","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"0123673542904299520","l2":"0123673689120112640","l3":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"session","uaspec":{"agent":"Chrome","ver":"67.0.3396.79","system":"Mac OS","platform":"WebKit","raw":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.79 Safari/537.36"},"duration":1529499971560}},{"eid":"LOG","ets":1529499976065,"ver":"3.0","mid":"LOG:5da0c8e5582a85a4f2aabd6785bbdd91","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"content-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/content/composite/v1/search"},{"protocol":"https"},{"method":"POST"},{}]}},{"eid":"LOG","ets":1529499976133,"ver":"3.0","mid":"LOG:db6a4ea5bf6aba1d6d3aaa4cc66b0071","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"learner-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/learner/user/v1/read/159e93d1-da0c-4231-be94-e75b0c226d7c?fields=completeness,missingFields,lastLoginTime"},{"protocol":"https"},{"method":"GET"},{}]}},{"eid":"LOG","ets":1529499976136,"ver":"3.0","mid":"LOG:cdb9df09dee37b1488926b2c85402cc0","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"learner-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/learner/data/v1/role/read"},{"protocol":"https"},{"method":"GET"},{}]}},{"eid":"LOG","ets":1529499976138,"ver":"3.0","mid":"LOG:46eaf5cbdf7d748e311898df93ecec48","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"learner-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/learner/course/v1/user/enrollment/list/159e93d1-da0c-4231-be94-e75b0c226d7c"},{"protocol":"https"},{"method":"GET"},{}]}},{"eid":"LOG","ets":1529499977167,"ver":"3.0","mid":"LOG:519081a41ee9f1550a889c81ff18ca83","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"content-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/content/org/v1/search"},{"protocol":"https"},{"method":"POST"},{}]}},{"eid":"LOG","ets":1529499977178,"ver":"3.0","mid":"LOG:a6e33b9c669acc8231d9eb372e51a330","actor":{"id":"public","type":"public"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"tenant","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"","cdata":[],"rollup":{"l1":"0123673542904299520","l2":"0123673689120112640","l3":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"Called tenant info","params":[]}},{"eid":"LOG","ets":1529499977212,"ver":"3.0","mid":"LOG:634d18ebc34be27d9d2940b9526ced33","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"content-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/content/composite/v1/search"},{"protocol":"https"},{"method":"POST"},{}]}},{"eid":"LOG","ets":1529499987034,"ver":"3.0","mid":"LOG:c39e332ad0b7dc56c19cbc047184905a","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"content-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/content/composite/v1/search"},{"protocol":"https"},{"method":"POST"},{}]}},{"eid":"LOG","ets":1529499987116,"ver":"3.0","mid":"LOG:6ac822896cd8a1736d55806c13ada64c","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"learner-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/learner/user/v1/read/159e93d1-da0c-4231-be94-e75b0c226d7c?fields=completeness,missingFields,lastLoginTime"},{"protocol":"https"},{"method":"GET"},{}]}},{"eid":"LOG","ets":1529499987118,"ver":"3.0","mid":"LOG:97b4f69fc3fe8fead706060e67837caa","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"learner-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/learner/data/v1/role/read"},{"protocol":"https"},{"method":"GET"},{}]}},{"eid":"LOG","ets":1529499987121,"ver":"3.0","mid":"LOG:df3cd7e874f2073d6d6b8ec4b3bf0b76","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"learner-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/learner/course/v1/user/enrollment/list/159e93d1-da0c-4231-be94-e75b0c226d7c"},{"protocol":"https"},{"method":"GET"},{}]}},{"eid":"LOG","ets":1529499988208,"ver":"3.0","mid":"LOG:1daf5f045d5d9e0a2b46f3fedbeb5d0c","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"content-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/content/org/v1/search"},{"protocol":"https"},{"method":"POST"},{}]}},{"eid":"LOG","ets":1529499988217,"ver":"3.0","mid":"LOG:2cd16668d217a92264a516c3e0e86709","actor":{"id":"public","type":"public"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"tenant","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"","cdata":[],"rollup":{"l1":"0123673542904299520","l2":"0123673689120112640","l3":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"Called tenant info","params":[]}},{"eid":"LOG","ets":1529499988252,"ver":"3.0","mid":"LOG:c1e597bce7abfbf31137079fa31ceeeb","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"content-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/content/composite/v1/search"},{"protocol":"https"},{"method":"POST"},{}]}},{"eid":"LOG","ets":1529499994398,"ver":"3.0","mid":"LOG:e20e25e5b1b58e77697242277d3d39bd","actor":{"id":"159e93d1-da0c-4231-be94-e75b0c226d7c","type":"user"},"context":{"channel":"b00bc992ef25f1a9a8d63291e20efc8d","pdata":{"id":"local.sunbird.portal","ver":"0.0.1"},"env":"content-service","sid":"PCNHgbKZvh6Yis8F7BxiaJ1EGw0N3L9B","did":"cab2a0b55c79d12c8f0575d6397e5678","cdata":[],"rollup":{"l1":"ORG_001","l2":"0123673542904299520","l3":"0123673689120112640","l4":"b00bc992ef25f1a9a8d63291e20efc8d"}},"object":{},"tags":["b00bc992ef25f1a9a8d63291e20efc8d"],"edata":{"type":"api_access","level":"INFO","message":"","params":[{"url":"/content/content/v1/read/do_1125232413877207041109?fields=createdBy,status,mimeType&mode=edit"},{"protocol":"https"},{"method":"GET"},{}]}}],"mid":"56c0c430-748b-11e8-ae77-cd19397ca6b0","syncts":1529500243955}
        |""".stripMargin
    val gson = new Gson()
    val event1 = gson.fromJson(EVENT_WITH_MESSAGE_ID, new util.LinkedHashMap[String, AnyRef]().getClass).asInstanceOf[util.Map[String, AnyRef]].asScala ++ Map("partition" -> 0.asInstanceOf[AnyRef])
    ctx.collect(event1.asJava)
  }

  override def cancel(): Unit = {}
}
