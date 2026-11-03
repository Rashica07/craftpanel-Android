package com.kiqa.omniservers.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kiqa.omniservers.data.ConsoleLine
import com.kiqa.omniservers.data.LogLevel
import com.kiqa.omniservers.data.PlayerList
import com.kiqa.omniservers.data.ServerStatus
import com.kiqa.omniservers.data.ServerSummary
import com.kiqa.omniservers.data.formatUptime
import com.kiqa.omniservers.design.ButtonKind
import com.kiqa.omniservers.design.Icon
import com.kiqa.omniservers.design.Motion
import com.kiqa.omniservers.design.OmniButton
import com.kiqa.omniservers.design.OmniCard
import com.kiqa.omniservers.design.OmniColor
import com.kiqa.omniservers.design.OmniField
import com.kiqa.omniservers.design.OmniIcon
import com.kiqa.omniservers.design.OmniIconButton
import com.kiqa.omniservers.design.OmniShape
import com.kiqa.omniservers.design.OmniText
import com.kiqa.omniservers.design.OmniType
import com.kiqa.omniservers.design.PowerButton
import com.kiqa.omniservers.design.Segmented
import com.kiqa.omniservers.design.Tag
import com.kiqa.omniservers.design.avatarColor
import com.kiqa.omniservers.design.pressable
import com.kiqa.omniservers.design.staggerIn
import com.kiqa.omniservers.design.statusWord
import kotlinx.coroutines.delay

private val Tabs = listOf("Overview", "Console", "Players")

@Composable
fun DetailScreen(
    server: ServerSummary?,
    console: List<ConsoleLine>,
    players: PlayerList,
    onBack: () -> Unit,
    onToggle: (ServerSummary) -> Unit,
    onSend: (String) -> Unit,
    onToast: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OmniIconButton(OmniIcon.Back, onBack, label = "Back")
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                OmniText(server?.name ?: "Server", style = OmniType.title, maxLines = 1)
                if (server != null) HeaderStatus(server)
            }
            if (server != null) PowerButton(server.status, { onToggle(server) }, size = 52.dp)
        }
        Spacer(Modifier.height(16.dp))
        Segmented(Tabs, tab, { tab = it }, Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(14.dp))

        if (server == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
                OmniText("This server isn't on your computer any more.", style = OmniType.body, color = OmniColor.inkDim)
            }
        } else {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(Motion.smooth()) { dir * it / 5 } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(Motion.smooth()) { -dir * it / 5 } + fadeOut(tween(120)))
                },
                label = "tab", modifier = Modifier.weight(1f),
            ) { t ->
                when (t) {
                    0 -> OverviewTab(server, players)
                    1 -> ConsoleTab(server, console, onSend, onToast)
                    else -> PlayersTab(server, players, onSend, onToast)
                }
            }
        }
    }
}

@Composable
private fun HeaderStatus(server: ServerSummary) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    val started = server.startedAt
    if (server.status == ServerStatus.Running && started != null) {
        LaunchedEffect(started) { while (true) { delay(1000); now = System.currentTimeMillis() / 1000 } }
    }
    val text = if (server.status == ServerStatus.Running && started != null) "Online · ${formatUptime(now - started)}" else statusWord(server.status)
    OmniText(text, style = OmniType.caption, color = if (server.status == ServerStatus.Running) OmniColor.okSoft else OmniColor.inkFaint)
}

// ───────────────────────────── Overview ─────────────────────────────

@Composable
private fun OverviewTab(server: ServerSummary, players: PlayerList) {
    val running = server.status == ServerStatus.Running
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (server.needsEula) {
            OmniCard(Modifier.fillMaxWidth(), highlight = false) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OmniText("One thing before the first start", style = OmniType.headline, color = OmniColor.warnSoft)
                    OmniText("Accept the Minecraft EULA in OMNIServers on your computer, then start it from here.", style = OmniType.body, color = OmniColor.inkDim)
                }
            }
        }
        PlayerRing(online = if (running) players.online else 0, max = players.max, running = running)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(OmniIcon.Cpu, "Memory", server.ramLabel, Modifier.weight(1f))
            Stat(OmniIcon.Box, "Software", server.software, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val up = server.startedAt?.takeIf { running }?.let { formatUptime(System.currentTimeMillis() / 1000 - it) } ?: "Off"
            Stat(OmniIcon.Clock, "Uptime", up, Modifier.weight(1f))
            Stat(OmniIcon.Users, "Capacity", if (players.max > 0) "${players.max} players" else "—", Modifier.weight(1f))
        }
    }
}

