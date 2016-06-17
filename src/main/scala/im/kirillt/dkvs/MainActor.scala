package im.kirillt.dkvs

import akka.actor.{Actor, LoggingFSM}
import protocol._


import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.collection.mutable


class MainActor(val config: Configurator, val otherNodes: List[NodeReference]) extends Actor with LoggingFSM[RaftState, StateData]
  with Candidate with Follower with Leader {

  private val ElectionTimeoutTimerName = "election-timer"
  private val HeartbeatTimeoutTimerName = "heartbeat-timer"

  var electionDeadline: Deadline = 0.seconds.fromNow

  when(Candidate)(candidateBehavior)
  when(Follower)(followerBehavior)
  when(Leader)(leaderBehavior)

  startWith(Follower, new StateData(self, config.NODE_NAME, otherNodes))


  onTransition {
    case Follower -> Candidate => {
      System.err.println("Follower -> Candidate")
    }
    case Candidate -> Leader => {
      System.err.println("Candidate -> Leader")
    }
    case Candidate -> Follower => {
      System.err.println("Candidate -> Follower")
    }
  }

  resetElectionDeadline()

  initialize()

  def cancelHeartbeatTimeout(): Unit = {
    cancelTimer(HeartbeatTimeoutTimerName)
  }

  def resetHeartbeatTimeout(): Unit = {
    cancelTimer(HeartbeatTimeoutTimerName)
    setTimer(HeartbeatTimeoutTimerName, HeartbeatTimeout, Configurator.heartbeatTimeout, repeat = true)
  }

  def cancelElectionDeadline() {
    cancelTimer(ElectionTimeoutTimerName)
  }

  def resetElectionDeadline(): Deadline = {
    cancelTimer(ElectionTimeoutTimerName)
    electionDeadline = nextElectionDeadline()
    log.debug("Resetting election timeout: {}", electionDeadline)
    setTimer(ElectionTimeoutTimerName, ElectionTimeout, electionDeadline.timeLeft, repeat = false)
    electionDeadline
  }

  def nextElectionDeadline(): Deadline = randomElectionTimeout(
    Configurator.electionTimeoutMin,
    Configurator.electionTimeoutMax
  ).fromNow

  private def randomElectionTimeout(from: FiniteDuration, to: FiniteDuration): FiniteDuration = {
    val fromMs = from.toMillis
    val toMs = to.toMillis
    require(toMs > fromMs, s"to ($to) must be greater than from ($from) in order to create valid election timeout.")

    (fromMs + ThreadLocalRandom.current().nextInt(toMs.toInt - fromMs.toInt)).millis
  }
}
