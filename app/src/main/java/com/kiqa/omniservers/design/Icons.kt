package com.kiqa.omniservers.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A small set of line icons (Lucide-style, 24px grid, 2px round stroke) so everything shares one voice. */
object OmniIcon {
    private fun line(name: String, vararg d: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        d.forEach {
            addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

    private fun solid(name: String, d: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black))
    }.build()

    val Play = solid("play", "M8 5.2v13.6a1 1 0 0 0 1.52.85l11-6.8a1 1 0 0 0 0-1.7l-11-6.8A1 1 0 0 0 8 5.2z")
    val Stop = solid("stop", "M8 5h8a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z")
    val Server = line("server",
        "M5 3h14a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        "M5 13h14a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z", "M7 7h.01", "M7 17h.01")
    val Terminal = line("terminal", "M4 17l6-6-6-6", "M12 19h8")
    val Users = line("users",
        "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
        "M22 21v-2a4 4 0 0 0-3-3.87", "M16 3.13a4 4 0 0 1 0 7.75")
    val Sliders = line("sliders", "M4 21v-7", "M4 10V3", "M12 21v-9", "M12 8V3", "M20 21v-5", "M20 12V3", "M1 14h6", "M9 8h6", "M17 16h6")
    val Back = line("back", "M15 18l-6-6 6-6")
    val Chevron = line("chevron", "M9 18l6-6-6-6")
    val Plus = line("plus", "M12 5v14", "M5 12h14")
    val Send = line("send", "M22 2L11 13", "M22 2l-7 20-4-9-9-4 20-7z")
    val Down = line("down", "M12 5v14", "M19 12l-7 7-7-7")
    val Close = line("close", "M18 6L6 18", "M6 6l12 12")
    val Check = line("check", "M20 6L9 17l-5-5")
    val Cpu = line("cpu", "M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M9 9h6v6H9z", "M9 1v3", "M15 1v3", "M9 20v3", "M15 20v3", "M20 9h3", "M20 14h3", "M1 9h3", "M1 14h3")
    val Clock = line("clock", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z", "M12 7v5l3 2")
    val Box = line("box", "M21 8l-9-5-9 5v8l9 5 9-5z", "M3.3 7L12 12l8.7-5", "M12 22V12")
    val Wifi = line("wifi", "M5 12.55a11 11 0 0 1 14.08 0", "M1.42 9a16 16 0 0 1 21.16 0", "M8.53 16.11a6 6 0 0 1 6.95 0", "M12 20h.01")
    val Link = line("link", "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71",
        "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71")
    val Power = line("power", "M18.36 6.64a9 9 0 1 1-12.73 0", "M12 2v10")
    val Heart = line("heart", "M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z")
}

@Composable
fun Icon(vector: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Image(
        imageVector = vector,
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
