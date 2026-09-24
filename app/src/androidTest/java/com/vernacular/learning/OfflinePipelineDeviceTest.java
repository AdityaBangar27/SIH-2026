package com.vernacular.learning;

import android.content.Context;
import android.os.Debug;
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.vernacular.learning.ai.ASRManager;
import com.vernacular.learning.ai.AudioProcessor;
import com.vernacular.learning.ai.PipelineResult;
import com.vernacular.learning.ai.TTSManager;
import com.vernacular.learning.ai.TranslationManager;
import com.vernacular.learning.ai.VoicePipelineManager;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class OfflinePipelineDeviceTest {
    private static final String TAG = "BENCHMARK_ANDROID";
    private Context context;
    private VoicePipelineManager pipelineManager;

    @Before
    public void setUp() throws InterruptedException {
        context = ApplicationProvider.getApplicationContext();
        pipelineManager = new VoicePipelineManager();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] initSuccess = {false};
        pipelineManager.initializeAsync(context, success -> {
            initSuccess[0] = success;
            latch.countDown();
        });

        assertTrue("Initialization timed out", latch.await(120, TimeUnit.SECONDS));
        assertTrue("Pipeline initialization failed", initSuccess[0]);
    }

    @After
    public void tearDown() {
        if (pipelineManager != null) {
            pipelineManager.close();
            pipelineManager = null;
        }
    }

    private File copyTestAudioToInternal(String fileName) throws Exception {
        File srcFile = new File("/data/local/tmp/" + fileName);
        File destFile = new File(context.getCacheDir(), fileName);
        if (srcFile.exists()) {
            try (InputStream in = new FileInputStream(srcFile);
                 OutputStream out = new FileOutputStream(destFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
        return destFile;
    }

    private long getProcessMemoryMb() {
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        return memoryInfo.getTotalPss() / 1024;
    }

    @Test
    public void testAccuracyAndBenchmarksAllSentences() throws Exception {
        String[] testFiles = {
                "test_sentence_1.wav",
                "test_sentence_2.wav",
                "test_sentence_3.wav",
                "test_sentence_4.wav",
                "test_sentence_5.wav",
                "test_sentence_6.wav",
                "test_aditya.wav"
        };

        String[] expectedHindi = {
                "मेरा भारत महान जय हिंद",
                "नमस्ते",
                "आज हम पढ़ाई करेंगे",
                "भारत एक महान देश है",
                "किताब खोलो और पाठ पढ़ो",
                "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है और यह हमें आगे बढ़ाती है",
                "मेरा नाम आदित्य है"
        };

        Log.i(TAG, "==================================================");
        Log.i(TAG, "STARTING REAL ANDROID DEVICE ACCURACY & BENCHMARK TEST");
        Log.i(TAG, "==================================================");

        double totalWer = 0.0;
        double totalCer = 0.0;
        long totalPipelineLatency = 0;
        int validCount = 0;

        for (int i = 0; i < testFiles.length; i++) {
            File audioFile = copyTestAudioToInternal(testFiles[i]);
            if (!audioFile.exists() || audioFile.length() <= 44) {
                Log.w(TAG, "Skipping missing test audio: " + testFiles[i]);
                continue;
            }

            File outputAudioFile = new File(context.getCacheDir(), "out_" + testFiles[i]);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<PipelineResult> resultRef = new AtomicReference<>();

            long memBefore = getProcessMemoryMb();
            long t0 = System.currentTimeMillis();

            pipelineManager.processAsync(audioFile, outputAudioFile, new VoicePipelineManager.PipelineCallback() {
                @Override
                public void onProgress(String stageMessage) {
                    Log.d(TAG, "Stage: " + stageMessage);
                }

                @Override
                public void onComplete(PipelineResult result) {
                    resultRef.set(result);
                    latch.countDown();
                }
            });

            boolean finished = latch.await(60, TimeUnit.SECONDS);
            assertTrue("Pipeline timed out for " + testFiles[i], finished);

            PipelineResult res = resultRef.get();
            assertNotNull(res);
            assertTrue("Pipeline failed for " + testFiles[i] + ": " + res.errorMessage, res.isSuccess);

            long memAfter = getProcessMemoryMb();

            double wer = computeWER(expectedHindi[i], res.recognizedHindiText);
            double cer = computeCER(expectedHindi[i], res.recognizedHindiText);
            totalWer += wer;
            totalCer += cer;
            totalPipelineLatency += res.totalLatencyMs;
            validCount++;

            Log.i(TAG, String.format("--- SENTENCE %d: %s ---", (i + 1), expectedHindi[i]));
            Log.i(TAG, "ASR Recognized: " + res.recognizedHindiText);
            Log.i(TAG, String.format("Accuracy Metrics: WER=%.2f%% | CER=%.2f%%", wer * 100.0, cer * 100.0));
            Log.i(TAG, "ASR Latency: " + res.asrLatencyMs + " ms");
            Log.i(TAG, "Santali Translation: " + res.translatedSantaliText);
            Log.i(TAG, "Translation Latency: " + res.translationLatencyMs + " ms");
            Log.i(TAG, "TTS Latency: " + res.ttsLatencyMs + " ms");
            if (res.outputAudioFile != null) {
                Log.i(TAG, "TTS Audio File: " + res.outputAudioFile.getAbsolutePath() + " (" + res.outputAudioFile.length() + " bytes)");
            }
            Log.i(TAG, "Total Pipeline Latency: " + res.totalLatencyMs + " ms");
            Log.i(TAG, "Process PSS RAM: " + memAfter + " MB (Delta: " + (memAfter - memBefore) + " MB)");

            assertFalse("Recognized text was empty", res.recognizedHindiText.isEmpty());
            assertFalse("Translated text was empty", res.translatedSantaliText.isEmpty());
            assertNotNull("TTS output audio must not be null", res.outputAudioFile);
            assertTrue("TTS output audio file must exist", res.outputAudioFile.exists());
            assertTrue("TTS output audio must have valid WAV content", res.outputAudioFile.length() > 44);
            assertTrue("Total latency must be < 10000ms", res.totalLatencyMs < 10000);
        }

        double avgWer = (validCount > 0) ? (totalWer / validCount) * 100.0 : 0.0;
        double avgCer = (validCount > 0) ? (totalCer / validCount) * 100.0 : 0.0;
        long avgLatency = (validCount > 0) ? (totalPipelineLatency / validCount) : 0;

        Log.i(TAG, "==================================================");
        Log.i(TAG, String.format("BENCHMARK SUMMARY: Tested %d sentences", validCount));
        Log.i(TAG, String.format("AVERAGE WER: %.2f%% (Target: <= 10%%)", avgWer));
        Log.i(TAG, String.format("AVERAGE CER: %.2f%% (Target: <= 15%%)", avgCer));
        Log.i(TAG, String.format("AVERAGE TOTAL PIPELINE LATENCY: %d ms (Target: < 10000 ms)", avgLatency));
        Log.i(TAG, "==================================================");
    }

    private static double computeWER(String reference, String hypothesis) {
        String[] refWords = reference.trim().replaceAll("[\\!\\?\\.\\,।॥]+", "").split("\\s+");
        String[] hypWords = hypothesis.trim().replaceAll("[\\!\\?\\.\\,।॥]+", "").split("\\s+");
        int[][] d = new int[refWords.length + 1][hypWords.length + 1];
        for (int i = 0; i <= refWords.length; i++) d[i][0] = i;
        for (int j = 0; j <= hypWords.length; j++) d[0][j] = j;
        for (int i = 1; i <= refWords.length; i++) {
            for (int j = 1; j <= hypWords.length; j++) {
                if (refWords[i - 1].equalsIgnoreCase(hypWords[j - 1])) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    d[i][j] = Math.min(d[i - 1][j] + 1, Math.min(d[i][j - 1] + 1, d[i - 1][j - 1] + 1));
                }
            }
        }
        return refWords.length == 0 ? 0.0 : (double) d[refWords.length][hypWords.length] / refWords.length;
    }

    private static double computeCER(String reference, String hypothesis) {
        String ref = reference.trim().replaceAll("[\\!\\?\\.\\,।॥\\s]+", "");
        String hyp = hypothesis.trim().replaceAll("[\\!\\?\\.\\,।॥\\s]+", "");
        int[][] d = new int[ref.length() + 1][hyp.length() + 1];
        for (int i = 0; i <= ref.length(); i++) d[i][0] = i;
        for (int j = 0; j <= hyp.length(); j++) d[0][j] = j;
        for (int i = 1; i <= ref.length(); i++) {
            for (int j = 1; j <= hyp.length(); j++) {
                if (ref.charAt(i - 1) == hyp.charAt(j - 1)) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    d[i][j] = Math.min(d[i - 1][j] + 1, Math.min(d[i][j - 1] + 1, d[i - 1][j - 1] + 1));
                }
            }
        }
        return ref.length() == 0 ? 0.0 : (double) d[ref.length()][hyp.length()] / ref.length();
    }

    @Test
    public void testFiveRunStabilityTest() throws Exception {
        File audioFile = copyTestAudioToInternal("test_sentence_1.wav");
        File outputAudioFile = new File(context.getCacheDir(), "out_stability.wav");

        Log.i(TAG, "==================================================");
        Log.i(TAG, "STARTING 5-RUN CONSECUTIVE STABILITY TEST");
        Log.i(TAG, "==================================================");

        for (int run = 1; run <= 5; run++) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<PipelineResult> resultRef = new AtomicReference<>();

            long memBefore = getProcessMemoryMb();
            long t0 = System.currentTimeMillis();

            pipelineManager.processAsync(audioFile, outputAudioFile, new VoicePipelineManager.PipelineCallback() {
                @Override public void onProgress(String stageMessage) {}
                @Override
                public void onComplete(PipelineResult result) {
                    resultRef.set(result);
                    latch.countDown();
                }
            });

            assertTrue("Run " + run + " timed out", latch.await(45, TimeUnit.SECONDS));
            PipelineResult res = resultRef.get();
            assertNotNull(res);
            assertTrue("Run " + run + " failed: " + res.errorMessage, res.isSuccess);

            long memAfter = getProcessMemoryMb();

            assertNotNull("Run " + run + " audio output should not be null", res.outputAudioFile);
            assertTrue("Run " + run + " audio output should exist", res.outputAudioFile.exists() && res.outputAudioFile.length() > 44);

            Log.i(TAG, String.format("Run %d -> ASR: %d ms | Translation: %d ms | TTS: %d ms | Total: %d ms | RAM: %d MB",
                    run, res.asrLatencyMs, res.translationLatencyMs, res.ttsLatencyMs, res.totalLatencyMs, memAfter));
        }

        Log.i(TAG, "==================================================");
        Log.i(TAG, "5-RUN STABILITY TEST PASSED WITH ZERO FAILURES");
        Log.i(TAG, "==================================================");
    }

    @Test
    public void testAudioPlayerPlayback() throws Exception {
        TTSManager tts = new TTSManager();
        boolean initOk = tts.initialize(context);
        assertTrue("TTS initialization failed", initOk);

        File outputFile = new File(context.getCacheDir(), "out_playback_test.wav");
        File generatedFile = tts.synthesize("ᱡᱚᱦᱟᱨ ᱜᱮ ᱥᱟᱱᱟᱢ ᱠᱚ", outputFile);
        assertNotNull("Generated audio must not be null", generatedFile);
        assertTrue("Generated audio must exist", generatedFile.exists() && generatedFile.length() > 44);

        File destTmp = new File(context.getExternalFilesDir(null), "santali_playback_verified.wav");
        try (InputStream in = new FileInputStream(generatedFile);
             OutputStream out = new FileOutputStream(destTmp)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        CountDownLatch playLatch = new CountDownLatch(1);
        boolean[] playStarted = new boolean[]{false};
        boolean[] playCompleted = new boolean[]{false};

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            com.vernacular.learning.utils.AudioPlayer player = new com.vernacular.learning.utils.AudioPlayer();
            player.play(generatedFile, new com.vernacular.learning.utils.AudioPlayer.PlaybackCallback() {
                @Override
                public void onPlaybackStarted() {
                    playStarted[0] = true;
                    Log.i(TAG, "AudioPlayer: Playback started successfully on device");
                }
                @Override
                public void onPlaybackCompleted() {
                    playCompleted[0] = true;
                    Log.i(TAG, "AudioPlayer: Playback completed successfully on device");
                    playLatch.countDown();
                }
                @Override
                public void onError(String message) {
                    Log.e(TAG, "AudioPlayer error: " + message);
                    playLatch.countDown();
                }
            });
        });

        assertTrue("Audio playback timed out", playLatch.await(15, TimeUnit.SECONDS));
        assertTrue("Audio playback did not start", playStarted[0]);
        assertTrue("Audio playback did not complete", playCompleted[0]);
        tts.close();
    }
}
