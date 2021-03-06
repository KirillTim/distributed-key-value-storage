package im.kirillt.dkvs.protocol

import java.io.PrintWriter

import akka.actor.{ActorRef, ActorSelection}
import im.kirillt.dkvs.model.{LogEntry, ReplicateLog}

import scala.collection.mutable
import scala.io.Source

class NodeReference(val actor: ActorSelection, val name: String, var alive: Boolean = false)

case class PendingResponse(answerTo: ActorRef, msg: ClientMessage, indexInLog: Int)

case class Self(actor: ActorRef, name: String)

class StateData(actor: ActorRef, name: String, val remoteNodes: Seq[NodeReference]) {
  val self = new Self(actor, name)
  val log = ReplicateLog.empty()
  var leader: Option[ActorRef] = None
  var currentTerm = 0
  val nextIndex = mutable.Map[String, Int]()
  val matchIndex = mutable.Map[String, Int]()
  var votesForMe = 0
  var votedForOnThisTerm: Option[String] = None
  var pendingResponse: Option[PendingResponse] = None
  var answerToClient = ""

  val storage = mutable.Map[String, String]()

  def updateData(): Unit = {
    rebuildStorage()
    saveLog()
  }

  def rebuildStorage(): Unit = {
    storage.clear()
    for (entry <- log.entries) {
      (entry.key, entry.value) match {
        case (key, null) =>
          val res = storage.remove(key)
          if (pendingResponse.isDefined && pendingResponse.get.indexInLog == entry.index) {
            answerToClient = if (res.isDefined) "DELETED" else "NOT_FOUND"
          }
        case (key, value) =>
          storage.put(key, value)
      }
    }
  }

  def saveLog(): Unit = {
    StateData.writeLog(this)
  }

  def newTerm(term: Int): StateData = {
    currentTerm = term
    votedForOnThisTerm = None
    votesForMe = 0
    this
  }

  def nextTerm() = newTerm(currentTerm + 1)

  def canVoteFor(lastLogIndex: Int, lastLogTerm: Int) = votedForOnThisTerm match {
    case Some(node) => false
    case _ => log.atLeastAsUpToDateAsMe(lastLogIndex, lastLogTerm)
  }

  def buildRequestVote() = new RequestVote(currentTerm, self.name, log.lastEntryIndex, log.lastEntryTerm)

  def buildAppendEntryFor(nodeName: String): AppendEntry = {
    val data = log.restFrom(nextIndex(nodeName))
    var prevLogTerm = 0
    if (matchIndex(nodeName) >= 0 && matchIndex(nodeName) < log.entries.size)
      prevLogTerm = log.entries(matchIndex(nodeName)).term
    new AppendEntry(currentTerm, self.name, matchIndex(nodeName), prevLogTerm, data, log.committedIndex)
  }

  def becomeLeader(): StateData = {
    votedForOnThisTerm = None
    votesForMe = 0
    leader = Some(self.actor)
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
    if ((msg.prevLogIndex >= 0 && msg.prevLogIndex <= log.lastEntryIndex) && log.entries(msg.prevLogIndex).term != msg.prevLogTerm)
      return false
    for (newEntry <- msg.entries) {
      if (newEntry.index < log.entries.size && log.entries(newEntry.index).term != newEntry.term) {
        log.removeNodesFrom(newEntry.index)
      }
      log.entries += newEntry
    }
    if (msg.leaderCommit > log.committedIndex)
      log.committedIndex = Math.min(msg.leaderCommit, log.lastEntryIndex)

    if (log.committedIndex > log.lastApplied) {
      log.lastApplied = log.committedIndex
      updateData()
    }

    true
  }

  def addEntry(key: String, value: String): Unit = {
    log.entries += LogEntry(log.lastEntryIndex + 1, currentTerm, key, value)
    /*for (node <- nextIndex.keys)
      nextIndex.put(node, log.lastEntryIndex+1)*/
  }
}

object StateData {
  def tryInitFromLogs(actor: ActorRef, name: String, remote: Seq[NodeReference]): StateData = {
    val result = new StateData(actor, name, remote)
    if (!new java.io.File(s"$name.log").exists)
      return result
    val filename = s"$name.log"
    for (line <- Source.fromFile(filename).getLines()) {
      val record = line.split(" ")
      if (record(3) == "EMPTY")
        record(3) = null
      val entry = LogEntry(record(0).toInt, record(1).toInt, record(2), record(3))
      result.log.entries += entry
    }
    result.currentTerm = result.log.lastEntryTerm
    result.rebuildStorage()
    result
  }

  def writeLog(meta: StateData): Unit = {
    val filename = s"${meta.self.name}.log"
    new PrintWriter(filename) {
      for (entry <- meta.log.entries) {
        var line = entry.index + " " + entry.term + " " + entry.key + " "
        if (entry.value == null)
          line += "EMPTY"
        else
          line += entry.value
        write(line + "\n")
      }
      close()
    }

  }
}