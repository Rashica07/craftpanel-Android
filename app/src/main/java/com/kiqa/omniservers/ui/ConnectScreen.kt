package com.kiqa.omniservers.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiqa.omniservers.data.Pairing
import com.kiqa.omniservers.design.BrandMark
import com.kiqa.omniservers.design.ButtonKind
import com.kiqa.omniservers.design.Icon
import com.kiqa.omniservers.design.Motion
import com.kiqa.omniservers.design.OmniButton
import com.kiqa.omniservers.design.OmniColor
import com.kiqa.omniservers.design.OmniField
import com.kiqa.omniservers.design.OmniIcon
import com.kiqa.omniservers.design.OmniText
import com.kiqa.omniservers.design.OmniType
import com.kiqa.omniservers.design.pressable
import com.kiqa.omniservers.design.staggerIn

/**
 * First run: pair with a desktop. The quick way is to paste the pairing code the desktop app shows;
 * typing host, port and token by hand is tucked behind "Enter details".
 */
@Composable
fun ConnectScreen(onConnect: (Pairing) -> Unit, onDemo: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var manual by rememberSaveable { mutableStateOf(false) }
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("8642") }
    var token by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val parsed = if (manual) {
        port.toIntOrNull()?.takeIf { it in 1..65535 && host.isNotBlank() && token.isNotBlank() }
            ?.let { Pairing(host.trim(), it, token.trim()) }
    } else Pairing.parse(code)
    val codeLooksWrong = !manual && code.isNotBlank() && parsed == null

    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
    val room = maxHeight
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height((room * 0.12f).coerceAtLeast(16.dp)))
        BrandMark(size = 120.dp, modifier = Modifier.staggerIn(0))
        Spacer(Modifier.height(24.dp))
        OmniText("Pair with your computer", style = OmniType.hero.copy(fontSize = 30.sp, textAlign = TextAlign.Center), modifier = Modifier.staggerIn(1).fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OmniText(
            "On your computer open OMNIServers → Settings → Remote API, switch on “Allow other devices”, then paste the pairing code here.",
            style = OmniType.body.copy(textAlign = TextAlign.Center), color = OmniColor.inkDim, modifier = Modifier.staggerIn(2).fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))

        Column(Modifier.fillMaxWidth().staggerIn(3).animateContentSize(Motion.smooth()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!manual) {
                OmniField(
                    code,
                    {
                        if (Pairing.isBareToken(it)) {
                            // Only the token was pasted: switch to the manual form with it filled in.
                            token = it.trim(); if (host.isBlank()) host = defaultHost(); manual = true; code = ""
                        } else code = it
                    },
                    "Pairing code", mono = true,
                    trailing = {
                        OmniText(
                            "Paste", style = OmniType.label, color = OmniColor.accentSoft,
                            modifier = Modifier.pressable {
                                clipboard.getText()?.text?.let {
                                    if (Pairing.isBareToken(it)) { token = it.trim(); if (host.isBlank()) host = defaultHost(); manual = true } else code = it
                                }
                            }.padding(8.dp),
                        )
                    },
                )
                AnimatedVisibility(codeLooksWrong, enter = fadeIn(), exit = fadeOut()) {
                    OmniText("That doesn't look like a pairing code. It should start with { and contain host, port and token.", style = OmniType.caption, color = OmniColor.bad)
                }
            } else {
                OmniField(host, { host = it }, "Computer address (e.g. 192.168.1.20)", keyboardType = KeyboardType.Uri)
                OmniField(port, { port = it.filter(Char::isDigit).take(5) }, "Port", keyboardType = KeyboardType.Number)
                OmniField(token, { token = it }, "Token (starts with cp_)", mono = true, imeAction = ImeAction.Done)
            }
            Row(
                Modifier.align(Alignment.CenterHorizontally).pressable(haptic = false) { manual = !manual }.padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OmniText(if (manual) "Use a pairing code instead" else "Enter details by hand", style = OmniType.label, color = OmniColor.inkDim)
                Spacer(Modifier.width(6.dp))
                Icon(OmniIcon.Chevron, OmniColor.inkFaint, size = 16.dp)
            }
        }

        Spacer(Modifier.height(8.dp))
        OmniButton("Connect", onClick = { parsed?.let(onConnect) }, enabled = parsed != null, icon = OmniIcon.Link,
            modifier = Modifier.fillMaxWidth().staggerIn(4))
        Spacer(Modifier.height(8.dp))
        OmniButton("Try the demo", onClick = onDemo, kind = ButtonKind.Quiet, modifier = Modifier.fillMaxWidth().staggerIn(5))

        Spacer(Modifier.height(32.dp))
        OmniText(
            "Use this on your home Wi-Fi. The link to your computer is not encrypted, so avoid public networks.",
            style = OmniType.caption.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
    }
    }
}

/** The emulator reaches the computer it runs on at 10.0.2.2; real phones need the computer's own address. */
private fun defaultHost(): String =
    if (android.os.Build.HARDWARE.contains("ranchu") || android.os.Build.HARDWARE.contains("goldfish")) "10.0.2.2" else ""
