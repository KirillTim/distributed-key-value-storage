package im.kirillt.dkvs

import im.kirillt.dkvs.protocol._

trait Follower {
  this: MainActor =>
  val followerBehavior: StateFunction = {
    case Event(ElectionTimeout, m: StateData) =>
      cancelElectionDeadline()
      m.self.actor ! BeginElection
      goto(Candidate) using m

    case Event(msg: AppendEntry, m: StateData) =>
      m.leader = Some(m.remoteNodes.find(_.name.equals(msg.leaderId)).get.actor)
      System.err.println("Follower: get message from leader")
      resetElectionDeadline()
      stay() using m

    case Event(msg: RequestVote, m: StateData) =>
      if (msg.term > m.currentTerm) {
        m.currentTerm = msg.term
        stay() using m
      } else {
        if (msg.term < m.currentTerm) {
          sender ! DeclineCandidate(m.currentTerm)
        } else {
          if (m.canVoteFor(msg.lastLogIndex, msg.lastLogTerm)) {
            resetElectionDeadline()
            m.voteForOnThisTerm = Some(msg.candidateName)
            sender ! VoteForCandidate(m.currentTerm)
            log.info("Vote for {}", msg.candidateName)
          } else {
            sender ! DeclineCandidate(m.currentTerm)
            log.info("Reject candidate {]", msg.candidateName)
          }
        }
        stay() using m
      }

    case Event(msg: VoteResponse, m: StateData) =>
      //ignore
      stay() using m

    case Event(Ping, m: StateData) =>
      sender ! ClientAnswer("Pong")
      stay() using m

    case Event(msg: ClientAnswer, m: StateData) =>
      msg.answerTo.get ! msg
      stay() using m

    case Event(msg: SetValue, m: StateData) =>
      m.leader.get ! new SetValue(msg.key, msg.value, Some(sender()))
      stay() using m

    case Event(msg: GetValue, m: StateData) =>
      m.leader.get ! new GetValue(msg.key, Some(sender()))
      stay() using m

    case Event(msg: DeleteValue, m: StateData) =>
      m.leader.get ! new DeleteValue(msg.key, Some(sender()))
      stay() using m

  }
}
