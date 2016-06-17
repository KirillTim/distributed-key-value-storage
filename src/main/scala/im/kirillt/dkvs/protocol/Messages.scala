package im.kirillt.dkvs.protocol

import akka.actor.ActorRef
import im.kirillt.dkvs.model.LogEntry

trait Messages {

  sealed trait Message
  sealed trait InternalMessage extends Message
  sealed trait ExternalMessage extends Message
  sealed trait ClientMessage extends ExternalMessage

  sealed trait ElectionMessage extends InternalMessage
  sealed trait LeaderMessage extends InternalMessage

  case class AppendEntry(term: Int, leaderId: String, prevLogIndex: Int, prevLogTerm: Int,
                         entries: Seq[LogEntry], leaderCommit: Int) extends ExternalMessage

  sealed trait AppendResponse extends ExternalMessage
  case class AppendRejected(node: String, term: Int) extends AppendResponse
  case class AppendSuccessful(node:String, term: Int, lastIndex: Int) extends AppendResponse

  case class RequestVote(term: Int, candidateName: String, lastLogIndex: Int, lastLogTerm: Int) extends ExternalMessage

  sealed trait VoteResponse extends ExternalMessage
  case class VoteForCandidate(myTerm: Int) extends VoteResponse
  case class DeclineCandidate(myTerm: Int) extends VoteResponse

  case class GetValue(key: String, answerTo: Option[ActorRef] = None) extends ClientMessage
  case class SetValue(key: String, value: String, answerTo: Option[ActorRef] = None) extends ClientMessage
  case class DeleteValue(key: String, answerTo: Option[ActorRef] = None) extends ClientMessage
  case class Ping() extends ClientMessage
  case class ClientAnswer(msg: String, answerTo: Option[ActorRef] = None) extends ClientMessage

  case object BeginElection extends ElectionMessage
  case object ElectionTimeout extends ElectionMessage
  case object ElectedAsLeader extends ElectionMessage
  case object HeartbeatTimeout extends LeaderMessage

}