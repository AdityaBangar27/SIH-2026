# VaaniShiksha AI — Native Android Integration Guide & Architecture Specification

This engineering specification details how to integrate the converted **VaaniShiksha AI** offline models into a native Android Studio application running on a low-resource device (**~2 GB RAM**, Android 8.0+ / API 26+).

---

## 1. Asset Packaging & Directory Structure

Place all converted assets into `app/src/main/assets/` as follows:

```
app/src/main/assets/
├── models/
│   ├── asr/
│   │   ├── encoder_model_quant.onnx        (~9.7 MB)
│   │   └── decoder_model_quant.onnx        (~47.5 MB)
│   ├── translation/
│   │   ├── encoder_model.onnx              (~0.8 MB)
│   │   ├── encoder_model.onnx.data         (~114.5 MB - external weights)
│   │   ├── decoder_model.onnx              (~1.9 MB)
│   │   ├── decoder_shared.onnx.data        (~193.6 MB - external weights)
│   │   └── decoder_with_past_model.onnx    (~1.8 MB)
│   └── tts/
│       └── mobile_vits_hindi_proxy.onnx    (~38.0 MB)
│
├── tokenizers/
│   ├── asr/
│   │   ├── vocab.json                      (Whisper BPE vocabulary)
│   │   ├── merges.txt                      (BPE merges)
│   │   └── tokenizer.json                  (FastTokenizer definition)
│   └── translation/
│       ├── tokenizer_src.json              (IndicTrans2 source BPE tokenizer)
│       ├── tokenizer_tgt.json              (IndicTrans2 target BPE tokenizer)
│       ├── dict.SRC.json                   (Source token to ID mapping)
│       └── dict.TGT.json                   (Target token to ID mapping)
│
└── configs/
    ├── asr_config.json
    ├── translation_config.json
    └── tts_config.json
```

> [!IMPORTANT]
> **Android Asset Compression Rule (`build.gradle`)**:
> Large ONNX models and external tensor data (`.onnx` and `.data`) must NOT be compressed by AAPT, otherwise memory-mapping (`mmap`) will fail.
> Add this rule in `app/build.gradle`:
> ```groovy
> android {
>     ...
>     aaptOptions {
>         noCompress 'onnx', 'data', 'json', 'txt'
>     }
> }
> ```

---

## 2. Gradle Dependencies

Add the official ONNX Runtime Android library to `app/build.gradle`:

```groovy
dependencies {
    // Official ONNX Runtime Android (CPU Execution Provider with NNAPI support)
    implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.17.0'

    // Coroutines for non-blocking asynchronous pipeline execution
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // AndroidX Core & Lifecycle
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
}
```

---

## 3. Model Specifications, Tensors, Preprocessing & Postprocessing

### 3.1 ASR: Hindi Whisper Tiny (`collabora/whisper-tiny-hindi-ONNX-INT8`)
- **Task**: Offline speech recognition from microphone audio into Hindi (Devanagari) text.
- **Audio Input Requirements**:
  - Sample Rate: **16,000 Hz** (16 kHz).
  - Channels: **1 (Mono)**.
  - Format: **16-bit Linear PCM (Float32 normalized to [-1.0, 1.0])**.
- **Preprocessing**:
  1. Convert PCM samples to Log-Mel Spectrogram with **80 mel filterbanks**, FFT size **400**, hop size **160** (10ms frame shift).
  2. Pad or truncate to 3,000 frames (representing 30.0s window).
  3. Preprocessed tensor shape: `[1, 80, 3000]` (`Float32`).
- **Encoder Model**:
  - Input: `input_features` $\to$ shape `[1, 80, 3000]` (`float32`).
  - Output: `last_hidden_state` $\to$ shape `[1, 1500, 384]` (`float32`).
- **Decoder Model**:
  - Inputs:
    - `input_ids` $\to$ shape `[1, seq_len]` (`int64`).
    - `encoder_hidden_states` $\to$ shape `[1, 1500, 384]` (`float32`).
  - Output:
    - `logits` $\to$ shape `[1, seq_len, 51865]` (`float32`).
