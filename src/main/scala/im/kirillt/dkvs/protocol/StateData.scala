package im.kirillt.dkvs.protocol

import akka.actor.{ActorRef, ActorSelection}
import im.kirillt.dkvs.model.ReplicateLog

import scala.collection.mutable

class NodeReference(val actor: ActorSelection, val name: String, var alive: Boolean = false)

class Self(val actor: ActorRef, val name: String)

class StateData(actor: ActorRef, name: String, val remoteNodes: Seq[NodeReference]) {
  val self = new Self(actor, name)
  val log = ReplicateLog.empty()
  var leader: Option[ActorSelection] = None
  var currentTerm = 0
  val nextIndex = mutable.Map[String, Int]()
  val matchIndex = mutable.Map[String, Int]()
  var votesForMe = 0
  var voteForOnThisTerm: Option[String] = None

  def nextTerm(): StateData = {
    currentTerm += 1
    voteForOnThisTerm = None
    votesForMe = 0
    this
  }

  def canVoteFor(lastLogIndex: Int, lastLogTerm: Int) = voteForOnThisTerm match {
    case Some(node) => false
    case _ => log.atLeastAsUpToDateAsMe(lastLogIndex, lastLogTerm)
  }

  def buildRequestVote() = RequestVote(currentTerm, self.name, log.lastEntryIndex, log.lastEntryTerm)

  def becomeLeader(): StateData = {
    matchIndex.clear()
    nextIndex.clear()
    for (node <- remoteNodes) {
      matchIndex.put(node.name, -1)
      nextIndex.put(node.name, log.lastEntryIndex + 1)
    }

    this
  }

  def tryToAppendEntries(msg: AppendEntry): Boolean = {
    if (msg.term < currentTerm)
      return false
    if (msg.prevLogIndex < 0 || msg.prevLogIndex > log.lastEntryIndex || log.entries(msg.prevLogIndex).term != msg.prevLogTerm)
      return false
    if (msg.entries.isEmpty)
      return true
    for (newEntry <- msg.entries) {
      if (newEntry.index < log.entries.size && log.entries(newEntry.index).term != newEntry.term) {
        log.removeNodesFrom(newEntry.index)
      }
      log.entries += newEntry
    }
    if (msg.leaderCommit > log.committedIndex)
      log.committedIndex = Math.min(msg.leaderCommit, msg.entries.last.index)
    true
  }
}

