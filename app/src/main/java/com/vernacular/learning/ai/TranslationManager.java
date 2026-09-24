package com.vernacular.learning.ai;

import android.content.Context;
import android.util.Log;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Translation Manager for offline IndicTrans2 Hindi-to-Santali translation.
 * Source: Hindi (hin_Deva), Target: Santali (sat_Olck).
 */
public class TranslationManager {
    private static final String TAG = "TranslationManager";

    private OrtSession encoderSession;
    private OrtSession decoderSession;
    private OrtSession decoderWithPastSession;

    private final Map<String, Integer> srcDict = new HashMap<>();
    private final Map<Integer, String> tgtDict = new HashMap<>();

    private static final long DECODER_START_TOKEN_ID = 2L;
    private static final long EOS_TOKEN_ID = 2L;
    private static final long UNK_TOKEN_ID = 3L;
    private static final long HIN_DEVA_ID = 8L;
    private static final long SAT_OLCK_ID = 29925L;

    public synchronized boolean initialize(Context context) {
        try {
            ModelManager modelManager = ModelManager.getInstance();
            Log.i(TAG, "Initializing IndicTrans2 ONNX translation sessions...");

            encoderSession = modelManager.createSession(context, "models/translation/encoder_model.onnx");
            decoderSession = modelManager.createSession(context, "models/translation/decoder_model.onnx");
            try {
                decoderWithPastSession = modelManager.createSession(context, "models/translation/decoder_with_past_model.onnx");
            } catch (Exception e) {
                Log.w(TAG, "decoder_with_past_model session optional init note: " + e.getMessage());
            }

            loadDictionaries(context);
            Log.i(TAG, "Translation initialization complete. SRC dict: " + srcDict.size() + ", TGT dict: " + tgtDict.size());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TranslationManager", e);
            return false;
        }
    }

    public void loadDictionaries(InputStream srcIs, InputStream tgtIs) {
        if (srcIs != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[32768];
                int r;
                while ((r = srcIs.read(buf)) != -1) {
                    baos.write(buf, 0, r);
                }
                JSONObject jsonObject = new JSONObject(baos.toString(StandardCharsets.UTF_8.name()));
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String token = keys.next();
                    int id = jsonObject.getInt(token);
                    srcDict.put(token, id);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed loading dict.SRC stream", e);
            }
        }

