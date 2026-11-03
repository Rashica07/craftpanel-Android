package com.kiqa.omniservers.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Where the app is. Only one screen shows at a time; Detail remembers which server. */
sealed interface Screen {
    data object Connect : Screen
    data object Servers : Screen
    data class Detail(val serverId: String) : Screen
    data object Settings : Screen
}

enum class LinkState { Connecting, Online, Offline, Rejected }

data class UiState(
    val screen: Screen = Screen.Connect,
    val servers: List<ServerSummary> = emptyList(),
    val loaded: Boolean = false,
    val link: LinkState = LinkState.Connecting,
    val demo: Boolean = false,
    val connectionLabel: String = "",
    val console: List<ConsoleLine> = emptyList(),
    val players: PlayerList = PlayerList.Empty,
    /** A short message the UI shows once, then clears. */
    val toast: String? = null,
)

/**
 * Holds the connection and keeps the screens up to date by polling. The Mac's API is
 * plain request/response, so polling every couple of seconds is the whole "live" story.
 */
class AppViewModel(
    private val store: PairingStore?,
    private val backendFor: (Pairing) -> ServerBackend = { RemoteApi(it) },
    private val demoBackend: () -> ServerBackend = { DemoBackend() },
    private val pollMs: Long = 2_000,
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var backend: ServerBackend? = null
    private var poller: Job? = null

    init {
        when {
            store?.demo == true -> connectDemo(persist = false)
            store?.pairing != null -> connect(store.pairing!!, persist = false)
        }
    }

    fun connect(pairing: Pairing, persist: Boolean = true) {
        if (persist) { store?.pairing = pairing; store?.demo = false }
        begin(backendFor(pairing), demo = false, label = pairing.label)
    }

    fun connectDemo(persist: Boolean = true) {
        if (persist) { store?.demo = true; store?.pairing = null }
        begin(demoBackend(), demo = true, label = "Demo")
    }

    fun disconnect() {
        poller?.cancel(); backend = null
        store?.clear()
        _state.value = UiState()
    }

    private fun begin(b: ServerBackend, demo: Boolean, label: String) {
        poller?.cancel()
        backend = b
        _state.value = UiState(screen = Screen.Servers, demo = demo, connectionLabel = label, link = LinkState.Connecting)
        poller = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(pollMs)
            }
        }
    }

    private suspend fun refresh() {
        val b = backend ?: return
        try {
            val servers = b.servers()
            _state.update { it.copy(servers = servers, loaded = true, link = LinkState.Online) }
            (_state.value.screen as? Screen.Detail)?.let { d ->
                val console = runCatching { b.console(d.serverId) }.getOrNull()
                val players = runCatching { b.players(d.serverId) }.getOrNull()
                _state.update { it.copy(console = console ?: it.console, players = players ?: it.players) }
            }
        } catch (e: ApiError.Unauthorized) {
            _state.update { it.copy(link = LinkState.Rejected) }
        } catch (e: Exception) {
            _state.update { it.copy(link = LinkState.Offline) }
        }
    }

    fun retryNow() { viewModelScope.launch { refresh() } }

    fun open(serverId: String) {
        _state.update { it.copy(screen = Screen.Detail(serverId), console = emptyList(), players = PlayerList.Empty) }
        retryNow()
    }

    fun show(screen: Screen) { _state.update { it.copy(screen = screen) } }

    /** Back button: returns false when there is nowhere to go back to. */
    fun back(): Boolean = when (_state.value.screen) {
        is Screen.Detail, Screen.Settings -> { show(Screen.Servers); true }
        else -> false
    }

    fun toggle(server: ServerSummary) {
        val b = backend ?: return
        viewModelScope.launch {
            try {
                if (server.status.isOn) b.stop(server.id) else b.start(server.id)
                refresh()
            } catch (e: ApiError.Http) {
                toast(e.message ?: "That didn't work")
            } catch (e: ApiError) {
                toast(e.message ?: "That didn't work")
            }
        }
    }

    fun sendCommand(serverId: String, line: String) {
        val b = backend ?: return
        val clean = line.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            try {
                b.send(serverId, clean)
                _state.update { it.copy(console = b.console(serverId)) }
            } catch (e: ApiError) {
                toast(e.message ?: "Couldn't send that")
            }
        }
    }

    fun toast(message: String) { _state.update { it.copy(toast = message) } }
    fun toastShown() { _state.update { it.copy(toast = null) } }
}
