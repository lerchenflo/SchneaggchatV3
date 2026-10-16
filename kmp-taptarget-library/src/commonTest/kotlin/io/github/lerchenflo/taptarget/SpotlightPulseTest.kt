package io.github.lerchenflo.taptarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpotlightPulseTest {

    @Test
    fun the_default_scrim_lets_the_app_shine_through() {
        assertTrue(DefaultTourBackgroundColor.alpha <= 0.6f)
    }

    @Test
    fun the_spotlight_breathes_between_rest_and_full_pulse() {
        val rest = spotlightPulse(pulse = 0f, maxInflatePx = 8f)
        val full = spotlightPulse(pulse = 1f, maxInflatePx = 8f)

        assertEquals(0f, rest.inflatePx)
        assertEquals(8f, full.inflatePx)
        assertTrue(full.fillAlpha > rest.fillAlpha)
        assertTrue(rest.outlineAlpha >= 0.6f, "the outline stays clearly visible at rest")
        assertEquals(1f, full.outlineAlpha)
    }

    @Test
    fun out_of_range_progress_is_clamped() {
        assertEquals(spotlightPulse(1f, 8f), spotlightPulse(1.4f, 8f))
        assertEquals(spotlightPulse(0f, 8f), spotlightPulse(-0.2f, 8f))
    }
}