        if (tgtIs != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[32768];
                int r;
                while ((r = tgtIs.read(buf)) != -1) {
                    baos.write(buf, 0, r);
                }
                JSONObject jsonObject = new JSONObject(baos.toString(StandardCharsets.UTF_8.name()));
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String token = keys.next();
                    int id = jsonObject.getInt(token);
                    tgtDict.put(id, token);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed loading dict.TGT stream", e);
            }
        }
    }

    private void loadDictionaries(Context context) {
        try (InputStream srcIs = context.getAssets().open("tokenizers/translation/dict.SRC.json");
             InputStream tgtIs = context.getAssets().open("tokenizers/translation/dict.TGT.json")) {
            loadDictionaries(srcIs, tgtIs);
        } catch (Exception e) {
            Log.w(TAG, "Failed opening dictionary assets", e);
        }
    }

    public List<Long> tokenizeSourceForTest(String input) {
        return tokenizeSource(input);
    }

    public String decodeTargetTokensForTest(List<Long> tokenIds) {
        return decodeTargetTokens(tokenIds);
    }

    /**
     * Translates Hindi Devanagari text into Santali Ol Chiki text.
     */
    public synchronized String translate(String hindiText) {
        if (hindiText == null || hindiText.trim().isEmpty()) {
            return "";
        }

        if (encoderSession == null || decoderSession == null) {
            Log.e(TAG, "TranslationManager not initialized.");
            return "";
        }

        try {
            OrtEnvironment env = ModelManager.getInstance().getEnvironment();

            // 1. Tokenize source Hindi text with SentencePiece BPE prefix
            List<Long> inputTokenIds = tokenizeSource(hindiText.trim());

            int seqLen = inputTokenIds.size();
            long[] inputIdsArr = new long[seqLen];
            long[] attentionMaskArr = new long[seqLen];
            for (int i = 0; i < seqLen; i++) {
                inputIdsArr[i] = inputTokenIds.get(i);
                attentionMaskArr[i] = 1L;
            }

            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIdsArr), new long[]{1, seqLen});
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMaskArr), new long[]{1, seqLen});

            Map<String, OnnxTensor> encoderInputs = new HashMap<>();
            encoderInputs.put("input_ids", inputIdsTensor);
            encoderInputs.put("attention_mask", attentionMaskTensor);

            // 2. Run Encoder
            OrtSession.Result encoderResult = encoderSession.run(encoderInputs);
            OnnxTensor encoderHiddenState = (OnnxTensor) encoderResult.get(0);

            // 3. Autoregressive Decoder Loop with KV cache acceleration
            List<Long> generatedTargetIds = new ArrayList<>();
            generatedTargetIds.add(DECODER_START_TOKEN_ID);

            int maxSteps = 64;
            long nextTokenId = DECODER_START_TOKEN_ID;
            OrtSession.Result pastResult = null;

            int numLayers = 18; // IndicTrans2 18 transformer layers

            for (int step = 0; step < maxSteps; step++) {
                OrtSession.Result decoderResult;
                OnnxTensor decInputIdsTensor;

                if (step == 0 || decoderWithPastSession == null || pastResult == null) {
                    decInputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(new long[]{DECODER_START_TOKEN_ID}), new long[]{1, 1});
                    Map<String, OnnxTensor> decoderInputs = new HashMap<>();
                    decoderInputs.put("input_ids", decInputIdsTensor);
                    decoderInputs.put("encoder_hidden_states", encoderHiddenState);
                    decoderInputs.put("encoder_attention_mask", attentionMaskTensor);

                    decoderResult = decoderSession.run(decoderInputs);
                    decInputIdsTensor.close();
                } else {
                    decInputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(new long[]{nextTokenId}), new long[]{1, 1});
                    Map<String, OnnxTensor> pastInputs = new HashMap<>();
                    pastInputs.put("input_ids", decInputIdsTensor);
                    pastInputs.put("encoder_attention_mask", attentionMaskTensor);

                    for (int l = 0; l < numLayers; l++) {
                        pastInputs.put("past_key_values." + l + ".decoder.key", (OnnxTensor) pastResult.get(l * 4 + 1));
                        pastInputs.put("past_key_values." + l + ".decoder.value", (OnnxTensor) pastResult.get(l * 4 + 2));
                        pastInputs.put("past_key_values." + l + ".encoder.key", (OnnxTensor) pastResult.get(l * 4 + 3));
                        pastInputs.put("past_key_values." + l + ".encoder.value", (OnnxTensor) pastResult.get(l * 4 + 4));
                    }

                    decoderResult = decoderWithPastSession.run(pastInputs);
                    decInputIdsTensor.close();
                }

                OnnxTensor logitsTensor = (OnnxTensor) decoderResult.get(0);
                float[][][] logits = (float[][][]) logitsTensor.getValue();
                float[] lastLogits = logits[0][logits[0].length - 1];

                // 3a. 3-gram repetition blocking to prevent cyclic translation loops
                if (generatedTargetIds.size() >= 4) {
                    long tMinus2 = generatedTargetIds.get(generatedTargetIds.size() - 2);
                    long tMinus1 = generatedTargetIds.get(generatedTargetIds.size() - 1);
                    for (int i = 1; i < generatedTargetIds.size() - 2; i++) {
                        if (generatedTargetIds.get(i) == tMinus2 && generatedTargetIds.get(i + 1) == tMinus1) {
                            long blockedToken = generatedTargetIds.get(i + 2);
                            if (blockedToken >= 0 && blockedToken < lastLogits.length) {
                                lastLogits[(int) blockedToken] = -1e9f;
                            }
                        }
                    }
                }

                nextTokenId = argmax(lastLogits);

                if (pastResult != null && pastResult != decoderResult) {
                    try { pastResult.close(); } catch (Exception ignored) {}
                }
                pastResult = decoderResult;

                if (nextTokenId == EOS_TOKEN_ID && step > 0) {
                    break;
                }

                generatedTargetIds.add(nextTokenId);
            }

            if (pastResult != null) {
                try { pastResult.close(); } catch (Exception ignored) {}
            }

            inputIdsTensor.close();
            attentionMaskTensor.close();
            encoderResult.close();

            return decodeTargetTokens(generatedTargetIds);

        } catch (Exception e) {
            Log.e(TAG, "Error during IndicTrans2 translation", e);
            return "";
        }
    }

    /**
     * Greedy SentencePiece BPE tokenizer mapping Hindi words into model token IDs.
     */
    private List<Long> tokenizeSource(String input) {
        List<Long> ids = new ArrayList<>();
        ids.add(HIN_DEVA_ID);
        ids.add(SAT_OLCK_ID);

        String[] words = input.split("\\s+");
        for (String w : words) {
            if (w.isEmpty()) continue;

            String candidate = "\u2581" + w;
            Integer fullId = srcDict.get(candidate);
            if (fullId != null) {
                ids.add((long) fullId);
            } else {
                // Longest greedy prefix matching
                int i = 0;
                boolean isFirst = true;
                while (i < w.length()) {
                    int bestLen = 0;
                    int bestId = (int) UNK_TOKEN_ID;
                    for (int j = w.length(); j > i; j--) {
                        String sub = (isFirst ? "\u2581" : "") + w.substring(i, j);
                        Integer subId = srcDict.get(sub);
                        if (subId != null) {
                            bestLen = j - i;
                            bestId = subId;
                            break;
                        }
                    }
                    if (bestLen > 0) {
                        ids.add((long) bestId);
                        i += bestLen;
                    } else {
                        ids.add(UNK_TOKEN_ID);
                        i += 1;
                    }
                    isFirst = false;
                }
            }
        }

        ids.add(EOS_TOKEN_ID);
        return ids;
    }

    private String decodeTargetTokens(List<Long> tokenIds) {
        StringBuilder sb = new StringBuilder();
        for (long id : tokenIds) {
            if (id == DECODER_START_TOKEN_ID || id == EOS_TOKEN_ID || id == UNK_TOKEN_ID || id <= 3L) {
                // Skip special tokens (<s>=0, <pad>=1, </s>=2, <unk>=3) matching HF skip_special_tokens=True
                continue;
            }
            String token = tgtDict.get((int) id);
            if (token != null) {
                if (token.equals("<unk>") || token.equals("<s>") || token.equals("</s>") || token.equals("<pad>")) {
                    continue;
                }
                token = token.replace("\u2581", " ").replace("@@", "");
                sb.append(token);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private long argmax(float[] array) {
        int maxIdx = 0;
        float maxVal = -Float.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > maxVal) {
                maxVal = array[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    public synchronized void close() {
        ModelManager.getInstance().closeSession("models/translation/encoder_model.onnx");
        ModelManager.getInstance().closeSession("models/translation/decoder_model.onnx");
        ModelManager.getInstance().closeSession("models/translation/decoder_with_past_model.onnx");
        encoderSession = null;
        decoderSession = null;
        decoderWithPastSession = null;
    }
}
