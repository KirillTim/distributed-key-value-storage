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

    case Event(HeartBeatMessage, m: StateData) =>
      resetHeartbeatTimeout()
      stay() using m

    case Event(msg: RequestVote, m : StateData) =>
      if (m.canVoteFor(msg.lastLogIndex, msg.lastLogTerm)) {
        sender ! VoteForCandidate(m.term)
        log.info("Vote for {}", msg.candidateName)
      } else {
        sender ! DeclineCandidate(m.term)
        log.info("Reject candidate {]", msg.candidateName)
      }
      stay() using m

    case Event(msg: VoteResponse, m: StateData) =>
      //ignore
      stay() using m

    case Event(msg: ClientMessage, m : StateData) =>
      log.info("get request form user")
      stay() using m
  }
}
