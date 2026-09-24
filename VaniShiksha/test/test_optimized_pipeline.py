import os
import sys
import time
from pathlib import Path
import numpy as np
import torch

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

# Optimize PyTorch CPU threading
num_threads = max(1, min(8, os.cpu_count() or 4))
torch.set_num_threads(num_threads)
print(f"[Init] Set PyTorch CPU threads to {num_threads}", flush=True)

def log(stage, start_time, detail=""):
    elapsed = time.time() - start_time
    print(f"[{stage}] Completed in {elapsed:.3f} seconds {detail}", flush=True)
    return elapsed

print("\n" + "="*60, flush=True)
print("TESTING OPTIMIZED PIPELINE COMPONENTS", flush=True)
print("="*60, flush=True)

# 1. TRANSLATION
print("\n[TRANSLATION] Starting...", flush=True)
t0 = time.time()
from src.translator import OfflineIndicTranslator
translator = OfflineIndicTranslator.get_instance()
log("TRANSLATION LOAD", t0)

t0 = time.time()
trans_res = translator.hindi_to_santali("नमस्ते, आप कैसे हैं?")
t_trans = log("TRANSLATION INFERENCE", t0, f"Output: '{trans_res['translated_text']}'")

# 2. HINDI TTS (MMS-TTS)
print("\n[HINDI TTS] Starting...", flush=True)
t0 = time.time()
from src.tts import OfflineTTSManager
tts_mgr = OfflineTTSManager()
tts_mgr.load_hindi_tts()
log("HINDI TTS LOAD", t0)

t0 = time.time()
res_hi_tts = tts_mgr.synthesize_hindi("नमस्ते, आप कैसे हैं?")
t_hi_tts = log("HINDI TTS INFERENCE", t0, f"Audio: {res_hi_tts.get('audio_path')}")

# 3. OPTIMIZED HINDI ASR
print("\n[HINDI ASR] Starting...", flush=True)
t0 = time.time()
from transformers import WhisperForConditionalGeneration, WhisperProcessor
processor = WhisperProcessor.from_pretrained("collabora/whisper-tiny-hindi")
model = WhisperForConditionalGeneration.from_pretrained("collabora/whisper-tiny-hindi")
model.eval()
log("HINDI ASR LOAD", t0)

# Transcribe with optimized settings
import soundfile as sf
audio_path = res_hi_tts.get('audio_path')
audio, sr = sf.read(audio_path)
if audio.ndim > 1:
    audio = np.mean(audio, axis=1)
if sr != 16000:
    import scipy.signal
    audio = scipy.signal.resample(audio, int(len(audio) * 16000 / sr))
audio = audio.astype(np.float32)

# Simple energy-based silence trimming
energy = np.abs(audio)
thresh = np.max(energy) * 0.05 if np.max(energy) > 0 else 0.01
voiced_idx = np.where(energy > thresh)[0]
if len(voiced_idx) > 0:
    start_idx = max(0, voiced_idx[0] - int(0.1 * 16000))
    end_idx = min(len(audio), voiced_idx[-1] + int(0.1 * 16000))
    audio_trimmed = audio[start_idx:end_idx]
else:
    audio_trimmed = audio

print(f"[Audio] Original duration: {len(audio)/16000:.2f}s | Trimmed duration: {len(audio_trimmed)/16000:.2f}s", flush=True)

t0 = time.time()
inputs = processor(audio_trimmed, sampling_rate=16000, return_tensors="pt", return_attention_mask=True)
input_features = inputs.input_features
attention_mask = inputs.attention_mask

forced_decoder_ids = processor.get_decoder_prompt_ids(language="hi", task="transcribe")
with torch.no_grad():
    predicted_ids = model.generate(
        input_features,
        attention_mask=attention_mask,
        forced_decoder_ids=forced_decoder_ids,
        max_new_tokens=40,
        no_repeat_ngram_size=3,
        repetition_penalty=1.2,
    )

transcription = processor.batch_decode(predicted_ids, skip_special_tokens=True)[0].strip()
t_asr = log("HINDI ASR INFERENCE", t0, f"Recognized: '{transcription}'")

# 4. OPTIMIZED SANTALI TTS
print("\n[SANTALI PARLER-TTS] Starting...", flush=True)
t0 = time.time()
tts_mgr.load_santali_tts()
log("SANTALI TTS LOAD", t0)

santali_text = trans_res['translated_text']
print(f"[Santali TTS] Input text: '{santali_text}'", flush=True)

# Test Fast Mode (max_new_tokens=160 ~ 1.8s audio)
t0 = time.time()
desc = "A female speaker speaks Santali in Ol Chiki script clearly."
desc_ids = tts_mgr._santali_tokenizer(desc, return_tensors="pt").input_ids
prompt_ids = tts_mgr._santali_tokenizer(santali_text, return_tensors="pt").input_ids

with torch.no_grad():
    gen_fast = tts_mgr._santali_model.generate(
        input_ids=desc_ids,
        prompt_input_ids=prompt_ids,
        max_new_tokens=160,
        do_sample=False,
    )
audio_arr_fast = gen_fast.cpu().numpy().squeeze()
sr_sat = tts_mgr._santali_model.config.sampling_rate
out_fast = ROOT_DIR / "audio" / "tts_output" / "test_fast_santali.wav"
sf.write(str(out_fast), audio_arr_fast, sr_sat)
t_sat_fast = log("SANTALI TTS (FAST MODE)", t0, f"Duration: {len(audio_arr_fast)/sr_sat:.2f}s | Saved: {out_fast}")

print("\n" + "="*60, flush=True)
print(f"BENCHMARK SUMMARY (FAST DEMO MODE):", flush=True)
print(f"ASR:         {t_asr:.2f}s")
print(f"Translation: {t_trans:.2f}s")
print(f"Santali TTS: {t_sat_fast:.2f}s")
print(f"TOTAL:       {t_asr + t_trans + t_sat_fast:.2f}s")
print("="*60, flush=True)
