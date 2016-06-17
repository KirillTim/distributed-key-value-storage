package im.kirillt.dkvs

import akka.actor._
import im.kirillt.dkvs.protocol.NodeReference

object DKVS extends App {

  if (args.length < 1) {
    println("<node name> needed")
    System.exit(1)
  }
  val configurator = new Configurator(args(0))
  val system = ActorSystem(configurator.SYSTEM_NAME, configurator.systemConfig)
  val otherNodes = configurator.otherActorsPaths
    .map(path => new NodeReference(system.actorSelection(path._2), path._1)).toList
  val mainActor = system.actorOf(Props(new MainActor(configurator, otherNodes)), configurator.NODE_NAME)

  println("node name=" + configurator.NODE_NAME)
  println("host=" + configurator.HOST)
  println("port=" + configurator.PORT)

}