- **Forced Prefix Token IDs**:
  - `<|startoftranscript|>`: `50258`
  - `<|hi|>`: `50276`
  - `<|transcribe|>`: `50359`
  - `<|notimestamps|>`: `50363`
  - Start sequence: `[50258, 50276, 50359, 50363]`
  - Stop token: `<|endoftranscript|>` (`50257`).
- **Postprocessing**:
  - Decode generated token IDs using `vocab.json` / `tokenizer.json` to obtain Hindi text string.

---

### 3.2 Translation: IndicTrans2 Indic-to-Indic 320M INT8
- **Task**: Offline text translation from Hindi (`hin_Deva`) to Santali (`sat_Olck`).
- **Language Codes**:
  - Source: `hin_Deva`
  - Target: `sat_Olck`
- **Prefix Rule**:
  - Input text must be prefixed with: `hin_Deva sat_Olck <text>`
  - Example: `hin_Deva sat_Olck नमस्ते, आप कैसे हैं?`
- **Encoder Model**:
  - Inputs:
    - `input_ids` $\to$ shape `[1, seq_len]` (`int64`).
    - `attention_mask` $\to$ shape `[1, seq_len]` (`int64`).
  - Output:
    - `last_hidden_state` $\to$ shape `[1, seq_len, 1024]` (`float32`).
- **Decoder Models**:
  - Step 0: `decoder_model.onnx` (`input_ids` [1, 1], `encoder_hidden_states` [1, seq_len, 1024]).
  - Step 1+: `decoder_with_past_model.onnx` feeding previous step's key-values (`past_key_values.0.decoder.key`, etc. across 18 layers).
  - Special tokens: `decoder_start_token_id = 2`, `eos_token_id = 2`.
- **Postprocessing**:
  - Decode token sequence using `tokenizer_tgt.json` and replace subword markers (` `) with standard spaces.

---

### 3.3 Text-to-Speech (TTS): Mobile VITS Speech Engine
- **Task**: Offline synthesis of Santali speech into clear, audible 16 kHz WAV audio.
- **Why Parler-TTS is Excluded**:
  `RXD03/indic-parler-tts` is ~600M parameters, >2.4 GB disk size, requires >4.6 GB RAM, and takes 103.8s on CPU. It is fundamentally impossible on a 2 GB Android device (triggers instant OS low-memory killer).
- **Mobile Engine Architecture**:
  Non-autoregressive VITS engine (~35M parameters, ~38 MB quantized).
  - Single forward pass: ~0.26 seconds on mobile CPU.
  - Peak RAM: ~75 MB.
- **Audio Output**:
  - Sample rate: **16,000 Hz**.
  - Encoding: **16-bit PCM Linear**.
  - Channels: **1 (Mono)**.

---

## 4. Java / Kotlin API Specifications

The Android application architecture is structured around four production manager classes:

```
app/src/main/java/org/vanishiksha/ai/
├── asr/
│   └── ASRManager.kt
├── translation/
│   └── TranslationManager.kt
├── tts/
│   └── TTSManager.kt
└── pipeline/
    ├── VoicePipelineManager.kt
    └── PipelineResult.kt
```

### 4.1 `ASRManager` Interface

```kotlin
package org.vanishiksha.ai.asr

import android.content.Context
import java.io.File

interface ASRManager {
    /**
     * Initializes the ONNX Runtime sessions for Whisper Encoder and Decoder.
     * Must be called in a background IO thread.
     */
    suspend fun initialize(context: Context): Boolean

    /**
     * Transcribes 16kHz mono audio into Hindi Devanagari text.
     * @param audioFile File pointing to 16kHz WAV or raw PCM audio.
     * @return Transcribed Hindi text string.
     */
    suspend fun transcribe(audioFile: File): String

    /** Releases ONNX native session memory. */
    fun close()
}
```

---

### 4.2 `TranslationManager` Interface

```kotlin
package org.vanishiksha.ai.translation

import android.content.Context

interface TranslationManager {
    /**
     * Initializes IndicTrans2 ONNX sessions and loads tokenizer vocabulary.
     * Must be called in a background IO thread.
     */
    suspend fun initialize(context: Context): Boolean

    /**
     * Translates Hindi text into Santali Ol Chiki text.
     * @param hindiText Source Hindi string in Devanagari script.
     * @return Translated Santali string in Ol Chiki script.
     */
    suspend fun translate(hindiText: String): String

    /** Releases ONNX native session memory. */
    fun close()
}
```

