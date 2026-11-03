package com.kiqa.omniservers.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kiqa.omniservers.data.LinkState
import com.kiqa.omniservers.data.ServerStatus
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ───────────────────────────── Surfaces ─────────────────────────────

/** The standard rounded surface. Pass [onClick] to make the whole card a tap target. */
@Composable
fun OmniCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlight: Boolean = false,
    content: @Composable () -> Unit,
) {
    val border by animateColorAsState(if (highlight) OmniColor.accent.copy(alpha = 0.35f) else OmniColor.line, Motion.snappy(), label = "cardBorder")
    Box(
        modifier
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.985f, onClick = onClick) else Modifier)
            .clip(OmniShape.card)
            .background(OmniColor.surface)
            .border(1.dp, border, OmniShape.card),
    ) { content() }
}

// ───────────────────────────── Buttons ─────────────────────────────

enum class ButtonKind { Primary, Secondary, Quiet }

@Composable
fun OmniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    haptic: Boolean = true,
    compact: Boolean = false,
) {
    val (bg, fg, stroke) = if (!enabled && kind == ButtonKind.Primary) Triple(OmniColor.surface3, OmniColor.inkGhost, OmniColor.line) else when (kind) {
        ButtonKind.Primary -> Triple(OmniColor.accent, OmniColor.onAccent, Color.Transparent)
        ButtonKind.Secondary -> Triple(OmniColor.surface3, OmniColor.ink, OmniColor.line)
        ButtonKind.Quiet -> Triple(Color.Transparent, OmniColor.inkDim, Color.Transparent)
    }
    val alpha by animateFloatAsState(if (enabled || kind == ButtonKind.Primary) 1f else 0.4f, Motion.snappy(), label = "btnAlpha")
    Row(
        modifier
            .pressable(enabled = enabled, haptic = haptic, onClick = onClick)
            .height(if (compact) 40.dp else 52.dp)
            .clip(OmniShape.pill)
            .background(bg.copy(alpha = bg.alpha * alpha))
            .border(1.dp, stroke, OmniShape.pill)
            .padding(horizontal = if (compact) 18.dp else 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, fg.copy(alpha = alpha), size = 18.dp)
            Spacer(Modifier.width(10.dp))
        }
        OmniText(text, style = OmniType.label.copy(fontFamily = OmniType.headline.fontFamily), color = fg.copy(alpha = alpha))
    }
}

/** A round icon-only button (back, settings). 48dp tap target, 40dp visual. */
@Composable
fun OmniIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, label: String? = null) {
    Box(
        modifier
            .pressable(onClickLabel = label, haptic = false, onClick = onClick)
            .size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(OmniColor.surface2).border(1.dp, OmniColor.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, OmniColor.ink, size = 20.dp) }
    }
}

// ───────────────────────────── Status ─────────────────────────────

private fun statusColor(s: ServerStatus) = when (s) {
    ServerStatus.Running -> OmniColor.ok
    ServerStatus.Starting, ServerStatus.Stopping -> OmniColor.warn
    ServerStatus.Crashed -> OmniColor.bad
    ServerStatus.Stopped -> OmniColor.inkGhost
}

fun statusWord(s: ServerStatus) = when (s) {
    ServerStatus.Running -> "Online"
    ServerStatus.Starting -> "Starting…"
    ServerStatus.Stopping -> "Stopping…"
    ServerStatus.Crashed -> "Crashed"
    ServerStatus.Stopped -> "Off"
}

/**
 * The server's heartbeat: a solid core, a quiet ring, and a ring that expands outward and fades
 * while the server is on (faster while it starts). Off servers are just still.
 */
