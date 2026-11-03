package com.kiqa.omniservers

import com.kiqa.omniservers.data.formatUptime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun seconds() { assertEquals("0s", formatUptime(0)); assertEquals("59s", formatUptime(59)) }
    @Test fun minutes() { assertEquals("1m 00s", formatUptime(60)); assertEquals("14m 05s", formatUptime(14 * 60 + 5)) }
    @Test fun hours() { assertEquals("1h 0m", formatUptime(3600)); assertEquals("2h 14m", formatUptime(2 * 3600 + 14 * 60 + 59)) }
    @Test fun days() { assertEquals("3d 4h", formatUptime(3 * 86_400 + 4 * 3600 + 59)) }
    @Test fun negativeIsClamped() { assertEquals("0s", formatUptime(-5)) }
}
