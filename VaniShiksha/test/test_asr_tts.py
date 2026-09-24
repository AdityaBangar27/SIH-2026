"""
VaaniShikhra AI - Component Test Script
Tests:
  1. Hindi ASR (collabora/whisper-tiny-hindi) - 5 sentences
  2. Santali TTS (RXD03/indic-parler-tts) - 5 sentences
"""
import sys
import os
import time
from pathlib import Path

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

from src.asr import OfflineASRManager
from src.tts import OfflineTTSManager

AUDIO_TEST_DIR = ROOT_DIR / "audio" / "test"


def separator(title):
    print("\n" + "=" * 65)
    print(f"  {title}")
    print("=" * 65)


def test_hindi_asr():
    separator("TEST 1: HINDI ASR (collabora/whisper-tiny-hindi -> Devanagari)")
    asr = OfflineASRManager()

    hindi_wav = AUDIO_TEST_DIR / "hindi_sample.wav"
    print(f"\nInput WAV: {hindi_wav}")
    t0 = time.time()
    result = asr.transcribe(str(hindi_wav), language="Hindi")

    print(f"\n  Recognized Text : {result['text']}")
    print(f"  Script          : {result['script']}")
    print(f"  ASR Latency     : {result['latency']:.3f}s")

    text = result["text"]
    devanagari_chars = sum(1 for c in text if '\u0900' <= c <= '\u097F')
    total_alpha = sum(1 for c in text if c.isalpha())
    devanagari_pct = (devanagari_chars / total_alpha * 100) if total_alpha > 0 else 0

    print(f"  Devanagari chars: {devanagari_chars}/{total_alpha} ({devanagari_pct:.0f}%)")

    if devanagari_pct >= 70:
        print("  PASS: Hindi Devanagari output confirmed")
    elif devanagari_pct > 0:
        print(f"  PARTIAL: Some Devanagari ({devanagari_pct:.0f}%)")
    else:
        print("  FAIL: No Devanagari detected")

    return result["text"]


def test_santali_tts():
    separator("TEST 2: SANTALI TTS (RXD03/indic-parler-tts -> WAV)")
    tts = OfflineTTSManager()

    if not tts.parler_library_available:
        print("  FAIL: parler-tts library not installed")
        print("  Run: pip install parler-tts")
        return False

    test_sentences = [
        "\u1c21\u1c38\u1c26\u1c1f\u1c28, \u1c2a\u1c2e\u1c2b \u1c3e\u1c2e\u1c35\u1c1f \u1c22\u1c2e\u1c1c\u1c1f\u1c1c \u1c15\u1c24\u1c1c\u1c1f?",
        "\u1c25\u1c2e\u1c2a\u1c2e\u1c2b \u1c2b\u1c38 \u1c1c\u1c24\u1c2b\u1c3b\u1c28\u1c1f\u1c39 \u1c35\u1c38\u1c35\u1c1f\u1c1c \u1c1f\u1c1c\u1c1f\u1c22 \u1c3e\u1c1f\u1c39\u1c2a\u1c2b \u1c1f\u1c39\u1c10\u1c24 \u1c21\u1c1f\u1c39\u1c25\u1c1c\u1c1f\u1c1c \u1c35\u1c1f\u1c1c\u1c1f\u1c5a",
        "\u1c1f\u1c22\u1c1f\u1c1c \u1c39\u1c25\u1c1b\u1c25\u1c22 \u1c2a\u1c2e\u1c2b?",
        "\u1c1b\u1c2e\u1c26\u1c2e\u1c39 \u1c1f\u1c15\u1c38 \u1c2e\u1c3e\u1c35\u1c37\u1c1f \u1c1f\u1c3e \u1c25\u1c1f\u1c3a\u1c2e\u1c25 \u1c15\u1c38\u1c1c \u1c2f\u1c1f\u1c32\u1c26\u1c1f\u1c1c\u1c34\u1c1f \u1c5a",
        "\u1c22\u1c25 \u1c26\u1c38\u1c32 \u1c22\u1c1f\u1c28\u1c1f\u1c22 \u1c35\u1c1f\u1c1c\u1c1f \u1c5a",
    ]

    passed = 0
    for i, text in enumerate(test_sentences, 1):
        print(f"\n  Sentence {i}: {text}")
        result = tts.synthesize_santali(text, output_filename=f"test_santali_{i}.wav")

        if result.get("success") and result.get("audio_path"):
            size = os.path.getsize(result["audio_path"])
            print(f"    PASS -> WAV: {result['audio_path']}")
            print(f"    Duration: {result.get('duration_sec',0):.2f}s | Latency: {result.get('latency',0):.3f}s | Size: {size} bytes")
            passed += 1
        else:
            print(f"    FAIL: {result.get('error', result.get('status', 'Unknown'))}")

    print(f"\n  Results: {passed}/{len(test_sentences)} sentences synthesized")
    if passed == len(test_sentences):
        print("  Santali TTS: FULLY WORKING")
    elif passed > 0:
        print("  Santali TTS: PARTIAL")
    else:
        print("  Santali TTS: FAILED")

    return passed > 0


if __name__ == "__main__":
    print("VaaniShikhra AI - Component Test")
    try:
        test_hindi_asr()
    except Exception as e:
        print(f"  Hindi ASR Error: {e}")
        import traceback; traceback.print_exc()

    try:
        test_santali_tts()
    except Exception as e:
        print(f"  Santali TTS Error: {e}")
        import traceback; traceback.print_exc()
