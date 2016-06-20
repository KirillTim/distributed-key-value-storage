package im.kirillt.dkvs

import im.kirillt.dkvs.protocol._

trait Follower {
  this: MainActor =>
  val followerBehavior: StateFunction = {
    case Event(ElectionTimeout, m: StateData) =>
      cancelElectionDeadline()
      m.self.actor ! BeginElection
      goto(Candidate) using m.nextTerm()

    case Event(msg: AppendEntry, m: StateData) =>
      System.err.println("get AppendEntry...")
      if (m.tryToAppendEntries(msg)) {
        System.err.println("accepted")
        val newM = m.newTerm(msg.term)
        newM.leader = Some(sender)
        sender ! new AppendSuccessful(m.self.name, newM.currentTerm, m.log.lastEntryIndex)
        resetElectionDeadline()
        stay() using newM
      } else {
        System.err.println("rejected")
        sender ! new AppendRejected(m.self.name, m.currentTerm)
        stay() using m
      }

    case Event(msg: RequestVote, m: StateData) =>
      resetElectionDeadline()
      if (msg.term < m.currentTerm) {
        sender ! new DeclineCandidate(m.currentTerm)
      } else {
        if (m.canVoteFor(msg.lastLogIndex, msg.lastLogTerm)) {
          m.votedForOnThisTerm = Some(msg.candidateName)
          sender ! new VoteForCandidate(m.currentTerm)
        }
      }
      stay() using m

      case Event(msg: VoteResponse, m: StateData) =>
      //ignore
      stay() using m

    case Event(msg: ClientAnswer, m: StateData) =>
      msg.answerTo.get ! msg
      stay() using m

    case Event(msg: Ping, m: StateData) =>
      sender ! ClientAnswer("Pong")
      stay() using m

    case Event(msg: SetValue, m: StateData) =>
      System.err.println("Follower: get message from client")
      m.leader.get ! new SetValue(msg.key, msg.value, Some(sender()))
      stay() using m

    case Event(msg: GetValue, m: StateData) =>
      System.err.println("Follower: get message from client")
      val value = m.storage.getOrElse(msg.key, "NOT_FOUND")
      sender ! ClientAnswer(s"VALUE ${msg.key} $value", msg.answerTo)
      stay() using m

    case Event(msg: DeleteValue, m: StateData) =>
      System.err.println("Follower: get message from client")
      m.leader.get ! new DeleteValue(msg.key, Some(sender()))
      stay() using m

  }
}
