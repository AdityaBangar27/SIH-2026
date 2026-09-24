package com.vernacular.learning.ai;

import android.content.Context;
import android.util.Log;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Speech Recognition Manager using quantized Whisper Tiny Hindi ONNX models.
 * Runs completely offline on device CPU.
 */
public class ASRManager {
    private static final String TAG = "ASRManager";

    private OrtSession encoderSession;
    private OrtSession decoderSession;
    private final AudioProcessor audioProcessor;
    private final Map<Integer, String> idToTokenMap = new HashMap<>();

    // Whisper special forced prefix token IDs
    private static final long START_OF_TRANSCRIPT = 50258L;
    private static final long HI_LANG = 50276L;
    private static final long TRANSCRIBE_TASK = 50359L;
    private static final long NO_TIMESTAMPS = 50363L;
    private static final long END_OF_TRANSCRIPT = 50257L;

    public ASRManager() {
        this.audioProcessor = new AudioProcessor();
    }

    public synchronized boolean initialize(Context context) {
        try {
            ModelManager modelManager = ModelManager.getInstance();
            Log.i(TAG, "Initializing Whisper ASR encoder and decoder sessions...");

            encoderSession = modelManager.createSession(context, "models/asr/encoder_model.onnx");
            decoderSession = modelManager.createSession(context, "models/asr/decoder_model_quant.onnx");

            loadVocabulary(context);
            try (InputStream is = context.getAssets().open("tokenizers/asr/mel_filters.bin")) {
                audioProcessor.loadMelFilters(is);
            } catch (Exception e) {
                Log.w(TAG, "Could not load mel_filters.bin from assets", e);
            }

            Log.i(TAG, "ASR initialization complete. Vocab size: " + idToTokenMap.size());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ASRManager", e);
            return false;
        }
    }

    public void loadMelFilters(InputStream is) {
        audioProcessor.loadMelFilters(is);
    }

