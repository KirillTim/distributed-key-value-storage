import im.kirillt.dkvs.model._
import im.kirillt.dkvs.protocol._


object AppendEntriesTest {
  def main(args: Array[String]) {
    val state = new StateData(null, "", List())
    state.currentTerm = 3
    state.log.entries += LogEntry(0, 3, "k0", "v0")
    state.log.entries += LogEntry(1, 3, "k1", "v1")
    var appendEntries = new AppendEntry(3, "1", 3, 3, List(), 0)
    assert(!state.tryToAppendEntries(appendEntries))
    val entry3 = LogEntry(3, 3, "k3", "v3")
    appendEntries = new AppendEntry(3, "1", 2, 3, List(entry3), 0)
    assert(!state.tryToAppendEntries(appendEntries))
    val entry2 = LogEntry(2, 3, "k2", "v2")
    appendEntries = new AppendEntry(3, "1", 1, 3, List(entry2, entry3), 0)
    assert(state.tryToAppendEntries(appendEntries))
    assert(state.log.entries.contains(entry2))
    assert(state.log.entries.contains(entry3))
  }
}
