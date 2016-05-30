package im.kirillt.dkvs.message

class RequestVote(val term: Int, candidateID: Int, lastLogIndex: Int, lastLogTerm: Int)

object RequestVote {

  class Result(val int: Int, val voteGranted: Boolean)

}
