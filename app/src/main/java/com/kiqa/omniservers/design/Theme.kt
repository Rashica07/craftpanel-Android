package com.kiqa.omniservers.design

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiqa.omniservers.R

/**
 * OMNI's look, same tokens as the desktop apps: warm near-black, ivory ink, Imperial Crimson
 * for the one primary action, Solar Gold as the rare highlight. Dark only, on purpose.
 */
object OmniColor {
    val bg = Color(0xFF0F0E0D)
    val surface = Color(0xFF171513)
    val surface2 = Color(0xFF1C1A17)
    val surface3 = Color(0xFF221F1C)
    val surface4 = Color(0xFF2B2824)
    val console = Color(0xFF0B0A09)

    val lineSoft = Color(0xFF24211E)
    val line = Color(0xFF34302B)
    val lineStrong = Color(0xFF484239)

    val ink = Color(0xFFE8DCC8)
    val inkDim = Color(0xFFB9AF9F)
    val inkFaint = Color(0xFF948B7C)
    val inkGhost = Color(0xFF6F685C)

    val accent = Color(0xFFC83246)
    val accentPress = Color(0xFFA8283A)
    val accentSoft = Color(0xFFF0707F)
    val accentMuted = Color(0xFF2A1216)
    val onAccent = Color.White

    val ok = Color(0xFF5FBF7F)
    val okSoft = Color(0xFF8FD6A5)
    val okMuted = Color(0xFF12241A)
    val warn = Color(0xFFE8B44F)
    val warnSoft = Color(0xFFF0CB85)
    val warnMuted = Color(0xFF2A2010)
    val bad = Color(0xFFF0707F)
    val badMuted = Color(0xFF2B1214)
}

/** Corner radii. Cards are 24, buttons are pills: the whole app leans round. */
object OmniShape {
    val xs = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val sm = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val md = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    val card = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val pill = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun spaceGrotesk(weight: Int) = Font(
    R.font.spacegrotesk,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Space Grotesk (the desktop apps' display face) for headings and numbers; the system face for reading. */
object OmniType {
    private val display = FontFamily(spaceGrotesk(400), spaceGrotesk(500), spaceGrotesk(600), spaceGrotesk(700))
    val mono = FontFamily.Monospace

    val hero = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp, color = OmniColor.ink)
    val title = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp, color = OmniColor.ink)
    val headline = TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 23.sp, letterSpacing = (-0.2).sp, color = OmniColor.ink)
    val number = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-1).sp, color = OmniColor.ink)
    val body = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, color = OmniColor.ink)
    val label = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp, color = OmniColor.ink)
    val caption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.1.sp, color = OmniColor.inkFaint)
    val code = TextStyle(fontFamily = mono, fontSize = 12.5.sp, lineHeight = 18.sp, color = OmniColor.inkDim)
}

@Composable
fun OmniText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = OmniType.body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (color == Color.Unspecified) style else style.copy(color = color),
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}
