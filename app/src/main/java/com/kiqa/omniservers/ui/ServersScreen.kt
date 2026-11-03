package com.kiqa.omniservers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kiqa.omniservers.data.LinkState
import com.kiqa.omniservers.data.ServerStatus
import com.kiqa.omniservers.data.ServerSummary
import com.kiqa.omniservers.data.UiState
import com.kiqa.omniservers.data.formatUptime
import com.kiqa.omniservers.design.ButtonKind
import com.kiqa.omniservers.design.Icon
import com.kiqa.omniservers.design.LinkChip
import com.kiqa.omniservers.design.OmniButton
import com.kiqa.omniservers.design.OmniCard
import com.kiqa.omniservers.design.OmniColor
import com.kiqa.omniservers.design.OmniIcon
import com.kiqa.omniservers.design.OmniIconButton
import com.kiqa.omniservers.design.OmniText
import com.kiqa.omniservers.design.OmniType
import com.kiqa.omniservers.design.PowerButton
import com.kiqa.omniservers.design.StatusOrb
import com.kiqa.omniservers.design.Tag
import com.kiqa.omniservers.design.statusWord
import com.kiqa.omniservers.design.staggerIn
import kotlinx.coroutines.delay

@Composable
fun ServersScreen(
    state: UiState,
    onOpen: (ServerSummary) -> Unit,
    onToggle: (ServerSummary) -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                OmniText("OMNIServers", style = OmniType.caption, color = OmniColor.accentSoft)
                OmniText("Servers", style = OmniType.hero)
            }
            OmniIconButton(OmniIcon.Sliders, onSettings, label = "Settings")
        }
        Spacer(Modifier.height(10.dp))
        LinkChip(state.link, state.connectionLabel, state.demo, Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.link == LinkState.Offline || state.link == LinkState.Rejected) {
                item(key = "offline") { OfflineCard(state.link, onRetry, onSettings) }
            }
            itemsIndexed(state.servers, key = { _, s -> s.id }) { i, s ->
                ServerCard(s, onOpen = { onOpen(s) }, onToggle = { onToggle(s) }, modifier = Modifier.staggerIn(i))
            }
            if (state.loaded && state.servers.isEmpty() && state.link == LinkState.Online) {
                item(key = "empty") { EmptyCard() }
            }
        }
    }
}

@Composable
private fun ServerCard(server: ServerSummary, onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    OmniCard(modifier.fillMaxWidth(), onClick = onOpen, highlight = server.status == ServerStatus.Running) {
        Column(Modifier.padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusOrb(server.status, size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    OmniText(server.name, style = OmniType.headline, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    OmniText("${server.software} · ${server.ramLabel}", style = OmniType.caption, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    StatusLine(server)
                }
                Spacer(Modifier.width(10.dp))
                PowerButton(server.status, onClick = onToggle)
            }
            if (server.needsEula) {
                Spacer(Modifier.height(12.dp))
                Tag("Accept the EULA on your computer first", color = OmniColor.warnSoft, background = OmniColor.warnMuted)
            }
        }
    }
}

/** "Online · 2h 14m", ticking once a second only while the server is running. */
@Composable
private fun StatusLine(server: ServerSummary) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    val started = server.startedAt
    if (server.status == ServerStatus.Running && started != null) {
        LaunchedEffect(started) { while (true) { delay(1000); now = System.currentTimeMillis() / 1000 } }
    }
    val text = if (server.status == ServerStatus.Running && started != null)
        "${statusWord(server.status)} · ${formatUptime(now - started)}" else statusWord(server.status)
    val color = when (server.status) {
        ServerStatus.Running -> OmniColor.okSoft
        ServerStatus.Crashed -> OmniColor.bad
        ServerStatus.Starting, ServerStatus.Stopping -> OmniColor.warnSoft
        else -> OmniColor.inkFaint
    }
    OmniText(text, style = OmniType.label, color = color)
}

@Composable
private fun OfflineCard(link: LinkState, onRetry: () -> Unit, onSettings: () -> Unit) {
    OmniCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OmniText(
                if (link == LinkState.Rejected) "Your computer doesn't recognise this pairing" else "Can't reach your computer",
                style = OmniType.headline,
            )
            OmniText(
                if (link == LinkState.Rejected) "The pairing code was changed or switched off. Pair again from OMNIServers → Settings → Remote API."
                else "Check that OMNIServers is open on your computer, that “Allow other devices” is on, and that both are on the same Wi-Fi.",
                style = OmniType.body, color = OmniColor.inkDim,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OmniButton("Try again", onRetry, compact = true, icon = null)
                OmniButton("Connection", onSettings, kind = ButtonKind.Secondary, compact = true)
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    OmniCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(OmniIcon.Server, OmniColor.inkFaint, size = 32.dp)
            OmniText("No servers yet", style = OmniType.headline)
            OmniText("Create one in OMNIServers on your computer and it will show up here.", style = OmniType.body, color = OmniColor.inkDim)
        }
    }
}
