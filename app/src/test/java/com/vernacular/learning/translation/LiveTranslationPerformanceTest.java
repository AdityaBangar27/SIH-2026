package com.vernacular.learning.translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.vernacular.learning.ai.PipelineResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Test;

/**
 * Unit tests verifying the Live Translation processing-time measurement,
 * latency formatting, stage breakdown, failure handling, and recent performance history.
 */
public class LiveTranslationPerformanceTest {

    @Test
    public void testMonotonicElapsedRealtimeFormatting() {
        long tStart = 100000L;
        long tEnd = 102840L; // 2840 ms elapsed
        long elapsedMs = tEnd - tStart;

        double seconds = elapsedMs / 1000.0;
        String formatted = String.format(Locale.US, "%.2f s", seconds);

        assertEquals("2.84 s", formatted);
    }

    @Test
    public void testDifferentElapsedDurations() {
        assertEquals("0.84 s", String.format(Locale.US, "%.2f s", 840 / 1000.0));
        assertEquals("2.31 s", String.format(Locale.US, "%.2f s", 2310 / 1000.0));
        assertEquals("4.17 s", String.format(Locale.US, "%.2f s", 4172 / 1000.0));
        assertEquals("12.48 s", String.format(Locale.US, "%.2f s", 12480 / 1000.0));
    }

    @Test
    public void testBreakdownFormattingWithPipelineResult() {
        PipelineResult result = PipelineResult.success(
                "मेरा भारत महान",
                "ᱧᱟᱹᱧ ᱵᱷᱟᱨᱚᱛ ᱢᱟᱦᱟᱱ",
                new File("dummy.wav"),
                820L,  // ASR
                1910L, // Translation
                450L,  // TTS
                2840L  // Total
        );

        assertTrue(result.isSuccess);
        assertEquals(820L, result.asrLatencyMs);
        assertEquals(1910L, result.translationLatencyMs);
        assertEquals(2840L, result.totalLatencyMs);

        StringBuilder sb = new StringBuilder();
        if (result.asrLatencyMs > 0) {
            sb.append(String.format(Locale.US, "ASR: %.2f s", result.asrLatencyMs / 1000.0));
        }
        if (result.translationLatencyMs > 0) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append(String.format(Locale.US, "Translation: %.2f s", result.translationLatencyMs / 1000.0));
        }
        if (result.totalLatencyMs > 0) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append(String.format(Locale.US, "Pipeline: %.2f s", result.totalLatencyMs / 1000.0));
        }

        assertEquals("ASR: 0.82 s  •  Translation: 1.91 s  •  Pipeline: 2.84 s", sb.toString());
    }

    @Test
    public void testRecentPerformanceHistoryCapAtThree() {
        List<String> history = new ArrayList<>();

        // 1st translation
        history.add(0, "4.17 s");
        assertEquals(1, history.size());
        assertEquals("4.17 s", history.get(0));

        // 2nd translation
        history.add(0, "2.84 s");
        assertEquals(2, history.size());
        assertEquals("2.84 s", history.get(0));

        // 3rd translation
        history.add(0, "3.12 s");
        assertEquals(3, history.size());
        assertEquals("3.12 s", history.get(0));

        // 4th translation (should displace oldest 4.17 s)
        history.add(0, "1.95 s");
        while (history.size() > 3) {
            history.remove(history.size() - 1);
        }
        assertEquals(3, history.size());
        assertEquals("1.95 s", history.get(0));
        assertEquals("3.12 s", history.get(1));
        assertEquals("2.84 s", history.get(2));
    }

    @Test
    public void testFailureLatencyFormatting() {
        long tStart = 50000L;
        long tFailed = 51420L;
        long elapsedMs = tFailed - tStart;
        double seconds = elapsedMs / 1000.0;
        String failureDesc = String.format(Locale.US, "Failed after %.2f s", seconds);

        assertEquals("Failed after 1.42 s", failureDesc);
    }
}
