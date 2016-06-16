package im.kirillt.dkvs.protocol

import akka.actor.{ActorRef, ActorSelection}
import im.kirillt.dkvs.model.ReplicateLog

import scala.collection.mutable

class NodeReference(val actor: ActorSelection, val name: String, var alive: Boolean = false)

class Self(val actor: ActorRef, val name:String)

class StateData(val self: Self, val remoteNodes: Seq[NodeReference]) {
  val log = ReplicateLog.empty()
  var leader: Option[ActorSelection] = None
  var term = 1
  val nextIndex = mutable.Map[ActorSelection, Int](remoteNodes.map(node => (node.actor, -1)): _*)
  val matchIndex = mutable.Map[ActorSelection, Int](remoteNodes.map(node => (node.actor, -1)): _*)
  var votesForMe = 0
  var voteForOnThisTerm: Option[String] = None

  def nextTerm(): StateData = {
    term += 1
    voteForOnThisTerm = None
    votesForMe = 0
    this
  }

  def canVoteFor(lastLogIndex: Int, lastLogTerm: Int) = voteForOnThisTerm match {
      case Some(name) => false
      case _ => log.atLeastAsUpToDateAsMe(lastLogIndex, lastLogTerm)
    }

}