@Composable
fun StatusOrb(status: ServerStatus, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val color by animateColorAsState(statusColor(status), tween(320), label = "orbColor")
    val alive = status == ServerStatus.Running || status.isBusy
    val period = if (status.isBusy) 900 else 2600
    val transition = rememberInfiniteTransition(label = "orb")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(period, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "orbPulse",
    )
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(color.copy(alpha = 0.22f), radius = r * 0.74f, style = Stroke(width = r * 0.08f))
        drawCircle(color, radius = r * 0.36f)
        if (alive) {
            drawCircle(color.copy(alpha = (1f - t) * 0.5f), radius = r * (0.4f + 0.6f * t), style = Stroke(width = r * 0.07f))
        }
    }
}

/**
 * The big round start/stop control. Off → crimson play; on → quiet stop; busy → a spinning arc.
 * The icon swaps with a spring scale so the change feels physical, not instant.
 */
@Composable
fun PowerButton(status: ServerStatus, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val isOff = status == ServerStatus.Stopped || status == ServerStatus.Crashed
    val bg by animateColorAsState(if (isOff) OmniColor.accent else OmniColor.surface3, tween(240), label = "powerBg")
    val stroke by animateColorAsState(if (isOff) Color.Transparent else OmniColor.line, tween(240), label = "powerStroke")
    val label = if (isOff) "Start server" else if (status.isBusy) "Working" else "Stop server"
    Box(
        modifier
            .pressable(enabled = !status.isBusy, haptic = true, onClickLabel = label, onClick = onClick)
            .size(size).clip(CircleShape).background(bg).border(1.dp, stroke, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = if (isOff) 0 else if (status.isBusy) 1 else 2,
            transitionSpec = { (scaleIn(Motion.snappy(), 0.4f) + fadeIn(tween(140))) togetherWith (scaleOut(Motion.snappy(), 0.4f) + fadeOut(tween(100))) },
            label = "powerIcon",
        ) { kind ->
            when (kind) {
                0 -> Icon(OmniIcon.Play, OmniColor.onAccent, size = size * 0.42f)
                2 -> Icon(OmniIcon.Stop, OmniColor.ink, size = size * 0.38f)
                else -> Spinner(OmniColor.warn, size * 0.42f)
            }
        }
    }
}

@Composable
fun Spinner(color: Color, size: Dp, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "spinAngle")
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension * 0.14f
        drawArc(color, startAngle = t, sweepAngle = 250f, useCenter = false, style = Stroke(w, cap = StrokeCap.Round),
            topLeft = Offset(w / 2, w / 2), size = Size(this.size.width - w, this.size.height - w))
    }
}

/** Small pill with a status colour: "Needs EULA", "Java", etc. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, color: Color = OmniColor.inkDim, background: Color = OmniColor.surface3) {
    Box(modifier.clip(OmniShape.pill).background(background).padding(horizontal = 10.dp, vertical = 5.dp)) {
        OmniText(text, style = OmniType.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = color, maxLines = 1)
    }
}

/** How the app is linked to the desktop, in one glanceable pill. */
@Composable
fun LinkChip(link: LinkState, label: String, demo: Boolean, modifier: Modifier = Modifier) {
    val (color, text) = when {
        demo -> OmniColor.warn to "Demo mode · sample servers"
        link == LinkState.Online -> OmniColor.ok to "Connected · $label"
        link == LinkState.Connecting -> OmniColor.warn to "Connecting…"
        link == LinkState.Rejected -> OmniColor.bad to "Pairing code rejected"
        else -> OmniColor.bad to "Can't reach your computer"
    }
    val c by animateColorAsState(color, tween(300), label = "chipColor")
    Row(
        modifier.clip(OmniShape.pill).background(OmniColor.surface2).border(1.dp, OmniColor.line, OmniShape.pill)
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusOrb(if (link == LinkState.Online || demo) ServerStatus.Running else if (link == LinkState.Connecting) ServerStatus.Starting else ServerStatus.Crashed, size = 20.dp)
        Spacer(Modifier.width(6.dp))
        OmniText(text, style = OmniType.caption, color = OmniColor.inkDim, maxLines = 1)
    }
}

