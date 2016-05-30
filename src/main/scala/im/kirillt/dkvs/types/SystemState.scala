package im.kirillt.dkvs.types

trait SystemState

case class Follower() extends SystemState

case class Candidate() extends SystemState

case class Leader() extends SystemState
