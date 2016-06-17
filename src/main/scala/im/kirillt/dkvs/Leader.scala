package im.kirillt.dkvs

import im.kirillt.dkvs.protocol._

trait Leader {
  this: MainActor =>
  val leaderBehavior: StateFunction = {
    case Event(ElectedAsLeader, m : StateData) =>
      cancelElectionDeadline() //no need
      resetHeartbeatTimeout()
      stay() using m

    case Event(HeartbeatTimeout, m : StateData) =>
      System.err.println("Leader: HeartbeatTimeout")
      m.remoteNodes.foreach(_.actor ! AppendEntry(m.term, m.self.name, m.log.lastEntryIndex, m.log.lastEntryTerm, List(), 0))
      stay() using m

    case Event(msg: RequestVote, m:StateData) =>
      sender ! AppendEntry(m.term, m.self.name, m.log.lastEntryIndex, m.log.lastEntryTerm, List(), 0)
      stay() using m

    //case Event(msg: AppendEntry, m : StateData) if =>

    case Event(msg: ClientMessage, m :StateData) =>
      log.info("get request from user")
      stay() using m
  }
}
