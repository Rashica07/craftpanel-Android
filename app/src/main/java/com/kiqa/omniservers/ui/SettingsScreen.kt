package com.kiqa.omniservers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kiqa.omniservers.data.UiState
import com.kiqa.omniservers.design.ButtonKind
import com.kiqa.omniservers.design.LinkChip
import com.kiqa.omniservers.design.OmniButton
import com.kiqa.omniservers.design.OmniCard
import com.kiqa.omniservers.design.OmniColor
import com.kiqa.omniservers.design.OmniIcon
import com.kiqa.omniservers.design.OmniIconButton
import com.kiqa.omniservers.design.OmniText
import com.kiqa.omniservers.design.OmniType
import com.kiqa.omniservers.design.staggerIn

@Composable
fun SettingsScreen(state: UiState, version: String, onBack: () -> Unit, onDisconnect: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OmniIconButton(OmniIcon.Back, onBack, label = "Back")
            Spacer(Modifier.width(8.dp))
            OmniText("Settings", style = OmniType.title)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OmniCard(Modifier.fillMaxWidth().staggerIn(0)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OmniText("Connection", style = OmniType.headline)
                    LinkChip(state.link, state.connectionLabel, state.demo)
                    OmniText(
                        if (state.demo) "You're looking at made-up servers. Disconnect to pair with your own computer."
                        else "Disconnecting forgets this pairing. You can pair again with the code from OMNIServers on your computer.",
                        style = OmniType.body, color = OmniColor.inkDim,
                    )
                    OmniButton(if (state.demo) "Leave demo" else "Disconnect", onDisconnect, kind = ButtonKind.Secondary, compact = true)
                }
            }
            OmniCard(Modifier.fillMaxWidth().staggerIn(1)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OmniText("About", style = OmniType.headline)
                    OmniText("OMNIServers for Android $version", style = OmniType.body, color = OmniColor.inkDim)
                    OmniText("Free and open source (GPL-3.0). Remote control for OMNIServers on Mac for now; Windows and Linux support comes with their Remote API.", style = OmniType.caption)
                }
            }
        }
    }
}
