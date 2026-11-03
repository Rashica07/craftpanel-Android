package com.kiqa.omniservers

import com.kiqa.omniservers.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private class Fake : ServerBackend {
        var fail: Exception? = null
        var servers = listOf(ServerSummary("a", "Alpha", "paper", "1.21", 2048, ServerStatus.Stopped, null, false))
        val calls = mutableListOf<String>()
        override suspend fun servers(): List<ServerSummary> { fail?.let { throw it }; return servers }
        override suspend fun start(id: String) { calls += "start $id"; fail?.let { throw it } }
        override suspend fun stop(id: String) { calls += "stop $id" }
        override suspend fun console(id: String) = listOf(ConsoleLine(1, "stdout", "hi"))
        override suspend fun send(id: String, line: String) { calls += "send $id $line" }
        override suspend fun players(id: String) = PlayerList(1, 10, listOf("P"))
    }

    private val fake = Fake()
    private lateinit var vm: AppViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = AppViewModel(store = null, backendFor = { fake }, demoBackend = { fake }, pollMs = 1_000_000)
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun startsOnTheConnectScreen() {
        assertEquals(Screen.Connect, vm.state.value.screen)
    }

    @Test fun connectingLoadsServersAndGoesOnline() {
        vm.connect(Pairing("h", 1, "t"), persist = false)
        val s = vm.state.value
        assertEquals(Screen.Servers, s.screen)
        assertEquals(LinkState.Online, s.link)
        assertEquals(listOf("Alpha"), s.servers.map { it.name })
        assertEquals("h:1", s.connectionLabel)
    }

    @Test fun unreachableMeansOffline() {
        fake.fail = ApiError.Unreachable(null)
        vm.connect(Pairing("h", 1, "t"), persist = false)
        assertEquals(LinkState.Offline, vm.state.value.link)
    }

    @Test fun badTokenMeansRejected() {
        fake.fail = ApiError.Unauthorized()
        vm.connect(Pairing("h", 1, "t"), persist = false)
        assertEquals(LinkState.Rejected, vm.state.value.link)
    }

    @Test fun openingAServerLoadsConsoleAndPlayers() {
        vm.connectDemo(persist = false)
        vm.open("a")
        val s = vm.state.value
        assertEquals(Screen.Detail("a"), s.screen)
        assertEquals("hi", s.console.single().text)
        assertEquals(listOf("P"), s.players.players)
    }

    @Test fun backWalksUpAndStopsAtServers() {
        vm.connectDemo(persist = false)
        vm.open("a")
        assertTrue(vm.back()); assertEquals(Screen.Servers, vm.state.value.screen)
        assertFalse(vm.back())
    }

    @Test fun toggleStartsAStoppedServerAndStopsARunningOne() {
        vm.connectDemo(persist = false)
        vm.toggle(fake.servers[0])
        vm.toggle(fake.servers[0].copy(status = ServerStatus.Running))
        assertEquals(listOf("start a", "stop a"), fake.calls)
    }

    @Test fun desktopErrorsBecomeAToastOnce() {
        vm.connectDemo(persist = false)
        fake.fail = ApiError.Http(400, "EULA not accepted")
        vm.toggle(fake.servers[0])
        assertEquals("EULA not accepted", vm.state.value.toast)
        vm.toastShown()
        assertNull(vm.state.value.toast)
    }

    @Test fun blankCommandsAreNotSent() {
        vm.connectDemo(persist = false)
        vm.sendCommand("a", "   ")
        vm.sendCommand("a", " list ")
        assertEquals(listOf("send a list"), fake.calls)
    }

    @Test fun disconnectReturnsToConnect() {
        vm.connectDemo(persist = false)
        vm.disconnect()
        assertEquals(Screen.Connect, vm.state.value.screen)
        assertTrue(vm.state.value.servers.isEmpty())
    }
}
