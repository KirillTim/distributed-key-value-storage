package im.kirillt.dkvs

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import im.kirillt.dkvs.protocol._
import im.kirillt.dkvs.protocol.NodeReference

import scala.collection.mutable

object Client extends App {

  val configurator = new ClientConfig("127.0.0.1", "9000")
  val system = ActorSystem(configurator.SYSTEM_NAME, configurator.systemConfig)
  val nodes = configurator.allActorsPaths
    .map(path => (path._1, system.actorSelection(path._2)))

  val clientActor = system.actorOf(Props[ClientActor], "client")

  println("Nodes:")
  for (i <- nodes.keys) {
    println(i)
  }

  while (true) {
    println("Enter command")
    val command = readLine().toLowerCase()
    val cmd = command.split(' ')
    if (cmd(0).equals("exit"))
      sys.exit(1)
    if (cmd(0).equals("ping")) {
      nodes(cmd(1)).tell(new Ping(), clientActor)
    } else if (cmd(0).equals("get")) {
      nodes(cmd(1)).tell(new GetValue(cmd(2)), clientActor)
    } else if (cmd(0).equals("set")) {
      nodes(cmd(1)).tell(new SetValue(cmd(2), cmd(3)), clientActor)
    } else if (cmd(0).equals("delete")) {
      nodes(cmd(1)).tell(new DeleteValue(cmd(2)), clientActor)
    }

  }
}

class ClientActor extends Actor {
  override def receive: Receive = {
    case (msg: ClientAnswer) =>
      println(msg.msg)
    case _ =>
      println("Unknown incoming message")
  }
}


class ClientConfig(val HOST: String, val PORT: String) {
  type NodeName = String

  private class HostPort(val host: String, val port: String)

  val SYSTEM_NAME = "dkvs"
  private val nodes = readHostPorts(SYSTEM_NAME)

  val systemConfig = {
    val hostConf = ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + HOST)
    val portConf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + PORT)
    val regularConfig = ConfigFactory.load()
    hostConf.withFallback(portConf).withFallback(regularConfig)
  }


  val allActorsPaths = nodes.map(node => (node._1, getPath(node._1, node._2)))

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