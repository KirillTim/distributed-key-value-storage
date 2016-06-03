package im.kirillt.dkvs

import akka.actor.{Actor, ActorRef}
import im.kirillt.dkvs.message.Ping


class MainActor extends Actor {
  var count = 0

  def sendMsg(message: Any, receiver: ActorRef) = {
    receiver ! message
  }

  override def receive: Receive = {
    case Ping =>
      println("get ping message from " + sender.path + " adrr: "+sender.path.address)
      if (count == 5)
        println("finished")
      else {
        count += 1
        println(s"count = $count")
        sender ! Ping
      }
    case msg: String =>

      println(s"get plane string message: $msg from " + sender.path + " adrr: "+sender.path.address)
      if (count == 5)
        println("finished")
      else {
        count += 1
        println(s"count = $count")
        sender ! Ping
      }
    case _ =>

      println(s"get unknown type message from " + sender.path + " adrr: "+sender.path.address)
      if (count == 5)
        println("finished")
      else {
        count += 1
        println(s"count = $count")
        sender ! Ping
      }
  }


}