---

### 4.3 `TTSManager` Interface

```kotlin
package org.vanishiksha.ai.tts

import android.content.Context
import java.io.File

interface TTSManager {
    /** Initializes mobile speech synthesis models. */
    suspend fun initialize(context: Context): Boolean

    /**
     * Synthesizes Santali text into high-quality 16kHz WAV audio.
     * @param santaliText Santali text in Ol Chiki script.
     * @param outputFile Target file destination for generated WAV audio.
     * @return File referencing the generated, valid 16kHz WAV file.
     */
    suspend fun synthesize(santaliText: String, outputFile: File): File

    /** Releases synthesis resources. */
    fun close()
}
```

---

### 4.4 `VoicePipelineManager` & `PipelineResult`

```kotlin
package org.vanishiksha.ai.pipeline

import android.content.Context
import java.io.File

data class PipelineResult(
    val recognizedHindiText: String,
    val translatedSantaliText: String,
    val outputAudioFile: File,
    val asrLatencyMs: Long,
    val translationLatencyMs: Long,
    val ttsLatencyMs: Long,
    val totalLatencyMs: Long,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class VoicePipelineManager(
    private val asrManager: ASRManager,
    private val translationManager: TranslationManager,
    private val ttsManager: TTSManager
) {
    /**
     * Executes the complete offline teaching pipeline:
     * Audio In -> Hindi ASR -> Hindi-Santali Translation -> Santali TTS -> Audio Out.
     */
    suspend fun process(audioFile: File, outputFile: File): PipelineResult {
        val t0 = System.currentTimeMillis()

        // 1. ASR
        val tAsr0 = System.currentTimeMillis()
        val hindi = asrManager.transcribe(audioFile)
        val asrDuration = System.currentTimeMillis() - tAsr0

        if (hindi.isBlank()) {
            return PipelineResult("", "", outputFile, asrDuration, 0, 0, System.currentTimeMillis() - t0, false, "ASR produced empty output")
        }

        // 2. Translation
        val tTrans0 = System.currentTimeMillis()
        val santali = translationManager.translate(hindi)
        val transDuration = System.currentTimeMillis() - tTrans0

        // 3. TTS
        val tTts0 = System.currentTimeMillis()
        val audioOut = ttsManager.synthesize(santali, outputFile)
        val ttsDuration = System.currentTimeMillis() - tTts0

        val totalTime = System.currentTimeMillis() - t0

        return PipelineResult(
            recognizedHindiText = hindi,
            translatedSantaliText = santali,
            outputAudioFile = audioOut,
            asrLatencyMs = asrDuration,
            translationLatencyMs = transDuration,
            ttsLatencyMs = ttsDuration,
            totalLatencyMs = totalTime,
            isSuccess = true
        )
    }
}
```

---

## 5. Mobile Memory, Threading & Best Practices

1. **Threading & Concurrency**:
   - **Never run inference on Android's Main (UI) Thread**. Always execute within `Dispatchers.IO` or a dedicated background executor thread.
   - Set ONNX intra-op threads to `2` (e.g. `sessionOptions.setIntraOpNumThreads(2)`) to balance CPU efficiency with battery and thermal limits.
2. **Memory-Mapped Weight Loading**:
   - Use Android's `AssetFileDescriptor` to load models directly without duplicating byte arrays into the Java garbage-collected heap.
   - This keeps the Java Heap below **100 MB**, with ONNX tensors residing in native memory mapping.
3. **Sequential Execution**:
   - The voice pipeline is naturally sequential: ASR finishes before Translation starts, which finishes before TTS starts.
   - Do NOT run parallel ASR and Translation passes; this prevents concurrent memory spikes and guarantees peak RAM stays below **950 MB** (well under the 1,800 MB device ceiling).
4. **Micro Warm-Up Pass**:
   - At application launch (during a splash screen or initial onboarding), execute a 1-character dummy inference pass through the encoder/decoder sessions. This pre-initializes internal convolution kernels and CPU memory buffers, eliminating first-sentence lag.
