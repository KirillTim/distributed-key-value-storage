package im.kirillt.dkvs.message

import im.kirillt.dkvs.types.LogEntry

trait Message

case class AppendEntry(term: Int, leaderId: Int, prevLogIndex: Int, prevLogTerm: Int,
                       entries: Seq[LogEntry], leaderCommit: Int) extends Message

object AppendEntry {

  class Result(val term: Int, val success: Boolean)

}

case class RequestVote(term: Int, candidateID: Int, lastLogIndex: Int, lastLogTerm: Int) extends Message

object RequestVote {

  class Result(val int: Int, val voteGranted: Boolean)

}

case class GetValue(key: String) extends Message

case class SetValue(key: String, value: String) extends Message

case class DeleteValue(key: String) extends Message

case class Ping() extends Message

case class ClientAnswer(msg: String) extends Message