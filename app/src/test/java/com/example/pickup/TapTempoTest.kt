package com.example.pickup

import com.example.pickup.viewmodel.tempoFromTaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests the tap-tempo maths with made-up tap times in milliseconds.
class TapTempoTest {
    @Test fun oneTap_givesNoTempo() = assertNull(tempoFromTaps(listOf(1000L)))

    @Test fun tapsHalfASecondApart_give120() =
        assertEquals(120, tempoFromTaps(listOf(0L, 500L, 1000L, 1500L)))

    @Test fun unevenTaps_areAveraged() =
        // gaps 480, 520, 500 -> average 500 ms -> 120 BPM
        assertEquals(120, tempoFromTaps(listOf(0L, 480L, 1000L, 1500L)))

    @Test fun veryFastTaps_areCappedAt240() =
        assertEquals(240, tempoFromTaps(listOf(0L, 100L, 200L)))

    @Test fun verySlowTaps_areLimitedTo40() =
        assertEquals(40, tempoFromTaps(listOf(0L, 1900L)))
}
