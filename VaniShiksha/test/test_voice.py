import os
import sys
import time
from pathlib import Path
import soundfile as sf

# Ensure UTF-8 console output on Windows
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

from src.hardware import print_system_startup
from src.voice_pipeline import VoiceTranslationPipeline
from src.tts import OfflineTTSManager
from src.audio_validator import validate_audio_file

AUDIO_TEST_DIR = ROOT_DIR / "audio" / "test"


def prepare_reference_audio() -> str:
    """Prepares reference Hindi test audio for: 'मुझे कल बाजार जाना है।'"""
    AUDIO_TEST_DIR.mkdir(parents=True, exist_ok=True)
    hindi_sample_path = AUDIO_TEST_DIR / "hindi_bazaar_test.wav"

    tts = OfflineTTSManager.get_instance()
    res = tts.synthesize_hindi("मुझे कल बाजार जाना है।", output_filename="hindi_bazaar_ref.wav")
    if res.get("audio_path") and os.path.exists(res["audio_path"]):
        import shutil
        shutil.copy(res["audio_path"], str(hindi_sample_path))
        print(f"[Setup] Reference Hindi audio created: {hindi_sample_path}", flush=True)

    return str(hindi_sample_path)


def main():
    print("=" * 60)
    print("VaaniShiksha AI: End-to-End Voice Translation Pipeline Test")
    print("=" * 60)
    
    # 1. System & Hardware Detection
    print_system_startup()

    # 2. Initialize & Preload Models (Measure COLD START)
    print("\n--- COLD START: Model Loading ---")
    t_cold_0 = time.time()
    pipeline = VoiceTranslationPipeline.get_instance()
    preload_status = pipeline.preload_all()
    cold_start_time = time.time() - t_cold_0
    print(f"Cold Start Time (Model Loading): {cold_start_time:.2f}s\n")

    # 3. Prepare reference audio
    test_audio = prepare_reference_audio()

    # 4. Run End-to-End Test (Measure WARM INFERENCE)
    print("\n--- WARM INFERENCE: Voice Pipeline Execution ---")
    print("Input Speech: Hindi WAV ('मुझे कल बाजार जाना है।')")
    
    t_warm_0 = time.time()
    res = pipeline.run(
        test_audio,
        source_language="Hindi",
        target_language="Santali",
        mode="fast"
    )
    total_warm_time = time.time() - t_warm_0

    asr_text = res.get("recognized_text", "").strip()
    trans_text = res.get("translated_text", "").strip()
    tts_audio = res.get("tts_audio_path")
    val = res.get("audio_validation", {})
    is_valid = val.get("valid_audio", False)

    print("\n========== PIPELINE TEST RESULTS ==========\n")
    print(f"ASR Time:         {res['asr_time']:.2f} seconds")
    print(f"Translation Time: {res['translation_time']:.2f} seconds")
    print(f"Santali TTS Time: {res['tts_time']:.2f} seconds")
    print(f"Total Warm Time:  {res['total_time']:.2f} seconds\n")

    print(f"Hindi ASR Output:        '{asr_text}' ({'✓ PASS' if asr_text else '❌ FAIL'})")
    print(f"Santali Translated Text: '{trans_text}' ({'✓ PASS' if trans_text else '❌ FAIL'})")
    
    if is_valid and tts_audio and os.path.exists(tts_audio):
        print(f"Santali TTS Output:      ✓ VALID AUDIO ({val['duration']:.2f}s, SR: {val['sample_rate']}Hz, RMS: {val['rms']:.4f})")
    else:
        print(f"Santali TTS Output:      ❌ FAILED ({val.get('reason', 'Empty audio')})")

    print("\n========== SUMMARY REPORT ==========")
    print(f"Cold Start:      {cold_start_time:.2f} sec")
    print(f"Warm Inference:  {res['total_time']:.2f} sec")
    print(f"Audio Valid:     {'✓ YES' if is_valid else '❌ NO'}")
    print("===================================\n")

    # Assertions
    assert asr_text, "Hindi ASR output must be non-empty"
    assert trans_text, "Translation output must be non-empty"
    assert is_valid, f"TTS Audio must be valid non-empty audio: {val.get('reason')}"
    assert val.get("duration", 0) > 0.5, "WAV duration must be > 0.5s"
    assert val.get("max_amplitude", 0) > 0.01, "WAV samples must be non-zero"

    print("✓ ALL PIPELINE TESTS PASSED LOCALLY & OFFLINE")


if __name__ == "__main__":
    main()
