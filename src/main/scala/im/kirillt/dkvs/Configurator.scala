package im.kirillt.dkvs

import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, SECONDS}


class Configurator(val NODE_NAME: String) {
  type NodeName = String
  private class HostPort(val host: String, val port: String)

  val SYSTEM_NAME = "dkvs"
  private val nodes = readHostPorts(SYSTEM_NAME)
  val HOST = nodes(NODE_NAME).host
  val PORT = nodes(NODE_NAME).port.toInt
  private val otherNodes = nodes - NODE_NAME

  val systemConfig = {
    val hostConf = ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + HOST)
    val portConf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + PORT)
    val regularConfig = ConfigFactory.load()
    hostConf.withFallback(portConf).withFallback(regularConfig)
  }


  val otherActorsPaths = otherNodes.map(node => (node._1, getPath(node._1, node._2)))

  private def getPath(name: NodeName, node: HostPort) = s"akka.tcp://$SYSTEM_NAME@${node.host}:${node.port}/user/$name"

  private def readHostPorts(fileName: String): Map[NodeName, HostPort] = {
    val config = ConfigFactory.load(fileName).getConfig("nodes")
    val data = mutable.Map[NodeName, HostPort]()
    val it = config.entrySet().iterator()
    while (it.hasNext) {
      val item = it.next()
      val name = item.getKey
      val value = config.getString(name).split(":")
      data.put(name, new HostPort(value(0), value(1)))
    }
    data.toMap
  }
}

object Configurator {
  val electionTimeoutMin = FiniteDuration(5, SECONDS)
  val electionTimeoutMax = FiniteDuration(10, SECONDS)
  val heartbeatTimeout = FiniteDuration(3, SECONDS)
}
