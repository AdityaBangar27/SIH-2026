import os
import sys
import time
from pathlib import Path
import soundfile as sf
import numpy as np

# Force UTF-8 on Windows
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

from src.asr import OfflineASRManager
from src.tts import OfflineTTSManager

AUDIO_TEST_DIR = ROOT_DIR / "audio" / "test_durations"
AUDIO_TEST_DIR.mkdir(parents=True, exist_ok=True)


def generate_test_audio(text: str, filename: str) -> str:
    """Generates synthetic reference Hindi speech for specific sentence lengths."""
    target_path = AUDIO_TEST_DIR / filename
    tts = OfflineTTSManager.get_instance()
    res = tts.synthesize_hindi(text, output_filename=f"ref_{filename}")
    if res.get("audio_path") and os.path.exists(res["audio_path"]):
        import shutil
        shutil.copy(res["audio_path"], str(target_path))
    return str(target_path)


def run_duration_tests():
    print("==================================================")
    print("TEST: Full Audio Hindi ASR Across Variable Durations")
    print("==================================================")

    asr = OfflineASRManager.get_instance()
    asr.load_hindi_asr()

    test_cases = [
        ("A. Short (~1-2 sec)", "नमस्ते।", "test_1s.wav"),
        ("B. Medium (~3 sec)", "मेरा भारत महान है।", "test_3s.wav"),
        ("C. Target (~5-6 sec)", "मेरा भारत महान, महान मेरा देश महान।", "test_5s.wav"),
        ("D. Long (~8-10 sec)", "हम सब मिलकर अपने देश के विकास के लिए काम करेंगे और शिक्षा का प्रसार करेंगे।", "test_8s.wav"),
    ]

    for label, sentence, fname in test_cases:
        print(f"\n--- {label} ---")
        print(f"Ground Truth Hindi: '{sentence}'")
        
        # 1. Generate or retrieve test audio
        wav_path = generate_test_audio(sentence, fname)
        info = sf.info(wav_path)
        input_dur = info.duration

        # 2. Transcribe with ASR
        t0 = time.time()
        res = asr.transcribe_hindi(wav_path, mode="full")
        inf_time = time.time() - t0

        diag = res.get("diagnostics", {})
        proc_dur = diag.get("processed_duration", res["audio_duration"])

        print(f"Input duration:     {input_dur:.2f} sec")
        print(f"Processed duration: {proc_dur:.2f} sec")
        print(f"Recognized text:    '{res['text']}'")
        print(f"Inference time:     {inf_time:.2f} sec")

        # 3. Validations
        dur_diff = abs(input_dur - proc_dur)
        print(f"Duration Match:     {'✓ PERFECT' if dur_diff < 0.2 else '❌ MISMATCH'}")
        
        # Assertions
        assert dur_diff < 0.2, f"Processed duration ({proc_dur}s) must match Input duration ({input_dur}s)!"
        assert len(res["text"].strip()) > 0, "Recognized text must not be empty!"

    print("\n==================================================")
    print("✓ ALL DURATION TESTS PASSED: NO 1-SECOND TRUNCATION")
    print("==================================================")


if __name__ == "__main__":
    run_duration_tests()
