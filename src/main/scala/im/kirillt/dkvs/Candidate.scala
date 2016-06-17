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
      m.voteForOnThisTerm = Some(m.self.name)
      stay() using m

    case Event(VoteForCandidate(term), m: StateData) =>
      if (term > m.currentTerm) {
        m.currentTerm = term
        goto(Follower) using m
      } else {
        log.info("get vote for self")
        m.votesForMe += 1
        if (m.votesForMe >= m.remoteNodes.size / 2 + 1) {
          m.self.actor ! ElectedAsLeader
          goto(Leader) using m.becomeLeader()
        } else {
          stay() using m
        }
      }

    case Event(DeclineCandidate(term), m : StateData) =>
      if (term > m.currentTerm) {
        m.currentTerm = term
        goto(Follower) using m
      } else {
        stay() using m
      }

    case Event(msg: RequestVote, m: StateData) =>
      if (msg.term > m.currentTerm) {
        m.currentTerm = msg.term
        goto(Follower) using m
      } else {
        sender ! DeclineCandidate(m.currentTerm)
        stay() using m
      }

    case Event(msg: AppendEntry, m : StateData) =>
      System.err.println("Candidate: get message from leader")
      m.leader = Some(m.remoteNodes.find(_.name.equals(msg.leaderId)).get.actor)
      resetElectionDeadline()
      goto(Follower) using m

    case Event(Ping, m : StateData  ) =>
      sender ! ClientAnswer("Pong")
      stay() using m

    case Event(msg: ClientMessage, m :StateData) =>
      sender ! ClientAnswer("Don't know who is a leader :(")
      stay() using m

    case Event(ElectionTimeout, m : StateData) =>
      log.info("election timeout fired")
      m.self.actor ! BeginElection
      stay() using m.nextTerm()
  }
}
