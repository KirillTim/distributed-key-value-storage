package im.kirillt.dkvs

import com.typesafe.config.ConfigFactory

import scala.collection.mutable


class Configurator(val NODE_NAME: String) {
  class HostPort(val host:String, val port:String)
  val SYSTEM_NAME = "dkvs"
  private val nodes = readHostPorts("dkvs")
  val HOST = nodes(NODE_NAME).host
  val PORT = nodes(NODE_NAME).port.toInt
  private val otherNodes = nodes - NODE_NAME

  val systemConfig = {
    val hostConf = ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + HOST)
    val portConf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + PORT)
    val regularConfig = ConfigFactory.load()
    hostConf.withFallback(portConf).withFallback(regularConfig)
  }

  val otherActorsPaths =
    otherNodes.map(node => "akka.tcp://"+SYSTEM_NAME+"@"+node._2.host+":"+node._2.port+"/user/"+node._1).toList


  private def readHostPorts(fileName:String) : Map[String, HostPort] ={
    val config = ConfigFactory.load(fileName).getConfig("nodes")
    val data = mutable.Map[String,HostPort]()
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
