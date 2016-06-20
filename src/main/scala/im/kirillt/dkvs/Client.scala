package im.kirillt.dkvs

import java.io.PrintWriter

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import im.kirillt.dkvs.protocol._

import scala.collection.mutable
import scala.io.StdIn.readLine

object Client extends App {

  val configurator = new ClientConfig("127.0.0.1", "9000")
  val system = ActorSystem(configurator.SYSTEM_NAME, configurator.systemConfig)
  val nodes = configurator.allActorsPaths
    .map(path => (path._1, system.actorSelection(path._2)))

  implicit val clientActor = system.actorOf(Props[ClientActor], "client")

  println("Nodes:")
  for (i <- nodes.keys) {
    println(i)
  }

  var log = mutable.MutableList[String]()

  while (true) {
    println("Enter command")
    try {
      val command = readLine()
      log += command
      writeLog(log)
      val cmd = command.split(' ')
      cmd(0) = cmd(0).toLowerCase()
      cmd(1) = cmd(1).toLowerCase()
      cmd match {
        case Array("exit", _) =>
          sys.exit(1)
        case Array("ping", node) =>
          nodes(node) ! new Ping()
        case Array("get", node, key) =>
          nodes(node) ! new GetValue(key)
        case Array("set", node, key, value) =>
          nodes(node) ! new SetValue(key, value)
        case Array("delete", node, key) =>
          nodes(node) ! new DeleteValue(key)
        case _ =>
          println("unknown command")
      }
    } catch {
      case e : Exception =>
        println("bad command")
    }
  }

  def writeLog(what: mutable.MutableList[String]) {
    val filename = "client.log"
    new PrintWriter(filename) {
      for (i <- what) {
        write(s"$i\n")
      }
      close()
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