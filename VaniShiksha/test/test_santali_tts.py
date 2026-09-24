import os
import sys
import time
from pathlib import Path

# Force UTF-8 on Windows
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

from src.tts import OfflineTTSManager, prepare_santali_for_tts
from src.audio_validator import validate_audio_file


def run_santali_tts_test():
    test_sentence = "ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱜ ᱵᱤᱱᱟ?"

    print("==================================================")
    print("TEST: Standalone Santali Text-to-Speech (TTS)")
    print("==================================================")
    print("Loading Santali TTS...")
    
    t0 = time.time()
    tts = OfflineTTSManager.get_instance()
    loaded = tts.load_santali_tts()
    load_time = time.time() - t0

    if not loaded:
        print("❌ FAILED: Could not load Santali Indic Parler-TTS model.")
        sys.exit(1)

    print(f"Model loaded in {load_time:.2f} seconds\n")

    print("Input:")
    print(test_sentence)
    print("\nGenerating...")

    t_inf_0 = time.time()
    res = tts.synthesize_santali(test_sentence, output_filename="test_santali_verified.wav", mode="fast")
    inf_time = time.time() - t_inf_0

    print(f"TTS inference: {inf_time:.2f} sec\n")

    if not res.get("success") or not res.get("audio_path"):
        print("Santali TTS FAILED: generated audio is empty or invalid")
        print(f"Error: {res.get('error')}")
        sys.exit(1)

    audio_path = res["audio_path"]
    validation = validate_audio_file(audio_path)

    print("Audio validation:")
    print(f"File size: {validation['file_size']} bytes")
    print(f"Sample rate: {validation['sample_rate']} Hz")
    print(f"Frames: {validation['frames']}")
    print(f"Duration: {validation['duration']:.2f} sec")
    print(f"Max amplitude: {validation['max_amplitude']:.5f}")
    print(f"RMS: {validation['rms']:.5f}")

    if not validation["valid_audio"]:
        print("\n❌ Santali TTS FAILED: generated audio is empty or invalid")
        print(f"Reason: {validation['reason']}")
        sys.exit(1)

    # Strict assertions
    assert validation["file_size"] > 0, "File size must be greater than 0"
    assert validation["sample_rate"] > 0, "Sample rate must be greater than 0"
    assert validation["frames"] > 0, "Frames must be greater than 0"
    assert validation["duration"] > 0.5, "Duration must be greater than 0.5s"
    assert validation["max_amplitude"] > 0.01, "Max amplitude must be greater than 0.01"
    assert validation["rms"] > 0.005, "RMS must be greater than 0.005"

    print("\n✓ VALID SANTALI AUDIO")
    print(f"Saved audio location: {audio_path}")
    print("==================================================")


if __name__ == "__main__":
    run_santali_tts_test()