// ───────────────────────────── Controls ─────────────────────────────

/**
 * A sliding-thumb picker. The thumb is a spring, so tapping quickly between options redirects it
 * mid-flight instead of waiting; a touch of overshoot makes it feel like a physical switch.
 */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().height(48.dp).clip(OmniShape.pill).background(OmniColor.surface2).border(1.dp, OmniColor.line, OmniShape.pill).padding(4.dp)) {
        val itemWidth = maxWidth / options.size
        val x by animateDpAsState(itemWidth * selected, Motion.lively(), label = "thumb")
        Box(
            Modifier.offset { IntOffset(x.roundToPx(), 0) }
                .width(itemWidth).fillMaxHeight().clip(OmniShape.pill)
                .background(OmniColor.surface4).border(1.dp, OmniColor.lineStrong, OmniShape.pill),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, text ->
                val color by animateColorAsState(if (i == selected) OmniColor.ink else OmniColor.inkFaint, tween(180), label = "segText")
                Box(
                    Modifier.weight(1f).fillMaxHeight().pressable(pressedScale = 0.96f, role = Role.Tab, haptic = false) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { OmniText(text, style = OmniType.label, color = color, maxLines = 1) }
            }
        }
    }
}

@Composable
fun OmniField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) OmniColor.accent.copy(alpha = 0.7f) else OmniColor.line, tween(160), label = "fieldBorder")
    val style = (if (mono) OmniType.code.copy(fontSize = OmniType.body.fontSize, color = OmniColor.ink) else OmniType.body)
    Row(
        modifier.defaultMinSize(minHeight = 52.dp).clip(OmniShape.md).background(OmniColor.surface2).border(BorderStroke(1.dp, border), OmniShape.md)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
            if (value.isEmpty()) OmniText(placeholder, style = style, color = OmniColor.inkGhost, maxLines = 1)
            BasicTextField(
                value = value, onValueChange = onValueChange, singleLine = singleLine, textStyle = style,
                cursorBrush = SolidColor(OmniColor.accentSoft),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            )
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
    }
}


// ───────────────────────────── Feedback ─────────────────────────────

/** A message that drops in from the top, stays a moment, and leaves. Pass null to hide. */
@Composable
fun OmniToast(message: String?, onDone: () -> Unit, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            shown = message
            delay(3200)
            onDone()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(Motion.snappy()) { -it } + fadeIn(tween(160)),
        exit = slideOutVertically(Motion.snappy()) { -it } + fadeOut(tween(140)),
        modifier = modifier,
    ) {
        Row(
            Modifier.clip(OmniShape.pill).background(OmniColor.surface4).border(1.dp, OmniColor.lineStrong, OmniShape.pill)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(OmniIcon.Close, OmniColor.warn, size = 16.dp)
            Spacer(Modifier.width(10.dp))
            OmniText(shown.orEmpty(), style = OmniType.label, maxLines = 2)
        }
    }
}

/** Deterministic, calm colour for a player's letter avatar. */
fun avatarColor(name: String): Color {
    val hue = (name.fold(7) { acc, c -> (acc * 31 + c.code) and 0x7fffffff } % 360).toFloat()
    return Color.hsv(hue, 0.38f, 0.62f)
}

/** The OMNI mark: a crimson ring around a gold core, breathing slowly. Used where nothing is "online" yet. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val t by rememberInfiniteTransition(label = "brand").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3200, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = "brandPulse",
    )
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(OmniColor.accent.copy(alpha = 0.14f), radius = r * 0.96f, style = Stroke(r * 0.03f))
        drawCircle(OmniColor.accent.copy(alpha = (1f - t) * 0.5f), radius = r * (0.62f + 0.34f * t), style = Stroke(r * 0.04f))
        drawCircle(OmniColor.accent, radius = r * 0.56f, style = Stroke(r * 0.15f))
        drawCircle(Color(0xFFE8B44F), radius = r * 0.2f)
    }
}
