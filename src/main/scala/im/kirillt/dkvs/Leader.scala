package im.kirillt.dkvs

import im.kirillt.dkvs.protocol._

trait Leader {
  this: MainActor =>
  val leaderBehavior: StateFunction = {
    case Event(ElectedAsLeader, m: StateData) =>
      cancelElectionDeadline() //no need
      resetHeartbeatTimeout()
      stay() using m

    case Event(HeartbeatTimeout, m: StateData) =>
      System.err.println("Leader: HeartbeatTimeout")
      m.remoteNodes.foreach((node: NodeReference) => {
        node.actor ! m.buildAppendEntryFor(node.name)
      })
      stay() using m

    case Event(msg: RequestVote, m: StateData) =>
      if (msg.term > m.currentTerm) {
        m.currentTerm = msg.term
        goto(Follower) using m
      } else {
        sender ! m.buildAppendEntryFor(msg.candidateName)
        stay() using m
      }

    case Event(msg: AppendSuccessful, m: StateData) =>
      m.matchIndex.put(msg.nodeName, msg.lastIndex)
      m.nextIndex.put(msg.nodeName, m.log.lastEntryIndex + 1)
      tryUpdateCommitIndex(m)
      if (m.log.committedIndex > m.log.lastApplied) {
        m.log.lastApplied = m.log.committedIndex
        m.rebuildStorage()
      }
      stay() using m

    case Event(msg: AppendRejected, m: StateData) =>
      if (msg.term > m.currentTerm) {
        goto(Follower) using m.newTerm(msg.term)
      } else {
        m.nextIndex.put(msg.nodeName, m.nextIndex.get(msg.nodeName).get - 1)
        sender ! m.buildAppendEntryFor(msg.nodeName)
        stay() using m
      }


    case Event(msg: VoteResponse, m: StateData) =>
      //ignore
      stay() using m

    //client messages

    case Event(msg: Ping, m: StateData) =>
      sender ! ClientAnswer("Pong")
      stay() using m

    case Event(msg: GetValue, m: StateData) =>
      log.info("get request from user")
      val data = m.storage.get(msg.key)
      sender ! ClientAnswer(data.getOrElse("NOT_FOUND"), msg.answerTo)
      stay() using m

    case Event(msg: DeleteValue, m: StateData) =>
      m.addEntry(msg.key, null)
      sendUpdates(m)
      stay() using m

    case Event(msg: SetValue, m: StateData) =>
      m.addEntry(msg.key, msg.value)
      sendUpdates(m)
      stay() using m
  }

  def sendUpdates(meta: StateData): Unit = {
    for (node <- meta.remoteNodes) {
      node.actor ! meta.buildAppendEntryFor(node.name)
    }
  }

  def tryUpdateCommitIndex(meta: StateData) = {
    def majorityAsBigAsN(n: Int): Boolean = {
      val sz = meta.matchIndex.values.count(_ >= n)
      sz > meta.matchIndex.size / 2
    }
    for ((entry, index) <- meta.log.entries.zipWithIndex) {
      if (entry.term == meta.currentTerm && majorityAsBigAsN(index) && index > meta.log.committedIndex)
        meta.log.committedIndex = index
    }
  }
}
