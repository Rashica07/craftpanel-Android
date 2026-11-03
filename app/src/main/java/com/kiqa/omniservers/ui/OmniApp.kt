package com.kiqa.omniservers.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiqa.omniservers.data.AppViewModel
import com.kiqa.omniservers.data.Screen
import com.kiqa.omniservers.design.Motion
import com.kiqa.omniservers.design.OmniColor
import com.kiqa.omniservers.design.OmniToast

private fun depth(s: Screen) = when (s) {
    Screen.Connect -> 0
    Screen.Servers -> 1
    is Screen.Detail, Screen.Settings -> 2
}

/** Which "page" a screen is: moving between pages animates, updates within a page don't. */
private fun pageKey(s: Screen): Any = if (s is Screen.Detail) s.serverId else s

@Composable
fun OmniApp(vm: AppViewModel, version: String) {
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.screen is Screen.Detail || state.screen is Screen.Settings) { vm.back() }

    Box(Modifier.fillMaxSize().background(OmniColor.bg)) {
        AnimatedContent(
            targetState = state.screen,
            contentKey = { pageKey(it) },
            transitionSpec = { transition(initialState, targetState) },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Connect -> ConnectScreen(onConnect = { vm.connect(it) }, onDemo = { vm.connectDemo() })
                Screen.Servers -> ServersScreen(
                    state, onOpen = { vm.open(it.id) }, onToggle = vm::toggle,
                    onSettings = { vm.show(Screen.Settings) }, onRetry = vm::retryNow,
                )
                is Screen.Detail -> DetailScreen(
                    server = state.servers.firstOrNull { it.id == screen.serverId },
                    console = state.console, players = state.players,
                    onBack = { vm.back() }, onToggle = vm::toggle,
                    onSend = { vm.sendCommand(screen.serverId, it) }, onToast = vm::toast,
                )
                Screen.Settings -> SettingsScreen(state, version, onBack = { vm.back() }, onDisconnect = vm::disconnect)
            }
        }
        OmniToast(
            message = state.toast, onDone = vm::toastShown,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
        )
    }
}

private fun transition(from: Screen, to: Screen): ContentTransform {
    val forward = depth(to) > depth(from)
    if (depth(to) == depth(from)) return fadeIn(tween(180)) togetherWith fadeOut(tween(120))
    return if (forward) {
        (slideInHorizontally(Motion.smooth()) { it } + fadeIn(tween(220))) togetherWith
            (slideOutHorizontally(Motion.smooth()) { -it / 4 } + fadeOut(tween(160)))
    } else {
        (slideInHorizontally(Motion.smooth()) { -it / 4 } + fadeIn(tween(220))) togetherWith
            (slideOutHorizontally(Motion.smooth()) { it } + fadeOut(tween(160)))
    }
}
