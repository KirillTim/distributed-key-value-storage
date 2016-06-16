package im.kirillt.dkvs

import im.kirillt.dkvs.protocol._

trait Candidate {
  this: MainActor =>

  val candidateBehavior: StateFunction = {
    //election
    case Event(BeginElection, m: StateData) =>
      log.info("start new election in {} term", m.term)
      resetElectionDeadline()
      m.remoteNodes.foreach(_.actor ! RequestVote(m.term, m.self.name, m.log.latsEntry.term, m.log.lastEntryIndex))
      m.votesForMe += 1
      stay() using m

    case Event(VoteForCandidate(term), m: StateData) =>
      log.info("get vote for self")
      m.votesForMe += 1
      if (m.votesForMe >= m.remoteNodes.size/2 + 1)
        goto(Leader) using m
      stay() using m

    case Event(DeclineCandidate(term), m : StateData) =>
      stay() using m

    // election response
    case Event(msg : RequestVote, m : StateData) if msg.term < m.term =>
      sender ! DeclineCandidate(m.term)
      stay() using m

    case Event(msg: RequestVote, m : StateData) if m.canVoteFor(msg.lastLogIndex, msg.lastLogTerm) =>
      sender ! VoteForCandidate(m.term)
      m.voteForOnThisTerm = Some(msg.candidateName)
      stay() using m

    case Event(msg: RequestVote, m: StateData) =>
      sender ! DeclineCandidate(m.term)
      stay() using m

    case Event(msg: AppendEntry, m : StateData) =>
      goto(Follower) using m

    case Event(msg: ClientMessage, m :StateData) =>
      stay() using m

    case Event(ElectionTimeout, m : StateData) =>
      log.info("election timeout fired")
      m.self.actor ! BeginElection
      stay() using m.nextTerm()
  }
}