/** The one rich moment on this screen: a ring that fills as players join. */
@Composable
private fun PlayerRing(online: Int, max: Int, running: Boolean) {
    val target = if (max > 0) (online.toFloat() / max).coerceIn(0f, 1f) else 0f
    val sweep by animateFloatAsState(target, Motion.smooth(), label = "ring")
    OmniCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(188.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 16.dp.toPx()
                    val tl = Offset(w / 2, w / 2); val sz = Size(size.width - w, size.height - w)
                    drawArc(OmniColor.surface3, 0f, 360f, false, tl, sz, style = Stroke(w))
                    if (running) {
                        // A minimum sliver so "1 of 20" is still visible as a ring, not a dot.
                        val deg = if (online > 0) maxOf(sweep * 360f, 14f) else 0f
                        drawArc(OmniColor.accent, -90f, deg, false, tl, sz, style = Stroke(w, cap = StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OmniText(if (running) "$online" else "—", style = OmniType.number)
                    OmniText(
                        if (running) (if (max > 0) "of $max players" else "players") else "server is off",
                        style = OmniType.caption,
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    OmniCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, OmniColor.inkFaint, size = 20.dp)
            Column {
                OmniText(label, style = OmniType.caption)
                OmniText(value, style = OmniType.headline, maxLines = 1)
            }
        }
    }
}

// ───────────────────────────── Console ─────────────────────────────

private val QuickCommands = listOf("list", "save-all", "weather clear", "time set day")

@Composable
private fun ConsoleTab(server: ServerSummary, lines: List<ConsoleLine>, onSend: (String) -> Unit, onToast: (String) -> Unit) {
    val running = server.status == ServerStatus.Running
    val listState = rememberLazyListState()
    val atBottom by remember { derivedStateOf { !listState.canScrollForward } }
    var input by rememberSaveable { mutableStateOf("") }

    // Follow new output only if the reader is already at the end; never yank them away from older lines.
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty() && !listState.canScrollForward) listState.scrollToItem(lines.lastIndex)
    }
    // First time the tab opens, start at the end.
    LaunchedEffect(Unit) { if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex) }

    fun send(text: String) {
        if (!running) { onToast("Start the server first"); return }
        onSend(text)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(OmniShape.md).background(OmniColor.console).border(1.dp, OmniColor.lineSoft, OmniShape.md)) {
            if (lines.isEmpty()) {
                OmniText("Nothing printed yet.", style = OmniType.code, modifier = Modifier.padding(16.dp))
            }
            LazyColumn(state = listState, contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                itemsIndexed(lines) { _, l ->
                    OmniText(
                        l.text, style = OmniType.code,
                        color = when (l.level) {
                            LogLevel.Error -> OmniColor.bad
                            LogLevel.Warn -> OmniColor.warnSoft
                            LogLevel.Command -> OmniColor.accentSoft
                            LogLevel.Info -> OmniColor.inkDim
                        },
                    )
                }
            }
            JumpButton(!atBottom && lines.isNotEmpty(), listState, lines.lastIndex, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(QuickCommands) { _, c ->
                Box(Modifier.pressable(haptic = true) { send(c) }.clip(OmniShape.pill).background(OmniColor.surface2).border(1.dp, OmniColor.line, OmniShape.pill).padding(horizontal = 14.dp, vertical = 8.dp)) {
                    OmniText(c, style = OmniType.code.copy(color = OmniColor.inkDim))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OmniField(
            input, { input = it }, if (running) "Type a command" else "Start the server to send commands", mono = true,
            imeAction = ImeAction.Send, onImeAction = { if (input.isNotBlank()) { send(input); input = "" } },
            trailing = {
                Box(
                    Modifier.pressable(haptic = true) { if (input.isNotBlank()) { send(input); input = "" } }.size(40.dp).clip(CircleShape)
                        .background(if (input.isNotBlank() && running) OmniColor.accent else OmniColor.surface3),
                    contentAlignment = Alignment.Center,
                ) { Icon(OmniIcon.Send, Color.White, size = 18.dp) }
            },
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun JumpButton(visible: Boolean, state: androidx.compose.foundation.lazy.LazyListState, last: Int, modifier: Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(Motion.snappy()) { it },
        exit = fadeOut() + slideOutVertically(Motion.snappy()) { it },
    ) { JumpToLatest(state, last) }
}

@Composable
private fun JumpToLatest(state: androidx.compose.foundation.lazy.LazyListState, last: Int) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(go) { if (go) { state.animateScrollToItem(last.coerceAtLeast(0)); go = false } }
    Row(
        Modifier.pressable { go = true }.clip(OmniShape.pill).background(OmniColor.surface4)
            .border(1.dp, OmniColor.lineStrong, OmniShape.pill).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(OmniIcon.Down, OmniColor.ink, size = 16.dp)
        Spacer(Modifier.width(6.dp))
        OmniText("Latest", style = OmniType.label)
    }
}

// ───────────────────────────── Players ─────────────────────────────

@Composable
private fun PlayersTab(server: ServerSummary, players: PlayerList, onSend: (String) -> Unit, onToast: (String) -> Unit) {
    val running = server.status == ServerStatus.Running
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmOp by rememberSaveable { mutableStateOf<String?>(null) }

    if (!running || players.players.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            OmniCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(OmniIcon.Users, OmniColor.inkFaint, size = 32.dp)
                    OmniText(if (running) "Nobody online" else "Server is off", style = OmniType.headline)
                    OmniText(
                        if (running) "Players show up here the moment they join." else "Start the server to see who's playing.",
                        style = OmniType.body.copy(textAlign = TextAlign.Center), color = OmniColor.inkDim,
                    )
                }
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(players.players, key = { _, n -> n }) { i, name ->
            val expanded = open == name
            OmniCard(Modifier.fillMaxWidth().staggerIn(i), onClick = { open = if (expanded) null else name; confirmOp = null }) {
                Column(Modifier.animateContentSize(Motion.snappy()).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(avatarColor(name)), contentAlignment = Alignment.Center) {
                            OmniText(name.first().uppercase(), style = OmniType.headline, color = Color(0xFF0F0E0D))
                        }
                        Spacer(Modifier.width(14.dp))
                        OmniText(name, style = OmniType.headline, maxLines = 1, modifier = Modifier.weight(1f))
                    }
                    if (expanded) {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OmniButton(
                                if (confirmOp == name) "Tap again to make op" else "Make op",
                                onClick = {
                                    if (confirmOp == name) { onSend("op $name"); confirmOp = null; onToast("$name is now an operator") } else confirmOp = name
                                },
                                kind = ButtonKind.Secondary, compact = true,
                            )
                            OmniButton("Kick", onClick = { onSend("kick $name"); open = null }, kind = ButtonKind.Quiet, compact = true)
                        }
                    }
                }
            }
        }
    }
}
