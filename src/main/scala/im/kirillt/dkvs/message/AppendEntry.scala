package im.kirillt.dkvs.message

import im.kirillt.dkvs.types.LogEntry

class AppendEntry(val term: Int, val leaderId: Int, val prevLogIndex: Int,
                  val prevLogTerm: Int, val entries: Seq[LogEntry], val leaderCommit: Int)

object AppendEntry {

  class Result(val term: Int, val success: Boolean)

}



