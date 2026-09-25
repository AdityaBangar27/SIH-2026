package com.vernacular.learning.ai;

import android.content.Context;
import android.util.Log;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Text-to-Speech Manager for local offline Santali speech synthesis.
 * Uses native Santali Piper VITS ONNX model (sat_piper_model.onnx)
 * with authentic Ol Chiki G2P Phonemizer (Grapheme-to-Phoneme converter)
 * and Piper eSpeak 189-symbol phoneme vocabulary.
 * Operates completely offline on device CPU within the 2 GB RAM budget.
 */
public class TTSManager {
    private static final String TAG = "TTSManager";

    public static final String MODEL_ASSET = "models/tts/sat_piper_model.onnx";
    public static final String CONFIG_ASSET = "models/tts/sat_piper_model.onnx.json";

    private static volatile TTSManager sInstance;

    public static TTSManager getInstance() {
        if (sInstance == null) {
            synchronized (TTSManager.class) {
                if (sInstance == null) {
                    sInstance = new TTSManager();
                }
            }
        }
        return sInstance;
    }

    private OrtSession ttsSession;
    private final Map<String, List<Long>> phonemeIdMap = new HashMap<>();
    private int sampleRate = 16000;
    // Calibrated Piper VITS inference hyperparameters matching trained acoustic generator
    private float noiseScale = 0.667f;
    private float lengthScale = 1.0f;
    private float noiseW = 0.800f;
    private long bosId = 1L;
    private long eosId = 2L;
    private long padId = 0L;
    private long spaceId = 3L;

    private boolean isInitialized = false;

    // Android offline Hindi TTS engine fallback
    private android.speech.tts.TextToSpeech hindiTtsEngine;
    private volatile boolean isHindiTtsAvailable = false;

    // --- Multi-character Atomic IPA Phonemes ---
    private static final String[] MULTI_CHAR_PHONEMES = {
            "kʰ", "ɡʰ", "cʰ", "ɟʰ", "ʈʰ", "ɖʰ", "tʰ", "dʰ", "pʰ", "bʰ", "ɽʰ",
            "ə̃", "ã", "ĩ", "ũ", "ẽ", "õ", "ɛ̃", "ɔ̃",
            "ɔː", "aː", "iː", "uː", "eː", "oː",
            "w̃", "ri"
    };

    // --- Santhali Ol Chiki G2P Phonemizer Tables ---
    private static final Map<String, String> OL_CHIKI_NUMERALS = new LinkedHashMap<>();
    private static final List<String[]> COMPOUND_MAP = new ArrayList<>();
    private static final Map<String, String> SINGLE_CHAR_MAP = new HashMap<>();

    // --- Hindi Devanagari G2P Phonemizer Tables ---
    private static final Map<Character, String> HINDI_VOWELS = new HashMap<>();
    private static final Map<Character, String> HINDI_MATRAS = new HashMap<>();
    private static final Map<String, String> HINDI_CONSONANTS = new HashMap<>();

