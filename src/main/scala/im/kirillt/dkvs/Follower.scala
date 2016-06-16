package im.kirillt.dkvs

import im.kirillt.dkvs.protocol._

trait Follower {
  this: MainActor =>
  val followerBehavior: StateFunction = {
    case Event(ElectionTimeout, m: StateData) =>
      m.self.actor ! BeginElection
      goto(Candidate) using m

    case Event(newEntries: AppendEntry, m: StateData) =>
      resetElectionDeadline()
      stay() using m

    case
  }
}
