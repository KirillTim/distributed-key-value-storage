package im.kirillt.dkvs

import im.kirillt.dkvs.protocol._

trait Leader {
  this: MainActor =>
  val leaderBehavior: StateFunction = {
    case Event(ElectedAsLeader, m : StateData) =>
      resetHeartbeatTimeout()
      stay() using m

    case Event(HeartbeatTimeout, m : StateData) =>
      m.remoteNodes.foreach(_.actor ! HeartBeatMessage)
      stay() using m

    //case Event(msg: AppendEntry, m : StateData) if =>

    case Event(msg: ClientMessage, m :StateData) =>
      log.info("get request from user")
      stay() using m
  }
}
