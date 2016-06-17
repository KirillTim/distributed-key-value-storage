package im.kirillt.dkvs

import im.kirillt.dkvs.protocol._

trait Follower {
  this: MainActor =>
  val followerBehavior: StateFunction = {
    case Event(ElectionTimeout, m: StateData) =>
      cancelElectionDeadline()
      m.self.actor ! BeginElection
      goto(Candidate) using m

    case Event(newEntries: AppendEntry, m: StateData) =>
      System.err.println("Follower: get message from leader")
      resetElectionDeadline()
      stay() using m

    case Event(msg: RequestVote, m: StateData) =>
      if (msg.term < m.currentTerm) {
        sender ! DeclineCandidate(m.currentTerm)
      } else {
        if (m.canVoteFor(msg.lastLogIndex, msg.lastLogTerm)) {
          m.voteForOnThisTerm = Some(msg.candidateName)
          sender ! VoteForCandidate(m.currentTerm)
          log.info("Vote for {}", msg.candidateName)
        } else {
          sender ! DeclineCandidate(m.currentTerm)
          log.info("Reject candidate {]", msg.candidateName)
        }
      }
      stay() using m

    case Event(msg: VoteResponse, m: StateData) =>
      //ignore
      stay() using m

    case Event(msg: ClientMessage, m: StateData) =>
      log.info("get request form user")
      stay() using m
  }
}
