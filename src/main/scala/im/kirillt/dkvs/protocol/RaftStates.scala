package im.kirillt.dkvs.protocol

trait RaftStates {
  sealed trait RaftState

  case object Follower  extends RaftState

  case object Candidate extends RaftState

  case object Leader    extends RaftState
}
