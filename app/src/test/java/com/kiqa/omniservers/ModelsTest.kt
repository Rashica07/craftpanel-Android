package com.kiqa.omniservers

import com.kiqa.omniservers.data.*
import org.junit.Assert.*
import org.junit.Test

class ModelsTest {
    @Test fun statusParsesDesktopWordsAndFallsBack() {
        assertEquals(ServerStatus.Running, ServerStatus.parse("running"))
        assertEquals(ServerStatus.Starting, ServerStatus.parse("Starting"))
        assertEquals(ServerStatus.Crashed, ServerStatus.parse("crashed"))
        assertEquals(ServerStatus.Stopped, ServerStatus.parse("unknown"))
        assertEquals(ServerStatus.Stopped, ServerStatus.parse(null))
    }

    @Test fun busyAndOnFlags() {
        assertTrue(ServerStatus.Starting.isBusy)
        assertTrue(ServerStatus.Stopping.isBusy)
        assertFalse(ServerStatus.Running.isBusy)
        assertTrue(ServerStatus.Running.isOn)
        assertTrue(ServerStatus.Starting.isOn)
        assertFalse(ServerStatus.Stopping.isOn)
    }

    @Test fun softwareAndRamLabels() {
        val s = ServerSummary("1", "A", "paper", "1.21.4", 4096, ServerStatus.Stopped, null, false)
        assertEquals("Paper 1.21.4", s.software)
        assertEquals("4 GB", s.ramLabel)
        val t = s.copy(type = "vanilla", mcVersion = null, ramMb = 1536)
        assertEquals("Vanilla", t.software)
        assertEquals("1536 MB", t.ramLabel)
    }

    @Test fun logLevels() {
        assertEquals(LogLevel.Command, LogLevel.of("> say hi", "stdout"))
        assertEquals(LogLevel.Error, LogLevel.of("[10:00:00] [Server thread/ERROR]: boom", "stdout"))
        assertEquals(LogLevel.Error, LogLevel.of("java.lang.NullPointerException", "stdout"))
        assertEquals(LogLevel.Error, LogLevel.of("anything", "stderr"))
        assertEquals(LogLevel.Warn, LogLevel.of("[10:00:00] [Server thread/WARN]: slow", "stdout"))
        assertEquals(LogLevel.Info, LogLevel.of("[10:00:00] [Server thread/INFO]: hi", "stdout"))
    }

    @Test fun pairingParsesTheDesktopPayload() {
        val p = Pairing.parse("""{"host":"192.168.1.20","port":8642,"token":"cp_abc"}""")
        assertEquals(Pairing("192.168.1.20", 8642, "cp_abc"), p)
        assertEquals("192.168.1.20:8642", p!!.label)
    }

    @Test fun pairingDefaultsPortAndTrimsWhitespace() {
        assertEquals(8642, Pairing.parse("  {\"host\":\" mac.local \",\"token\":\"t\"}\n")!!.port)
        assertEquals("mac.local", Pairing.parse("{\"host\":\" mac.local \",\"token\":\"t\"}")!!.host)
    }

    @Test fun pairingRejectsJunk() {
        assertNull(Pairing.parse("hello"))
        assertNull(Pairing.parse("{}"))
        assertNull(Pairing.parse("""{"host":"","token":"t"}"""))
        assertNull(Pairing.parse("""{"host":"h","token":""}"""))
        assertNull(Pairing.parse("""{"host":"h","port":99999,"token":"t"}"""))
    }
}
