package com.kiqa.omniservers

import com.kiqa.omniservers.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DemoBackendTest {
    private var t = 1_000_000L
    private val demo = DemoBackend { t }

    @Test fun startsWithOneRunningServer() = runBlocking {
        val s = demo.servers()
        assertEquals(3, s.size)
        assertEquals(listOf(ServerStatus.Running, ServerStatus.Stopped, ServerStatus.Stopped), s.map { it.status })
    }

    @Test fun startGoesThroughStartingThenRunning() = runBlocking {
        demo.start("demo-creative")
        assertEquals(ServerStatus.Starting, demo.servers().first { it.id == "demo-creative" }.status)
        t += 4
        val s = demo.servers().first { it.id == "demo-creative" }
        assertEquals(ServerStatus.Running, s.status)
        assertNotNull(s.startedAt)
    }

    @Test fun stopGoesThroughStoppingThenStopped() = runBlocking {
        demo.stop("demo-survival")
        assertEquals(ServerStatus.Stopping, demo.servers()[0].status)
        t += 3
        assertEquals(ServerStatus.Stopped, demo.servers()[0].status)
        assertEquals(0, demo.players("demo-survival").online)
    }

    @Test fun commandsEchoAndAnswer() = runBlocking {
        demo.send("demo-survival", "list")
        val lines = demo.console("demo-survival")
        assertEquals("> list", lines[lines.size - 2].text)
        assertTrue("players online" in lines.last().text)
    }

    @Test fun consoleSequenceNumbersIncrease() = runBlocking {
        demo.send("demo-survival", "save-all")
        val seqs = demo.console("demo-survival").map { it.seq }
        assertEquals(seqs.sorted(), seqs)
        assertEquals(seqs.size, seqs.toSet().size)
    }
}
