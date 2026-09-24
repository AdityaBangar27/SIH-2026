package com.vernacular.learning.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.vernacular.learning.utils.TwoWayTranslationHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AIPipelineTest {

    @Test
    public void testPipelineResultCreationAndMetrics() {
        PipelineResult result = PipelineResult.success(
                "नमस्ते",
                "ᱡᱚᱦᱟᱨ",
                null,
                150L,
                45L,
                0L,
                195L
        );

        assertTrue(result.isSuccess);
        assertEquals("नमस्ते", result.recognizedHindiText);
        assertEquals("ᱡᱚᱦᱟᱨ", result.translatedSantaliText);
        assertEquals(150L, result.asrLatencyMs);
        assertEquals(45L, result.translationLatencyMs);
        assertEquals(0L, result.ttsLatencyMs);
        assertEquals(195L, result.totalLatencyMs);
    }

    @Test
    public void testTTSManagerUninitializedStatusHandling() {
        TTSManager ttsManager = new TTSManager();
        assertFalse(ttsManager.isAvailable());
        assertEquals("TTS Unavailable", ttsManager.getStatusMessage());
    }

    @Test
    public void testTTSManagerConfigAndPhonemeMapping() throws Exception {
        TTSManager ttsManager = new TTSManager();
        File configFile = new File("src/main/assets/models/tts/sat_piper_model.onnx.json");
        if (!configFile.exists()) {
            configFile = new File("app/src/main/assets/models/tts/sat_piper_model.onnx.json");
        }
        assertTrue("sat_piper_model.onnx.json must exist in assets", configFile.exists());

        try (InputStream is = new FileInputStream(configFile)) {
            ttsManager.loadConfig(is);
        }

        assertEquals(16000, ttsManager.getSampleRate());
        assertFalse(ttsManager.getPhonemeIdMap().isEmpty());
        assertTrue(ttsManager.getPhonemeIdMap().containsKey("ᱥ"));
        assertTrue(ttsManager.getPhonemeIdMap().containsKey("ᱟ"));
        assertTrue(ttsManager.getPhonemeIdMap().containsKey("ᱱ"));

        // Test phoneme conversion for "ᱥᱟᱱᱟᱢ ᱠᱚ ᱜᱮ ᱥᱟᱹᱜᱩᱱ ᱡᱚᱦᱟᱨ"
        List<Long> ids = ttsManager.textToPhonemeIds("ᱥᱟᱱᱟᱢ ᱠᱚ ᱜᱮ ᱥᱟᱹᱜᱩᱱ ᱡᱚᱦᱟᱨ");
        assertNotNull(ids);
        assertTrue(ids.size() > 10);
        assertEquals(Long.valueOf(1L), ids.get(0)); // BOS
        assertEquals(Long.valueOf(2L), ids.get(ids.size() - 1)); // EOS
    }

    @Test
    public void testTTSPhonemizationCoverage() throws Exception {
        TTSManager ttsManager = new TTSManager();
        File configFile = new File("src/main/assets/models/tts/sat_piper_model.onnx.json");
        if (!configFile.exists()) {
            configFile = new File("app/src/main/assets/models/tts/sat_piper_model.onnx.json");
        }
        assertTrue("sat_piper_model.onnx.json must exist in assets", configFile.exists());

        try (InputStream is = new FileInputStream(configFile)) {
            ttsManager.loadConfig(is);
        }

        // Test coverage of Ol Chiki phoneme mapping across diverse Santali phrases
        String[] phrases = {
                "ᱥᱟᱱᱟᱢ ᱠᱚ ᱜᱮ ᱥᱟᱹᱜᱩᱱ ᱡᱚᱦᱟᱨ",
                "ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ , ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱾",
                "ᱛᱮᱦᱮᱧ ᱟᱢ ᱯᱟᱲᱦᱟᱣ ᱢᱮ ᱾",
                "ᱵᱷᱟᱨᱚᱛ ᱫᱚ ᱢᱤᱫᱴᱟᱝ ᱢᱟᱨᱟᱝ ᱫᱤᱥᱚᱢ ᱾",
                "ᱯᱚᱛᱚᱵ ᱫᱚ ᱮᱦᱚᱵ ᱢᱮ ᱟᱨ ᱚᱞ ᱫᱚ ᱯᱟᱲᱦᱟᱣ ᱢᱮ ᱾"
        };

        for (String phrase : phrases) {
            List<Long> ids = ttsManager.textToPhonemeIds(phrase);
            assertNotNull("Phoneme ID sequence must not be null", ids);
            assertTrue("Phoneme sequence must contain phonemes: " + phrase, ids.size() > 5);
            assertEquals("Must start with BOS (1)", Long.valueOf(1L), ids.get(0));
            assertEquals("Must end with EOS (2)", Long.valueOf(2L), ids.get(ids.size() - 1));
        }
    }

    @Test
    public void testAudioProcessorFeatureExtraction() {
        AudioProcessor processor = new AudioProcessor();
        float[] dummyAudio = new float[16000]; // 1 second of silent audio at 16kHz
        float[] melFeatures = processor.generateMelSpectrogram(dummyAudio);

        assertNotNull(melFeatures);
        // Shape [1, 80, 3000] -> flattened size 240,000
        assertEquals(1 * 80 * 3000, melFeatures.length);
    }

    @Test
    public void testASRByteLevelBpeDecodingCorrectUnicode() throws Exception {
        ASRManager asrManager = new ASRManager();
        File vocabFile = new File("src/main/assets/tokenizers/asr/vocab.json");
        if (!vocabFile.exists()) {
            vocabFile = new File("app/src/main/assets/tokenizers/asr/vocab.json");
        }
        assertTrue("vocab.json must exist", vocabFile.exists());

        try (InputStream is = new FileInputStream(vocabFile)) {
            asrManager.loadVocabulary(is);
        }

        // Token sequence for: "मेरा भारत महान जय हिंद"
        // [50258, 50363, 48521, 21981, 25411, 17937, 8485, 255, 17937, 25411, 36158, 48449, 44500, 17937, 35082, 8485, 250, 48268, 37139, 33279, 31945, 3941, 99, 50257]
        List<Long> tokenIds = Arrays.asList(
                50258L, 50363L, 48521L, 21981L, 25411L, 17937L, 8485L, 255L, 17937L,
                25411L, 36158L, 48449L, 44500L, 17937L, 35082L, 8485L, 250L, 48268L,
                37139L, 33279L, 31945L, 3941L, 99L, 50257L
        );

        String decodedHindi = asrManager.decodeTokenSequence(tokenIds);

        // Verify Devanagari text is decoded cleanly without mojibake (à, â, Ã, etc.)
        assertEquals("मेरा भारत महान जय हिंद", decodedHindi);
        assertFalse("Decoded text should not contain mojibake 'à'", decodedHindi.contains("à"));
        assertFalse("Decoded text should not contain mojibake 'Ã'", decodedHindi.contains("Ã"));
    }

    @Test
    public void testIndicTrans2SourceTokenization() throws Exception {
        TranslationManager tm = new TranslationManager();
        File srcFile = new File("src/main/assets/tokenizers/translation/dict.SRC.json");
        if (!srcFile.exists()) {
            srcFile = new File("app/src/main/assets/tokenizers/translation/dict.SRC.json");
        }
        File tgtFile = new File("src/main/assets/tokenizers/translation/dict.TGT.json");
        if (!tgtFile.exists()) {
            tgtFile = new File("app/src/main/assets/tokenizers/translation/dict.TGT.json");
        }
        assertTrue("dict.SRC.json must exist", srcFile.exists());
        assertTrue("dict.TGT.json must exist", tgtFile.exists());

        try (InputStream srcIs = new FileInputStream(srcFile);
             InputStream tgtIs = new FileInputStream(tgtFile)) {
            tm.loadDictionaries(srcIs, tgtIs);
        }

        // Test 1: मेरा भारत महान जय हिंद
        List<Long> ids1 = tm.tokenizeSourceForTest("मेरा भारत महान जय हिंद");
        List<Long> expected1 = Arrays.asList(8L, 29925L, 2262L, 106L, 2337L, 2776L, 5275L, 2L);
        assertEquals("Token IDs for 'मेरा भारत महान जय हिंद' must match HF tokenizer exactly", expected1, ids1);

        // Test 2: नमस्ते
        List<Long> ids2 = tm.tokenizeSourceForTest("नमस्ते");
        List<Long> expected2 = Arrays.asList(8L, 29925L, 41881L, 2L);
        assertEquals("Token IDs for 'नमस्ते' must match HF tokenizer exactly", expected2, ids2);

        // Test 3: आज हम पढ़ाई करेंगे
        List<Long> ids3 = tm.tokenizeSourceForTest("आज हम पढ़ाई करेंगे");
        List<Long> expected3 = Arrays.asList(8L, 29925L, 772L, 293L, 12201L, 1628L, 2L);
        assertEquals("Token IDs for 'आज हम पढ़ाई करेंगे' must match HF tokenizer exactly", expected3, ids3);

        // Test 4: भारत एक महान देश है
        List<Long> ids4 = tm.tokenizeSourceForTest("भारत एक महान देश है");
        List<Long> expected4 = Arrays.asList(8L, 29925L, 106L, 34L, 2337L, 239L, 11L, 2L);
        assertEquals("Token IDs for 'भारत एक महान देश है' must match HF tokenizer exactly", expected4, ids4);
    }

    @Test
    public void testIndicTrans2TargetDecodingOlChikiAndNoUnk() throws Exception {
        TranslationManager tm = new TranslationManager();
        File tgtFile = new File("src/main/assets/tokenizers/translation/dict.TGT.json");
        if (!tgtFile.exists()) {
            tgtFile = new File("app/src/main/assets/tokenizers/translation/dict.TGT.json");
        }
        try (InputStream tgtIs = new FileInputStream(tgtFile)) {
            tm.loadDictionaries(null, tgtIs);
        }

        // Tokens generated by IndicTrans2 for "मेरा भारत महान जय हिंद":
        // [2, 76527, 89023, 74948, 48377, 79018, 63364, 67823, 92985, 99320, 106289, 74321, 31428, 2]
        List<Long> targetIds = Arrays.asList(
                2L, 76527L, 89023L, 74948L, 48377L, 79018L, 63364L, 67823L,
                92985L, 99320L, 106289L, 74321L, 31428L, 2L
        );
        String decodedSantali = tm.decodeTargetTokensForTest(targetIds);

        assertEquals("ᱤᱧᱟᱹᱜ ᱵᱷᱟᱨᱚᱛ ᱫᱚ ᱦᱩᱭᱩᱜ ᱠᱟᱱᱟ ᱢᱮᱥᱴ ᱡᱚᱭ ᱦᱤᱱᱫ ᱾", decodedSantali);
        assertFalse("Target text must not contain <unk>", decodedSantali.contains("<unk>"));

        // Verify that UNK sequence does not produce <unk><unk>
        List<Long> unkSequence = Arrays.asList(2L, 3L, 3L, 3L, 3L, 2L);
        String decodedUnk = tm.decodeTargetTokensForTest(unkSequence);
        assertEquals("Special unk tokens must be skipped cleanly", "", decodedUnk);
    }

    @Test
    public void testVerifiedClassroomCorpusLookup() {
        assertTrue("नमस्ते must be present in verified dictionary",
                TwoWayTranslationHelper.hasVerifiedTranslation("नमस्ते"));
        assertEquals("ᱡᱚᱦᱟᱨ", TwoWayTranslationHelper.getVerifiedTranslation("नमस्ते"));

        assertTrue("एक, दो, तीन must be present in verified dictionary",
                TwoWayTranslationHelper.hasVerifiedTranslation("एक, दो, तीन"));
        assertEquals("ᱢᱤᱫ, ᱵᱟᱨ, ᱯᱮ", TwoWayTranslationHelper.getVerifiedTranslation("एक, दो, तीन"));

        assertTrue("किताब खोलो must be present in verified dictionary",
                TwoWayTranslationHelper.hasVerifiedTranslation("किताब खोलो"));
        assertEquals("ᱯᱩᱛᱷᱤ ᱡᱷᱤᱡᱽ ᱢᱮ", TwoWayTranslationHelper.getVerifiedTranslation("किताब खोलो"));
    }
}
