package im.kirillt.dkvs

import im.kirillt.dkvs.model.LogEntry
import im.kirillt.dkvs.protocol._

import scala.collection.mutable.ArrayBuffer

trait Leader {
  this: MainActor =>
  val leaderBehavior: StateFunction = {
    case Event(ElectedAsLeader, m: StateData) =>
      cancelElectionDeadline() //no need
      resetHeartbeatTimeout()
      stay() using m

    case Event(HeartbeatTimeout, m: StateData) =>
      System.err.println("Leader: HeartbeatTimeout")
      m.remoteNodes.foreach(_.actor ! AppendEntry(m.currentTerm, m.self.name, m.log.lastEntryIndex, m.log.lastEntryTerm, List(), 0))
      stay() using m

    case Event(msg: RequestVote, m: StateData) =>
      if (msg.term > m.currentTerm) {
        m.currentTerm = msg.term
        goto(Follower) using m
      } else {
        sender ! AppendEntry(m.currentTerm, m.self.name, m.log.lastEntryIndex, m.log.lastEntryTerm, List(), 0)
        stay() using m
      }

    case Event(msg: AppendSuccessful, m: StateData) =>
      m.nextIndex.put(msg.node, m.log.lastEntryIndex + 1)
      m.matchIndex.put(msg.node, m.log.lastEntryIndex)
      updateCommitIndex(m)
      stay() using m

    case Event(msg: AppendRejected, m: StateData) =>
      m.nextIndex.put(msg.node, m.nextIndex.get(msg.node).get - 1)
      sendUpdatesTo(msg.node, m)
      stay() using m

    case Event(msg: VoteResponse, m: StateData) =>
      //ignore
      stay() using m

    //client messages
    case Event(Ping, m : StateData  ) =>
      sender ! ClientAnswer("Pong")
      stay() using m

    case Event(msg: GetValue, m: StateData) =>
      log.info("get request from user")
      val data = m.storage.get(msg.key)
      if (data.isDefined)
        sender ! ClientAnswer(data.get, msg.answerTo)
      else
        sender ! ClientAnswer("EMPTY", msg.answerTo)
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
      if (meta.log.lastEntryIndex >= meta.nextIndex(node.name)) {
        sendUpdatesTo(node.name, meta)
      }

    }
  }

  def updateCommitIndex(meta: StateData) = {
    def majorityAsBigAsN(n : Int): Boolean = {
      val sz = meta.matchIndex.values.count(_ >= n)
      sz > meta.matchIndex.size / 2
    }
    for ((entry, index) <- meta.log.entries.zipWithIndex) {
      if (entry.term == meta.currentTerm && majorityAsBigAsN(index) && index > meta.log.committedIndex)
        meta.log.committedIndex = index
    }
  }

  def sendUpdatesTo(nodeName: String, meta: StateData): Unit = {
    val data = meta.log.restFrom(meta.nextIndex(nodeName))
    val msg = new AppendEntry(meta.currentTerm, meta.self.name, meta.log.lastEntryIndex, meta.log.lastEntryTerm, data, meta.log.committedIndex)
    val node = meta.remoteNodes.find(_.name.equals(nodeName)).head
    node.actor ! msg
  }
}
