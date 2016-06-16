package im.kirillt.dkvs.protocol

import im.kirillt.dkvs.model.LogEntry

trait Message {

  sealed trait Message
  sealed trait InternalMessage extends Message
  sealed trait ExternalMessage extends Message
  sealed trait ClientMessage extends ExternalMessage

  sealed trait ElectionMessage extends InternalMessage
  sealed trait LeaderMessage extends InternalMessage

  case class AppendEntry(term: Int, leaderId: Int, prevLogIndex: Int, prevLogTerm: Int,
                         entries: Seq[LogEntry], leaderCommit: Int) extends ExternalMessage

  case object HeartBeatMessage extends ExternalMessage

  sealed trait AppendResponse extends ExternalMessage
  case class AppendRejected(term: Int) extends AppendResponse
  case class AppendSuccessful(term: Int, lastIndex: Int) extends AppendResponse

  case class RequestVote(term: Int, candidateName: String, lastLogIndex: Int, lastLogTerm: Int) extends ExternalMessage

  abstract class VoteResponse(val term: Int) extends ExternalMessage
  case class VoteForCandidate(myTerm: Int) extends VoteResponse(myTerm)
  case class DeclineCandidate(myTerm: Int) extends VoteResponse(myTerm)

  case class GetValue(key: String) extends ClientMessage
  case class SetValue(key: String, value: String) extends ClientMessage
  case class DeleteValue(key: String) extends ClientMessage
  case object Ping extends ClientMessage
  case class ClientAnswer(msg: String) extends ClientMessage

  case object BeginElection extends ElectionMessage
  case object ElectionTimeout extends ElectionMessage
  case object ElectedAsLeader extends ElectionMessage
  case object HeartbeatTimeout extends LeaderMessage

}