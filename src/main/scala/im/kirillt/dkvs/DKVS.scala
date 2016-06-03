package im.kirillt.dkvs

import java.net.InetSocketAddress

import akka.actor._

import scala.collection.mutable.ArrayBuffer

object DKVS extends App {
  if (args.length < 1) {
    println("<node name> needed")
    System.exit(1)
  }
  val configurator = new Configurator(args(0))
  val system = ActorSystem(configurator.SYSTEM_NAME, configurator.systemConfig)
  val remoteActors = configurator.otherActorsPaths.map(path => system.actorSelection(path))
  val mainActor = system.actorOf(Props[MainActor], configurator.NODE_NAME)
  println("node name="+configurator.NODE_NAME)
  println("host="+configurator.HOST)
  println("port="+configurator.PORT)
  println("remotes:")
  for (i <- remoteActors) {
    println(i.pathString)
  }

  var msg = readLine()
  if (!msg.isEmpty) {
    remoteActors.foreach(_.tell(msg, mainActor))
  }


}

