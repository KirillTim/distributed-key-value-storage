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
      m.nextIndex.put(msg.nodeName, msg.lastIndex + 1)
      System.err.println("Leader: prev commitindex = " + m.log.committedIndex)
      tryUpdateCommitIndex(m)
      System.err.println("Leader: now commitindex = " + m.log.committedIndex)
      if (m.log.committedIndex > m.log.lastApplied) {
        System.err.println("Leader: update applied index")
        m.log.lastApplied = m.log.committedIndex
        m.updateData()
        System.err.println("Leader: rebuild storage")
        if (m.pendingResponse.isDefined) {
          m.pendingResponse.get.msg match {
            case DeleteValue(key, client) =>
              m.pendingResponse.get.answerTo ! new ClientAnswer(m.answerToClient, client)
            case SetValue(key, value, client) =>
              m.pendingResponse.get.answerTo ! new ClientAnswer("STORED", client)
          }
        }
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
      System.err.println("Leader: get `get` message from client")
      val value = m.storage.getOrElse(msg.key, "NOT_FOUND")
      sender ! ClientAnswer(s"VALUE ${msg.key} $value", msg.answerTo)
      stay() using m

    case Event(msg: DeleteValue, m: StateData) =>
      System.err.println("Leader: get `delete` message from client")
      m.addEntry(msg.key, null)
      m.pendingResponse = Some(PendingResponse(sender, msg, m.log.lastEntryIndex))
      sendUpdates(m)
      stay() using m

    case Event(msg: SetValue, m: StateData) =>
      System.err.println("Leader: get `set` message from client")
      m.addEntry(msg.key, msg.value)
      m.pendingResponse = Some(PendingResponse(sender, msg, m.log.lastEntryIndex))
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
      sz >= meta.matchIndex.size / 2
    }
    for ((entry, index) <- meta.log.entries.zipWithIndex) {
      if (entry.term == meta.currentTerm && majorityAsBigAsN(index) && index > meta.log.committedIndex)
        meta.log.committedIndex = index
    }
  }
}
