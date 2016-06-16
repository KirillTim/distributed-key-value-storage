package im.kirillt.dkvs

import java.net.InetSocketAddress

import akka.actor._
import akka.pattern.ask
import im.kirillt.dkvs.types.NodeReference

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Promise
import concurrent._


object DKVS {//extends App {


    def main(args: Array[String]) {
      var line = ""
      import ExecutionContext.Implicits._
      val f = Future {
        blocking(Thread.sleep(5000L))
        this.synchronized {
          println("after 5 sec")
          if (line.isEmpty) {
            println("line is still empty")
          } else {
            println("line = " + line)
          }
          sys.exit(1)
        }
      }
      while (true) {
        line = readLine()
        if (line == "stop")

        println("readed: "+line)
      }
    }

//  if (args.length < 1) {
//    println("<node name> needed")
//    System.exit(1)
//  }
//  val configurator = new Configurator(args(0))
//  val system = ActorSystem(configurator.SYSTEM_NAME, configurator.systemConfig)
//  val otherNodes = configurator.otherActorsPaths
//    .map(path => new NodeReference(system.actorSelection(path._2), path._1)).toList
//  val mainActor = system.actorOf(Props(new MainActor(otherNodes)), configurator.NODE_NAME)
//
//  println("node name=" + configurator.NODE_NAME)
//  println("host=" + configurator.HOST)
//  println("port=" + configurator.PORT)
//
//  while (true) {
//    var msg = readLine()
//    if (!msg.isEmpty) {
//      mainActor ! IAmAlive()
//    } else
//      sys.exit(1)
//  }


}

