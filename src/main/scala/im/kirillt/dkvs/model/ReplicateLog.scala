package im.kirillt.dkvs.model

import scala.collection.mutable.ArrayBuffer

case class LogEntry(index: Int, term: Int, key: String, value: String)

class ReplicateLog(val entries: ArrayBuffer[LogEntry]) {
  var lastApplied = -1
  var committedIndex = -1

  def lastEntryIndex = entries.length - 1

  def lastEntry = entries.lastOption

  def lastEntryTerm = if (lastEntry.isDefined) lastEntry.get.term else 0

  def atLeastAsUpToDateAsMe(lastLogIndex: Int, lastLogTerm: Int): Boolean = {
    if (lastLogTerm > lastEntryTerm)
      return true
    lastLogIndex >= lastEntryIndex
  }

  def removeNodesFrom(index: Int): ReplicateLog = {
    entries.dropRight(entries.size - index)
    this
  }

  def restFrom(index: Int) = entries.takeRight(entries.size - index)
}

object ReplicateLog {
  def empty() = new ReplicateLog(ArrayBuffer.empty[LogEntry])
}