    public void loadVocabulary(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[32768];
            int r;
            while ((r = is.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            JSONObject jsonObject = new JSONObject(baos.toString(StandardCharsets.UTF_8.name()));
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String token = keys.next();
                int id = jsonObject.getInt(token);
                idToTokenMap.put(id, token);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading ASR vocabulary stream", e);
        }
    }

    private void loadVocabulary(Context context) {
        try (InputStream is = context.getAssets().open("tokenizers/asr/vocab.json")) {
            loadVocabulary(is);
        } catch (Exception e) {
            Log.e(TAG, "Error opening ASR vocabulary asset", e);
        }
    }

    public String decodeTokenSequence(List<Long> tokenIds) {
        return decodeTokens(tokenIds);
    }

    /**
     * Transcribes audio file into Hindi Devanagari text.
     */
    public synchronized String transcribe(File audioFile) {
        if (audioFile == null || !audioFile.exists()) {
            return "";
        }
        float[] pcmSamples = audioProcessor.readWavPcmSamples(audioFile);
        return transcribe(pcmSamples);
    }

    /**
     * Transcribes 16kHz Float32 PCM audio samples.
     */
    public synchronized String transcribe(float[] pcmSamples) {
        if (encoderSession == null || decoderSession == null) {
            Log.e(TAG, "ASRManager not initialized.");
            return "";
        }

        if (pcmSamples == null || pcmSamples.length == 0) {
            return "";
        }

        try {
            OrtEnvironment env = ModelManager.getInstance().getEnvironment();

            // 1. Generate 80-mel log-spectrogram [1, 80, 3000]
            float[] melFeatures = audioProcessor.generateMelSpectrogram(pcmSamples);
            long[] melShape = new long[]{1, 80, 3000};
            OnnxTensor inputFeaturesTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(melFeatures), melShape);

            // 2. Run Encoder
            OrtSession.Result encoderResult = encoderSession.run(Collections.singletonMap("input_features", inputFeaturesTensor));
            OnnxTensor lastHiddenStateTensor = (OnnxTensor) encoderResult.get(0);

            // 3. Autoregressive Greedy Decoder with Repetition Penalty & Early Stopping
            List<Long> generatedTokenIds = new ArrayList<>();
            generatedTokenIds.add(START_OF_TRANSCRIPT);
            generatedTokenIds.add(HI_LANG);
            generatedTokenIds.add(TRANSCRIBE_TASK);
            generatedTokenIds.add(NO_TIMESTAMPS);

            int maxNewTokens = 128;
            for (int step = 0; step < maxNewTokens; step++) {
                int seqLen = generatedTokenIds.size();
                long[] seqArray = new long[seqLen];
                for (int i = 0; i < seqLen; i++) {
                    seqArray[i] = generatedTokenIds.get(i);
                }

                OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(seqArray), new long[]{1, seqLen});

                Map<String, OnnxTensor> decoderInputs = new HashMap<>();
                decoderInputs.put("input_ids", inputIdsTensor);
                decoderInputs.put("encoder_hidden_states", lastHiddenStateTensor);

                OrtSession.Result decoderResult = decoderSession.run(decoderInputs);
                OnnxTensor logitsTensor = (OnnxTensor) decoderResult.get(0);

                float[][][] logits = (float[][][]) logitsTensor.getValue();
                float[] lastTokenLogits = logits[0][seqLen - 1];

                long nextTokenId = argmax(lastTokenLogits);

                inputIdsTensor.close();
                logitsTensor.close();
                decoderResult.close();

                // Stop if EOS or timestamp token (>= 50364) reached
                if (nextTokenId == END_OF_TRANSCRIPT || nextTokenId >= 50364L) {
                    break;
                }

                generatedTokenIds.add(nextTokenId);

                // Early break if an autoregressive repetition loop of k >= 8 tokens occurred
                int genLen = generatedTokenIds.size() - 4; // exclude 4 prompt tokens
                boolean repetitionFound = false;
                if (genLen >= 16) {
                    for (int k = 8; k <= genLen / 2; k++) {
                        boolean match = true;
                        int end = generatedTokenIds.size();
                        for (int j = 0; j < k; j++) {
                            if (!generatedTokenIds.get(end - 2 * k + j).equals(generatedTokenIds.get(end - k + j))) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            for (int r = 0; r < k; r++) {
                                generatedTokenIds.remove(generatedTokenIds.size() - 1);
                            }
                            repetitionFound = true;
                            break;
                        }
                    }
                }
                if (repetitionFound) {
                    break;
                }
            }

            inputFeaturesTensor.close();
            encoderResult.close();

            // 4. Decode generated token sequence to text string
            return decodeTokens(generatedTokenIds);

        } catch (Exception e) {
            Log.e(TAG, "Error during ASR transcription", e);
            return "";
        }
    }

    private long argmax(float[] array) {
        int bestIdx = 0;
        float maxVal = -Float.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            // Suppress prompt / special control tokens (50258..50363)
            // Allow EOS (50257) and timestamp tokens (>= 50364)
            if (i >= 50258 && i < 50364) continue;
            if (array[i] > maxVal) {
                maxVal = array[i];
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private static final byte[] BYTE_DECODER = new byte[350];
    private static final boolean[] HAS_BYTE = new boolean[350];

    static {
        List<Integer> bs = new ArrayList<>();
        for (int c = '!'; c <= '~'; c++) bs.add(c);
        for (int c = '¡'; c <= '¬'; c++) bs.add(c);
        for (int c = '®'; c <= 'ÿ'; c++) bs.add(c);

        List<Integer> cs = new ArrayList<>(bs);
        int n = 0;
        for (int b = 0; b < 256; b++) {
            if (!bs.contains(b)) {
                bs.add(b);
                cs.add(256 + n);
                n++;
            }
        }
        for (int i = 0; i < bs.size(); i++) {
            int charCode = cs.get(i);
            int byteVal = bs.get(i);
            if (charCode < BYTE_DECODER.length) {
                BYTE_DECODER[charCode] = (byte) byteVal;
                HAS_BYTE[charCode] = true;
            }
        }
    }

    private String decodeTokens(List<Long> tokenIds) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (long id : tokenIds) {
            if (id == START_OF_TRANSCRIPT || id == HI_LANG || id == TRANSCRIBE_TASK || id == NO_TIMESTAMPS || id == END_OF_TRANSCRIPT) {
                continue;
            }
            if (id >= 50257L) {
                continue;
            }
            String token = idToTokenMap.get((int) id);
            if (token != null && !(token.startsWith("<|") && token.endsWith("|>"))) {
                for (int i = 0; i < token.length(); i++) {
                    char c = token.charAt(i);
                    if (c < HAS_BYTE.length && HAS_BYTE[c]) {
                        baos.write(BYTE_DECODER[c] & 0xFF);
                    }
                }
            }
        }
        String text = new String(baos.toByteArray(), StandardCharsets.UTF_8).trim();
        return removeConsecutiveRepetitions(text);
    }

    private String removeConsecutiveRepetitions(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] words = text.split("\\s+");
        if (words.length < 6) return text;

        List<String> wordList = new ArrayList<>(Arrays.asList(words));
        boolean changed = true;
        while (changed) {
            changed = false;
            int n = wordList.size();
            for (int k = n / 2; k >= 3; k--) {
                for (int i = 0; i <= n - 2 * k; i++) {
                    boolean match = true;
                    for (int j = 0; j < k; j++) {
                        String w1 = wordList.get(i + j).replaceAll("[\\!\\?\\.\\,।॥]+", "");
                        String w2 = wordList.get(i + k + j).replaceAll("[\\!\\?\\.\\,।॥]+", "");
                        if (!w1.equalsIgnoreCase(w2)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        for (int r = 0; r < k; r++) {
                            wordList.remove(i + k);
                        }
                        changed = true;
                        break;
                    }
                }
                if (changed) break;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordList.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(wordList.get(i));
        }
        return sb.toString();
    }

    public synchronized void close() {
        ModelManager.getInstance().closeSession("models/asr/encoder_model.onnx");
        ModelManager.getInstance().closeSession("models/asr/decoder_model_quant.onnx");
        encoderSession = null;
        decoderSession = null;
    }
}