    static {
        // 1. Numerals (Ol Chiki ᱐-᱙ to spoken Santhali words)
        OL_CHIKI_NUMERALS.put("᱐", "ᱥᱩᱱ");   // sun (zero)
        OL_CHIKI_NUMERALS.put("᱑", "ᱢᱤᱫ");   // mid (one)
        OL_CHIKI_NUMERALS.put("᱒", "ᱵᱟᱨ");   // bar (two)
        OL_CHIKI_NUMERALS.put("᱓", "ᱯᱮ");    // pe (three)
        OL_CHIKI_NUMERALS.put("᱔", "ᱯᱳᱱ");   // pon (four)
        OL_CHIKI_NUMERALS.put("᱕", "ᱢᱚᱬᱮ");  // mone (five)
        OL_CHIKI_NUMERALS.put("᱖", "ᱛᱩᱨᱩᱭ"); // turui (six)
        OL_CHIKI_NUMERALS.put("᱗", "ᱮᱭᱟᱭ");  // eyae (seven)
        OL_CHIKI_NUMERALS.put("᱘", "ᱤᱨᱟᱹᱞ"); // irəl (eight)
        OL_CHIKI_NUMERALS.put("᱙", "ᱟᱨᱮ");   // are (nine)

        // 2. Multi-character compound mappings (ordered by length descending)
        // Deglottalized plosives with Ohod (ᱽ)
        COMPOUND_MAP.add(new String[]{"ᱜᱽ", "ɡ"});
        COMPOUND_MAP.add(new String[]{"ᱡᱽ", "ɟ"});
        COMPOUND_MAP.add(new String[]{"ᱫᱽ", "d"});
        COMPOUND_MAP.add(new String[]{"ᱵᱽ", "b"});

        // Aspirated plosives with Oh (ᱷ)
        COMPOUND_MAP.add(new String[]{"ᱛᱷ", "tʰ"});
        COMPOUND_MAP.add(new String[]{"ᱠᱷ", "kʰ"});
        COMPOUND_MAP.add(new String[]{"ᱪᱷ", "cʰ"});
        COMPOUND_MAP.add(new String[]{"ᱯᱷ", "pʰ"});
        COMPOUND_MAP.add(new String[]{"ᱴᱷ", "ʈʰ"});
        COMPOUND_MAP.add(new String[]{"ᱫᱷ", "dʰ"});
        COMPOUND_MAP.add(new String[]{"ᱜᱷ", "ɡʰ"});
        COMPOUND_MAP.add(new String[]{"ᱡᱷ", "ɟʰ"});
        COMPOUND_MAP.add(new String[]{"ᱵᱷ", "bʰ"});
        COMPOUND_MAP.add(new String[]{"ᱰᱷ", "ɖʰ"});

        // Vowels with Mu-Gahla Tundag (ᱺ) - Nasal + Low
        COMPOUND_MAP.add(new String[]{"ᱚᱺ", "ə̃"});
        COMPOUND_MAP.add(new String[]{"ᱟᱺ", "ə̃"});
        COMPOUND_MAP.add(new String[]{"ᱮᱺ", "ɛ̃"});

        // Vowels with Gahla Tundag (ᱹ) - Low/Centralized
        COMPOUND_MAP.add(new String[]{"ᱚᱹ", "ə"});
        COMPOUND_MAP.add(new String[]{"ᱟᱹ", "ə"});
        COMPOUND_MAP.add(new String[]{"ᱮᱹ", "ɛ"});
        COMPOUND_MAP.add(new String[]{"ᱩᱹ", "ʊ"});
        COMPOUND_MAP.add(new String[]{"ᱤᱹ", "ɪ"});
        COMPOUND_MAP.add(new String[]{"ᱳᱹ", "ɔ"});

        // Vowels with Mu Tundag (ᱸ) - Nasalization
        COMPOUND_MAP.add(new String[]{"ᱚᱸ", "ɔ̃"});
        COMPOUND_MAP.add(new String[]{"ᱟᱸ", "ã"});
        COMPOUND_MAP.add(new String[]{"ᱤᱸ", "ĩ"});
        COMPOUND_MAP.add(new String[]{"ᱩᱸ", "ũ"});
        COMPOUND_MAP.add(new String[]{"ᱮᱸ", "ẽ"});
        COMPOUND_MAP.add(new String[]{"ᱳᱸ", "õ"});

        // Vowels with Relah (ᱻ) - Lengthening
        COMPOUND_MAP.add(new String[]{"ᱚᱻ", "ɔː"});
        COMPOUND_MAP.add(new String[]{"ᱟᱻ", "aː"});
        COMPOUND_MAP.add(new String[]{"ᱤᱻ", "iː"});
        COMPOUND_MAP.add(new String[]{"ᱩᱻ", "uː"});
        COMPOUND_MAP.add(new String[]{"ᱮᱻ", "eː"});
        COMPOUND_MAP.add(new String[]{"ᱳᱻ", "oː"});

        // 3. Single Ol Chiki characters to IPA
        SINGLE_CHAR_MAP.put("ᱚ", "ɔ");
        SINGLE_CHAR_MAP.put("ᱟ", "a");
        SINGLE_CHAR_MAP.put("ᱤ", "i");
        SINGLE_CHAR_MAP.put("ᱩ", "u");
        SINGLE_CHAR_MAP.put("ᱮ", "e");
        SINGLE_CHAR_MAP.put("ᱳ", "o");

        SINGLE_CHAR_MAP.put("ᱛ", "t");
        SINGLE_CHAR_MAP.put("ᱠ", "k");
        SINGLE_CHAR_MAP.put("ᱪ", "c");
        SINGLE_CHAR_MAP.put("ᱯ", "p");
        SINGLE_CHAR_MAP.put("ᱴ", "ʈ");
        SINGLE_CHAR_MAP.put("ᱰ", "ɖ");

        SINGLE_CHAR_MAP.put("ᱜ", "ɡ");
        SINGLE_CHAR_MAP.put("ᱡ", "ɟ");
        SINGLE_CHAR_MAP.put("ᱫ", "d");
        SINGLE_CHAR_MAP.put("ᱵ", "b");

        SINGLE_CHAR_MAP.put("ᱝ", "ŋ");
        SINGLE_CHAR_MAP.put("ᱢ", "m");
        SINGLE_CHAR_MAP.put("ᱧ", "ɲ");
        SINGLE_CHAR_MAP.put("ᱬ", "ɳ");
        SINGLE_CHAR_MAP.put("ᱱ", "n");
        SINGLE_CHAR_MAP.put("ᱶ", "w̃");

        SINGLE_CHAR_MAP.put("ᱞ", "l");
        SINGLE_CHAR_MAP.put("ᱣ", "w");
        SINGLE_CHAR_MAP.put("ᱥ", "s");
        SINGLE_CHAR_MAP.put("ᱦ", "h");
        SINGLE_CHAR_MAP.put("ᱨ", "r");
        SINGLE_CHAR_MAP.put("ᱭ", "j");
        SINGLE_CHAR_MAP.put("ᱲ", "ɽ");
        SINGLE_CHAR_MAP.put("ᱷ", "ʰ");

        SINGLE_CHAR_MAP.put("ᱸ", "̃");
        SINGLE_CHAR_MAP.put("ᱹ", "ə");
        SINGLE_CHAR_MAP.put("ᱺ", "ə̃");
        SINGLE_CHAR_MAP.put("ᱻ", "ː");
        SINGLE_CHAR_MAP.put("ᱼ", "ʔ");
        SINGLE_CHAR_MAP.put("ᱽ", "");

        SINGLE_CHAR_MAP.put("᱾", ".");
        SINGLE_CHAR_MAP.put("᱿", ".");

        // 4. Hindi Devanagari Vowels to IPA
        HINDI_VOWELS.put('अ', "ə");
        HINDI_VOWELS.put('आ', "a");
        HINDI_VOWELS.put('इ', "i");
        HINDI_VOWELS.put('ई', "i");
        HINDI_VOWELS.put('उ', "u");
        HINDI_VOWELS.put('ऊ', "u");
        HINDI_VOWELS.put('ए', "e");
        HINDI_VOWELS.put('ऐ', "ɛ");
        HINDI_VOWELS.put('ओ', "o");
        HINDI_VOWELS.put('औ', "ɔ");
        HINDI_VOWELS.put('ऋ', "ri");

        // 5. Hindi Devanagari Matras to IPA
        HINDI_MATRAS.put('ा', "a");
        HINDI_MATRAS.put('ि', "i");
        HINDI_MATRAS.put('ी', "i");
        HINDI_MATRAS.put('ु', "u");
        HINDI_MATRAS.put('ू', "u");
        HINDI_MATRAS.put('े', "e");
        HINDI_MATRAS.put('ै', "ɛ");
        HINDI_MATRAS.put('ो', "o");
        HINDI_MATRAS.put('ौ', "ɔ");
        HINDI_MATRAS.put('ृ', "ri");

        // 6. Hindi Devanagari Consonants to IPA
        HINDI_CONSONANTS.put("क", "k");
        HINDI_CONSONANTS.put("ख", "kʰ");
        HINDI_CONSONANTS.put("ग", "ɡ");
        HINDI_CONSONANTS.put("घ", "ɡʰ");
        HINDI_CONSONANTS.put("ङ", "ŋ");
        HINDI_CONSONANTS.put("च", "c");
        HINDI_CONSONANTS.put("छ", "cʰ");
        HINDI_CONSONANTS.put("ज", "ɟ");
        HINDI_CONSONANTS.put("झ", "ɟʰ");
        HINDI_CONSONANTS.put("ञ", "ɲ");
        HINDI_CONSONANTS.put("ट", "ʈ");
        HINDI_CONSONANTS.put("ठ", "ʈʰ");
        HINDI_CONSONANTS.put("ड", "ɖ");
        HINDI_CONSONANTS.put("ढ", "ɖʰ");
        HINDI_CONSONANTS.put("ण", "ɳ");
        HINDI_CONSONANTS.put("त", "t");
        HINDI_CONSONANTS.put("थ", "tʰ");
        HINDI_CONSONANTS.put("द", "d");
        HINDI_CONSONANTS.put("ध", "dʰ");
        HINDI_CONSONANTS.put("न", "n");
        HINDI_CONSONANTS.put("प", "p");
        HINDI_CONSONANTS.put("फ", "pʰ");
        HINDI_CONSONANTS.put("ब", "b");
        HINDI_CONSONANTS.put("भ", "bʰ");
        HINDI_CONSONANTS.put("म", "m");
        HINDI_CONSONANTS.put("य", "j");
        HINDI_CONSONANTS.put("र", "r");
        HINDI_CONSONANTS.put("ल", "l");
        HINDI_CONSONANTS.put("व", "w");
        HINDI_CONSONANTS.put("श", "ʃ");
        HINDI_CONSONANTS.put("ष", "ʂ");
        HINDI_CONSONANTS.put("स", "s");
        HINDI_CONSONANTS.put("ह", "h");
        HINDI_CONSONANTS.put("ड़", "ɽ");
        HINDI_CONSONANTS.put("ढ़", "ɽ");
        HINDI_CONSONANTS.put("क़", "k");
        HINDI_CONSONANTS.put("ख़", "x");
        HINDI_CONSONANTS.put("ग़", "ɣ");
        HINDI_CONSONANTS.put("ज़", "z");
        HINDI_CONSONANTS.put("फ़", "f");
    }

