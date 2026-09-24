import os
import sys
import time
from pathlib import Path

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

def log(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)

log("Starting debug benchmark...")

# Test 1: Translation
log("\n[1] Testing Translation...")
t0 = time.time()
from src.translator import OfflineIndicTranslator
trans = OfflineIndicTranslator.get_instance()
log(f"[1.1] Translator loaded in {time.time()-t0:.2f}s")

t0 = time.time()
res_trans = trans.hindi_to_santali("नमस्ते, आप कैसे हैं?")
log(f"[1.2] Translation output: '{res_trans['translated_text']}' in {time.time()-t0:.2f}s")

# Test 2: Hindi TTS (MMS-TTS)
log("\n[2] Testing Hindi MMS-TTS...")
t0 = time.time()
from src.tts import OfflineTTSManager
tts = OfflineTTSManager()
ok = tts.load_hindi_tts()
log(f"[2.1] Hindi TTS loaded: {ok} in {time.time()-t0:.2f}s")

t0 = time.time()
res_hi_tts = tts.synthesize_hindi("नमस्ते, आप कैसे हैं?")
log(f"[2.2] Hindi TTS synth: success={res_hi_tts['success']} latency={res_hi_tts.get('latency', 0):.2f}s")

# Test 3: Hindi ASR (collabora/whisper-tiny-hindi)
log("\n[3] Testing Hindi Whisper ASR...")
t0 = time.time()
from src.asr import OfflineASRManager
asr = OfflineASRManager()
ok = asr.load_hindi_asr()
log(f"[3.1] Hindi ASR loaded: {ok} in {time.time()-t0:.2f}s")

if res_hi_tts.get('audio_path') and os.path.exists(res_hi_tts['audio_path']):
    t0 = time.time()
    res_hi_asr = asr.transcribe_hindi(res_hi_tts['audio_path'])
    log(f"[3.2] Hindi ASR transcribe: '{res_hi_asr['text']}' in {time.time()-t0:.2f}s")

# Test 4: Santali TTS (Indic Parler-TTS)
log("\n[4] Testing Santali Indic Parler-TTS...")
t0 = time.time()
ok = tts.load_santali_tts()
log(f"[4.1] Santali TTS loaded: {ok} in {time.time()-t0:.2f}s")

if ok:
    test_sat_text = res_trans['translated_text'] or "ᱡᱚᱦᱟᱨ"
    log(f"[4.2] Testing Santali TTS with text: '{test_sat_text}'...")
    t0 = time.time()
    res_sat_tts = tts.synthesize_santali(test_sat_text)
    log(f"[4.3] Santali TTS synth: success={res_sat_tts['success']} latency={res_sat_tts.get('latency', 0):.2f}s")
    if not res_sat_tts['success']:
        log(f"[4.3 Error] {res_sat_tts.get('error')}")

log("\nBenchmark complete!")
