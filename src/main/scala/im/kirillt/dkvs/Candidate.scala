package im.kirillt.dkvs

import im.kirillt.dkvs.protocol._

trait Candidate {
  this: MainActor =>

  val candidateBehavior: StateFunction = {
    //election
    case Event(BeginElection, m: StateData) =>
      log.info("start new election in {} term", m.currentTerm)
      resetElectionDeadline()
      m.remoteNodes.foreach(_.actor ! m.buildRequestVote())
      m.votesForMe += 1
      m.votedForOnThisTerm = Some(m.self.name)
      stay() using m

    case Event(msg: VoteForCandidate, m: StateData) =>
      log.info("get vote for self")
      m.votesForMe += 1
      if (m.votesForMe >= m.remoteNodes.size / 2 + 1) {
        m.self.actor ! ElectedAsLeader
        goto(Leader) using m.becomeLeader()
      } else {
        stay() using m
      }

    case Event(msg: DeclineCandidate, m: StateData) =>
      if (msg.term > m.currentTerm) {
        goto(Follower) using m.newTerm(msg.term)
      } else {
        stay() using m
      }

    case Event(msg: RequestVote, m: StateData) =>
      if (msg.term > m.currentTerm) {
        val newM = m.newTerm(msg.term)
        if (newM.canVoteFor(msg.lastLogIndex, msg.lastLogTerm))
          sender ! VoteForCandidate(m.currentTerm)
        goto(Follower) using newM
      } else {
        sender ! DeclineCandidate(m.currentTerm)
        stay() using m
      }

    case Event(msg: AppendEntry, m: StateData) =>
      if (m.tryToAppendEntries(msg)) {
        val newM = m.newTerm(msg.term)
        newM.leader = Some(sender)
        sender ! new AppendSuccessful(m.self.name, newM.currentTerm, m.log.lastEntryIndex)
        resetElectionDeadline()
        goto(Follower) using newM
      } else {
        sender ! new AppendRejected(m.self.name, m.currentTerm)
        stay() using m
      }

      //clients
    case Event(msg: Ping, m: StateData) =>
      sender ! ClientAnswer("Pong")
      stay() using m

    case Event(msg: SetValue, m: StateData) =>
      sender ! ClientAnswer("Don't know who is a leader :(")
      stay() using m

    case Event(msg: GetValue, m: StateData) =>
      sender ! ClientAnswer(m.storage.getOrElse(msg.key, "NOT_FOUND"))
      stay() using m

    case Event(msg: DeleteValue, m: StateData) =>
      sender ! ClientAnswer("Don't know who is a leader :(")
      stay() using m

    case Event(ElectionTimeout, m: StateData) =>
      log.info("election timeout fired")
      m.self.actor ! BeginElection
      stay() using m.nextTerm()
  }
}