    public synchronized boolean initialize(Context context) {
        if (isInitialized && ttsSession != null) {
            return true;
        }

        try {
            Log.i(TAG, "Initializing Santali Piper VITS TTS model & Hindi TTS...");
            long t0 = System.currentTimeMillis();

            // 1. Load config and phoneme mapping
            loadConfig(context);

            // 2. Load ONNX model session via ModelManager
            ModelManager modelManager = ModelManager.getInstance();
            ttsSession = modelManager.createSession(context, MODEL_ASSET);

            // 3. Initialize offline Android Hindi TTS
            try {
                if (context != null) {
                    hindiTtsEngine = new android.speech.tts.TextToSpeech(context.getApplicationContext(), status -> {
                        if (status == android.speech.tts.TextToSpeech.SUCCESS && hindiTtsEngine != null) {
                            int r = hindiTtsEngine.setLanguage(new java.util.Locale("hi", "IN"));
                            if (r == android.speech.tts.TextToSpeech.LANG_MISSING_DATA ||
                                r == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                                hindiTtsEngine.setLanguage(new java.util.Locale("hi"));
                            }
                            isHindiTtsAvailable = true;
                            Log.i(TAG, "Android offline Hindi TTS initialized successfully.");
                        }
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "Note on Hindi TTS init: " + e.getMessage());
            }

            isInitialized = (ttsSession != null && !phonemeIdMap.isEmpty());
            long initTime = System.currentTimeMillis() - t0;
            Log.i(TAG, "Santali TTS initialized in " + initTime + " ms. Ready: " + isInitialized +
                    " (phoneme symbols: " + phonemeIdMap.size() + ", sampleRate: " + sampleRate + " Hz)");
            return isInitialized;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Santali TTSManager", e);
            isInitialized = false;
            return false;
        }
    }

    public void loadConfig(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        JSONObject config = new JSONObject(baos.toString(StandardCharsets.UTF_8.name()));

        if (config.has("audio")) {
            JSONObject audio = config.getJSONObject("audio");
            if (audio.has("sample_rate")) {
                this.sampleRate = audio.getInt("sample_rate");
            }
        }

        if (config.has("inference")) {
            JSONObject inf = config.getJSONObject("inference");
            if (inf.has("length_scale")) lengthScale = (float) inf.getDouble("length_scale");
            if (inf.has("noise_scale")) noiseScale = (float) inf.getDouble("noise_scale");
            if (inf.has("noise_w")) noiseW = (float) inf.getDouble("noise_w");
        } else {
            this.noiseScale = 0.667f;
            this.lengthScale = 1.0f;
            this.noiseW = 0.800f;
        }

        if (config.has("phoneme_id_map")) {
            JSONObject idMapJson = config.getJSONObject("phoneme_id_map");
            Iterator<String> keys = idMapJson.keys();
            phonemeIdMap.clear();
            while (keys.hasNext()) {
                String symbol = keys.next();
                JSONArray arr = idMapJson.getJSONArray(symbol);
                List<Long> ids = new ArrayList<>(arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    ids.add(arr.getLong(i));
                }
                phonemeIdMap.put(symbol, ids);
            }
        }

        if (phonemeIdMap.containsKey("^") && !phonemeIdMap.get("^").isEmpty()) {
            bosId = phonemeIdMap.get("^").get(0);
        }
        if (phonemeIdMap.containsKey("$") && !phonemeIdMap.get("$").isEmpty()) {
            eosId = phonemeIdMap.get("$").get(0);
        }
        if (phonemeIdMap.containsKey("_") && !phonemeIdMap.get("_").isEmpty()) {
            padId = phonemeIdMap.get("_").get(0);
        }
        if (phonemeIdMap.containsKey(" ") && !phonemeIdMap.get(" ").isEmpty()) {
            spaceId = phonemeIdMap.get(" ").get(0);
        }
    }

    private void loadConfig(Context context) throws Exception {
        InputStream is = null;
        try {
            try {
                is = context.getAssets().open(CONFIG_ASSET);
            } catch (Exception e) {
                File localAsset = new File("src/main/assets/" + CONFIG_ASSET);
                if (!localAsset.exists()) {
                    localAsset = new File("app/src/main/assets/" + CONFIG_ASSET);
                }
                if (localAsset.exists()) {
                    is = new java.io.FileInputStream(localAsset);
                } else {
                    throw e;
                }
            }
            loadConfig(is);
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
        }
    }

    public boolean isAvailable() {
        return isInitialized && ttsSession != null;
    }

    public String getStatusMessage() {
        if (isAvailable()) {
            return "TTS Ready (Santali Piper VITS)";
        } else {
            return "TTS Unavailable";
        }
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public Map<String, List<Long>> getPhonemeIdMap() {
        return Collections.unmodifiableMap(phonemeIdMap);
    }

    /**
     * Determines whether text contains Devanagari characters (U+0900 - U+097F).
     */
    public static boolean containsDevanagari(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0900 && c <= 0x097F) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether text contains Ol Chiki characters (U+1C50 - U+1C7F).
     */
    public static boolean containsOlChiki(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x1C50 && c <= 0x1C7F) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalizes Santali Ol Chiki text:
     * - Unicode NFC normalization
     * - Converts Ol Chiki digits (᱐-᱙) into spoken Santhali words
     * - Cleans up multiple spaces and trims
     */
    public String normalizeSanthaliText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        for (Map.Entry<String, String> entry : OL_CHIKI_NUMERALS.entrySet()) {
            normalized = normalized.replace(entry.getKey(), " " + entry.getValue() + " ");
        }
        return normalized.replaceAll("\\s+", " ").trim();
    }

    /**
     * Translates Santali Ol Chiki text into an authentic standard IPA phoneme sequence
     * matching the Piper VITS neural acoustic model vocabulary.
     */
    public String santhaliToIpa(String text) {
        String normalized = normalizeSanthaliText(text);
        if (normalized.isEmpty()) {
            return "";
        }

        String result = normalized;

        // 1. Apply multi-character compound substitutions first
        for (String[] pair : COMPOUND_MAP) {
            result = result.replace(pair[0], pair[1]);
        }

        // 2. Apply single Ol Chiki character substitutions
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = result.length();
        while (i < len) {
            int codePoint = result.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            String ch = result.substring(i, i + charCount);

            if (SINGLE_CHAR_MAP.containsKey(ch)) {
                sb.append(SINGLE_CHAR_MAP.get(ch));
            } else {
                sb.append(ch);
            }
            i += charCount;
        }

        String resStr = sb.toString();
        // Clean formatting: normalize spaces
        resStr = resStr.replaceAll("\\s+", " ");
        // Ensure ASCII 'g' maps to IPA script 'ɡ' (U+0261)
        resStr = resStr.replace('g', 'ɡ');
        // Normalize duplicate periods
        resStr = resStr.replaceAll("\\.+", ".");
        return resStr.trim();
    }

    /**
     * Translates Hindi Devanagari text into an authentic standard IPA phoneme sequence
     * matching the Piper VITS neural acoustic model vocabulary.
     */
    public String hindiToIpa(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int n = normalized.length();

        while (i < n) {
            char ch = normalized.charAt(i);

            // Two-character nukta consonants
            if (i + 1 < n) {
                String pair = normalized.substring(i, i + 2);
                if (HINDI_CONSONANTS.containsKey(pair)) {
                    String consIpa = HINDI_CONSONANTS.get(pair);
                    i += 2;
                    if (i < n && normalized.charAt(i) == '्') {
                        sb.append(consIpa);
                        i++;
                    } else if (i < n && HINDI_MATRAS.containsKey(normalized.charAt(i))) {
                        sb.append(consIpa).append(HINDI_MATRAS.get(normalized.charAt(i)));
                        i++;
                    } else {
                        boolean isWordEnd = (i >= n || Character.isWhitespace(normalized.charAt(i)) || "।.,!?\n".indexOf(normalized.charAt(i)) >= 0);
                        sb.append(consIpa);
                        if (!isWordEnd) sb.append("ə");
                    }
                    continue;
                }
            }

            // Single consonants
            String singleStr = String.valueOf(ch);
            if (HINDI_CONSONANTS.containsKey(singleStr)) {
                String consIpa = HINDI_CONSONANTS.get(singleStr);
                i++;
                if (i < n && normalized.charAt(i) == '्') {
                    sb.append(consIpa);
                    i++;
                } else if (i < n && HINDI_MATRAS.containsKey(normalized.charAt(i))) {
                    sb.append(consIpa).append(HINDI_MATRAS.get(normalized.charAt(i)));
                    i++;
                } else {
                    boolean isWordEnd = (i >= n || Character.isWhitespace(normalized.charAt(i)) || "।.,!?\n".indexOf(normalized.charAt(i)) >= 0);
                    sb.append(consIpa);
                    if (!isWordEnd) sb.append("ə");
                }
                continue;
            }

            // Independent vowels
            if (HINDI_VOWELS.containsKey(ch)) {
                sb.append(HINDI_VOWELS.get(ch));
                i++;
                continue;
            }

            // Dependent matras (stray or compound)
            if (HINDI_MATRAS.containsKey(ch)) {
                sb.append(HINDI_MATRAS.get(ch));
                i++;
                continue;
            }

            // Anusvara / Chandrabindu
            if (ch == 'ं' || ch == 'ँ') {
                sb.append("̃");
                i++;
                continue;
            }

            // Visarga
            if (ch == 'ः') {
                sb.append("h");
                i++;
                continue;
            }

            // Danda punctuation
            if (ch == '।' || ch == '॥') {
                sb.append(".");
                i++;
                continue;
            }

            if (Character.isWhitespace(ch)) {
                sb.append(" ");
                i++;
                continue;
            }

            if (".,!?-".indexOf(ch) >= 0) {
                sb.append(ch);
                i++;
                continue;
            }

            // Default passthrough
            sb.append(ch);
            i++;
        }

        String resStr = sb.toString();
        resStr = resStr.replaceAll("\\s+", " ");
        resStr = resStr.replaceAll("\\.+", ".");
        return resStr.trim();
    }

    /**
     * Converts text (Santali Ol Chiki or Hindi Devanagari) into an atomic phoneme token sequence for Piper VITS.
     * Treats multi-character phonemes (e.g. aspirated plosives and combining diacritics) as atomic units
     * and places silence pad tokens ONLY after the complete atomic phoneme, preventing clicks and fragmented syllables.
     */
    public List<Long> textToPhonemeIds(String text) {
        List<Long> ids = new ArrayList<>();
        ids.add(bosId);

        if (text != null && !text.trim().isEmpty()) {
            String ipa;
            if (containsDevanagari(text)) {
                ipa = hindiToIpa(text);
            } else {
                ipa = santhaliToIpa(text);
            }

            int i = 0;
            int len = ipa.length();
            while (i < len) {
                // 1. Check for multi-character atomic IPA phonemes first
                boolean matchedMulti = false;
                for (String multiPhone : MULTI_CHAR_PHONEMES) {
                    if (ipa.startsWith(multiPhone, i)) {
                        for (int k = 0; k < multiPhone.length(); ) {
                            int cp = multiPhone.codePointAt(k);
                            int cc = Character.charCount(cp);
                            String sub = multiPhone.substring(k, k + cc);
                            if (phonemeIdMap.containsKey(sub)) {
                                ids.addAll(phonemeIdMap.get(sub));
                            }
                            k += cc;
                        }
                        ids.add(padId); // Pad token ONLY after the complete atomic phoneme
                        i += multiPhone.length();
                        matchedMulti = true;
                        break;
                    }
                }
                if (matchedMulti) {
                    continue;
                }

                int codePoint = ipa.codePointAt(i);
                int charCount = Character.charCount(codePoint);
                String ch = ipa.substring(i, i + charCount);

                if (phonemeIdMap.containsKey(ch)) {
                    ids.addAll(phonemeIdMap.get(ch));
                    ids.add(padId);
                } else if (Character.isWhitespace(codePoint)) {
                    if (phonemeIdMap.containsKey(" ")) {
                        ids.addAll(phonemeIdMap.get(" "));
                    }
                    ids.add(padId);
                } else {
                    if (phonemeIdMap.containsKey(ch)) {
                        ids.addAll(phonemeIdMap.get(ch));
                        ids.add(padId);
                    }
                }
                i += charCount;
            }
        }

        ids.add(eosId);
        return ids;
    }

    /**
     * Splits multi-sentence text safely on sentence delimiters
     * without breaking words or abbreviations.
     */
    public List<String> splitSentences(String text) {
        List<String> list = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return list;

        // Split on Santali delimiters (᱾, ᱿), Devanagari danda (।, ॥), or standard (. ? ! \n)
        String[] parts = text.split("(?<=[᱾᱿।॥\\.\\?\\!\\n])\\s*");
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        if (list.isEmpty() && !text.trim().isEmpty()) {
            list.add(text.trim());
        }
        return list;
    }

    /**
     * Synthesizes a single sentence to raw float PCM samples using Piper VITS ONNX session.
     */
    private float[] synthesizeSentenceSamples(String cleanSentence) throws Exception {
        List<Long> phonemeIds = textToPhonemeIds(cleanSentence);
        int seqLen = phonemeIds.size();
        if (seqLen <= 2) {
            return new float[0];
        }

        long[] inputIds = new long[seqLen];
        for (int i = 0; i < seqLen; i++) {
            inputIds[i] = phonemeIds.get(i);
        }

        OrtEnvironment env = ModelManager.getInstance().getEnvironment();
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), new long[]{1, seqLen});
        OnnxTensor lengthsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(new long[]{seqLen}), new long[]{1});
        OnnxTensor scalesTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(new float[]{noiseScale, lengthScale, noiseW}), new long[]{3});

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input", inputTensor);
        inputs.put("input_lengths", lengthsTensor);
        inputs.put("scales", scalesTensor);

        OrtSession.Result result = ttsSession.run(inputs);
        OnnxTensor outputTensor = (OnnxTensor) result.get(0);
        FloatBuffer fb = outputTensor.getFloatBuffer();
        int sampleCount = fb.remaining();
        float[] samples = new float[sampleCount];
        fb.get(samples);

        inputTensor.close();
        lengthsTensor.close();
        scalesTensor.close();
        outputTensor.close();
        result.close();

        return samples;
    }

    /**
     * Synthesizes Santali or Hindi text into a local 16kHz mono WAV audio file.
     * Uses sentence-based chunking for long text and applies true peak normalization
     * with trailing fade-out padding to prevent hardware buffer cutoff.
     *
     * @param text Input text (Santali Ol Chiki or Hindi Devanagari).
     * @param outputFile Destination WAV file.
     * @return File referencing generated WAV, or null if synthesis failed.
     */
    public synchronized File synthesize(String text, File outputFile) {
        if (!isAvailable()) {
            Log.w(TAG, "Cannot synthesize: TTS is not available.");
            return null;
        }

        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Empty input text for TTS.");
            return null;
        }

        long t0 = System.currentTimeMillis();
        String cleanText = text.trim();

        Log.i(TAG, "Synthesizing text [" + cleanText.length() + " chars]: '" + cleanText + "'");

        try {
            List<String> sentences = splitSentences(cleanText);
            List<float[]> sentenceOutputs = new ArrayList<>();
            int totalSamples = 0;

            for (int s = 0; s < sentences.size(); s++) {
                float[] chunk = synthesizeSentenceSamples(sentences.get(s));
                if (chunk.length > 0) {
                    sentenceOutputs.add(chunk);
                    totalSamples += chunk.length;
                }
            }

            if (totalSamples == 0) {
                Log.w(TAG, "TTS generated 0 total audio samples.");
                return null;
            }

            // Trailing fade-out padding (80ms = 1280 samples) to prevent Android hardware buffer cutoff
            int trailingPadSamples = (int) (sampleRate * 0.08f);
            int finalSampleCount = totalSamples + trailingPadSamples;

            float[] allSamples = new float[finalSampleCount];
            int offset = 0;
            for (int s = 0; s < sentenceOutputs.size(); s++) {
                float[] chunk = sentenceOutputs.get(s);
                System.arraycopy(chunk, 0, allSamples, offset, chunk.length);
                offset += chunk.length;
            }

            // Apply smooth linear decay across trailing pad
            float lastVal = (totalSamples > 0) ? allSamples[totalSamples - 1] : 0.0f;
            for (int t = 0; t < trailingPadSamples; t++) {
                float fade = 1.0f - ((float) t / trailingPadSamples);
                allSamples[totalSamples + t] = lastVal * fade;
            }

            // Audio waveform statistics & true peak normalization
            float peakAmp = 0.0f;
            for (float val : allSamples) {
                float abs = Math.abs(val);
                if (abs > peakAmp) peakAmp = abs;
            }

            float targetPeak = 0.92f;
            float scaleFactor = (peakAmp > 0.01f) ? Math.min(3.5f, targetPeak / peakAmp) : 1.0f;

            short[] pcmData = new short[finalSampleCount];
            for (int i = 0; i < finalSampleCount; i++) {
                float val = allSamples[i] * scaleFactor;
                if (val > 1.0f) val = 1.0f;
                if (val < -1.0f) val = -1.0f;
                pcmData[i] = (short) (val * 32767.0f);
            }

            // Write 16kHz mono WAV file with physical storage sync
            writeWavFile(pcmData, sampleRate, outputFile);

            float audioDurationSec = (float) finalSampleCount / sampleRate;
            long totalDuration = System.currentTimeMillis() - t0;
            Log.i(TAG, String.format("TTS synthesis finished in %d ms | Audio: %.2fs (%d sentences, peak: %.2f) | File: %d bytes",
                    totalDuration, audioDurationSec, sentences.size(), peakAmp * scaleFactor, outputFile.length()));

            if (outputFile.exists() && outputFile.length() > 44 && audioDurationSec > 0.1f) {
                return outputFile;
            } else {
                Log.e(TAG, "Generated audio file validation failed.");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during TTS synthesis", e);
            return null;
        }
    }

    /**
     * Synthesizes Hindi text to a local WAV audio file.
     * Uses offline on-device Piper neural TTS with Hindi G2P, with automatic
     * fallback to Android offline TextToSpeech.
     */
    public synchronized boolean synthesizeHindi(String hindiText, File outputFile) {
        if (hindiText == null || hindiText.trim().isEmpty()) {
            return false;
        }

        // 1. Try offline on-device Piper neural TTS with Hindi G2P first
        if (isAvailable()) {
            File generated = synthesize(hindiText, outputFile);
            if (generated != null && generated.exists() && generated.length() > 44) {
                return true;
            }
        }

        // 2. Fallback to Android system TextToSpeech if available
        if (hindiTtsEngine == null || !isHindiTtsAvailable) {
            Log.w(TAG, "Hindi TTS engine is not available.");
            return false;
        }

        try {
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            String utteranceId = "hi_tts_" + System.currentTimeMillis();

            hindiTtsEngine.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override
                public void onStart(String id) {}

                @Override
                public void onDone(String id) {
                    if (utteranceId.equals(id)) {
                        latch.countDown();
                    }
                }

                @Override
                public void onError(String id) {
                    if (utteranceId.equals(id)) {
                        latch.countDown();
                    }
                }
            });

            File parent = outputFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            int res = hindiTtsEngine.synthesizeToFile(hindiText, null, outputFile, utteranceId);
            if (res == android.speech.tts.TextToSpeech.SUCCESS) {
                boolean completed = latch.await(4, java.util.concurrent.TimeUnit.SECONDS);
                return completed && outputFile.exists() && outputFile.length() > 44;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during Hindi TTS synthesis fallback", e);
        }
        return false;
    }

    /**
     * Direct speak convenience method for offline Hindi playback.
     */
    public synchronized void speakHindi(String hindiText) {
        if (hindiTtsEngine != null && isHindiTtsAvailable && hindiText != null) {
            hindiTtsEngine.speak(hindiText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "hi_speak_" + System.currentTimeMillis());
        }
    }

    public boolean isHindiAvailable() {
        return isAvailable() || (isHindiTtsAvailable && hindiTtsEngine != null);
    }

    /**
     * Universal synthesis method respecting language direction.
     */
    public synchronized File synthesizeForLanguage(String text, String langCode, File outputFile) {
        return synthesize(text, outputFile);
    }

    /**
     * Writes 16-bit PCM samples to a standard 44-byte RIFF WAV file.
     * Ensures fd.sync() is called so playback only starts when file is completely closed.
     */
    private void writeWavFile(short[] pcm, int sRate, File wavFile) throws IOException {
        File parent = wavFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        int channels = 1;
        long totalAudioLen = pcm.length * 2L;
        long totalDataLen = totalAudioLen + 36;
        long byteRate = (long) sRate * channels * 2;

        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (sRate & 0xff);
        header[25] = (byte) ((sRate >> 8) & 0xff);
        header[26] = (byte) ((sRate >> 16) & 0xff);
        header[27] = (byte) ((sRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2); header[33] = 0;
        header[34] = 16; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        try (FileOutputStream fos = new FileOutputStream(wavFile)) {
            fos.write(header, 0, 44);
            byte[] buffer = new byte[4096];
            int bufIdx = 0;
            for (short sample : pcm) {
                buffer[bufIdx++] = (byte) (sample & 0xff);
                buffer[bufIdx++] = (byte) ((sample >> 8) & 0xff);
                if (bufIdx >= buffer.length) {
                    fos.write(buffer, 0, bufIdx);
                    bufIdx = 0;
                }
            }
            if (bufIdx > 0) {
                fos.write(buffer, 0, bufIdx);
            }
            fos.flush();
            try {
                fos.getFD().sync(); // Ensure physical flush to storage before any player touches it
            } catch (Exception ignored) {}
        }
    }

    public synchronized void close() {
        ModelManager.getInstance().closeSession("models/tts/sat_piper_model.onnx");
        ttsSession = null;
        if (hindiTtsEngine != null) {
            try {
                hindiTtsEngine.stop();
                hindiTtsEngine.shutdown();
            } catch (Exception ignored) {}
            hindiTtsEngine = null;
        }
        isInitialized = false;
        isHindiTtsAvailable = false;
    }
}
