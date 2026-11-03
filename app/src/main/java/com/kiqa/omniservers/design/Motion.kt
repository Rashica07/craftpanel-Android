package com.kiqa.omniservers.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Motion rules, taken from Apple's fluid-interface talks:
 *  - everything that moves is a spring, so it can be grabbed or redirected mid-flight;
 *  - critically damped (no bounce) by default; bounce only after a gesture that carried momentum;
 *  - feedback starts the instant a finger lands, not when it lifts.
 */
object Motion {
    /** Quick, settles without overshoot. Taps, toggles, small shifts. */
    fun <T> snappy() = spring<T>(dampingRatio = 1f, stiffness = 560f)

    /** Calmer, for whole screens and large surfaces. */
    fun <T> smooth() = spring<T>(dampingRatio = 1f, stiffness = 280f)

    /** A little overshoot. Only for things that were thrown or that should feel alive (the segmented thumb). */
    fun <T> lively() = spring<T>(dampingRatio = 0.82f, stiffness = 420f)

    const val PressedScale = 0.965f
}

/**
 * Tap target with instant press feedback: it shrinks the moment a finger lands and springs back,
 * and tapping fires [onClick] (with a light tick when [haptic] is set). Exposed as a button to TalkBack.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = Motion.PressedScale,
    haptic: Boolean = false,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(enabled) {
            if (enabled) detectTapGestures(
                onPress = {
                    pressed = true
                    try { tryAwaitRelease() } finally { pressed = false }
                },
                onTap = {
                    if (haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
        }
        .semantics(mergeDescendants = true) {
            this.role = role
            if (enabled) onClick(label = onClickLabel) { onClick(); true }
        }
}

/** Fades and lifts an item in shortly after it first appears; later items wait a little longer. */
@androidx.compose.runtime.Composable
fun Modifier.staggerIn(index: Int): Modifier {
    var shown by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index.coerceAtMost(8) * 50L)
        shown = true
    }
    val a by animateFloatAsState(if (shown) 1f else 0f, Motion.smooth(), label = "stagger")
    return graphicsLayer {
        alpha = a
        translationY = (1f - a) * 28.dp.toPx()
    }
}
