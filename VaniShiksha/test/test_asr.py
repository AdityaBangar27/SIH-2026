import os
import sys
import time
from pathlib import Path
import numpy as np
import soundfile as sf

# Ensure UTF-8 console output on Windows
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

from src.asr import OfflineASRManager
from src.tts import OfflineTTSManager


def main():
    print("====================================")
    print("VaaniShiksha AI: Offline ASR Test")
    print("====================================")

    asr = OfflineASRManager.get_instance()
    tts = OfflineTTSManager.get_instance()

    # 1. Test Hindi ASR
    print("\n[1] Testing Hindi Whisper ASR (collabora/whisper-tiny-hindi)...")
    audio_dir = ROOT_DIR / "audio" / "test"
    audio_dir.mkdir(parents=True, exist_ok=True)
    hindi_test_wav = audio_dir / "hindi_asr_test.wav"

    tts_res = tts.synthesize_hindi("नमस्ते आप कैसे हैं", output_filename="hindi_asr_test.wav")
    if tts_res.get("audio_path") and os.path.exists(tts_res["audio_path"]):
        t0 = time.time()
        res = asr.transcribe_hindi(tts_res["audio_path"], mode="fast")
        lat = time.time() - t0
        print(f"Recognized Hindi: '{res['text']}'")
        print(f"Latency: {lat:.2f}s | Script: {res['script']}")
        assert res["text"].strip(), "Hindi ASR transcription must not be empty"
        print("✓ Hindi ASR OK")

    print("\n====================================")
    print("ALL ASR TESTS COMPLETED")
    print("====================================")


if __name__ == "__main__":
    main()
