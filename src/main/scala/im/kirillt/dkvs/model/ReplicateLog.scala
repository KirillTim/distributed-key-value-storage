package im.kirillt.dkvs.model

import scala.collection.mutable.ArrayBuffer

case class LogEntry(term: Int, key: String, value: String)

class ReplicateLog(val entries: ArrayBuffer[LogEntry], var committedIndex: Int) {
  def lastEntryIndex = entries.length
  def latsEntry = entries.last

  def atLeastAsUpToDateAsMe(lastLogIndex: Int, lastLogTerm: Int):Boolean = {
    if (lastLogTerm > latsEntry.term)
      return true
    lastLogIndex >= lastEntryIndex
  }
}

object ReplicateLog {
  def empty() = new ReplicateLog(ArrayBuffer.empty[LogEntry], 0)
}
