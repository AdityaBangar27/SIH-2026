import os
import sys
import time
from pathlib import Path

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

import numpy as np
import soundfile as sf
from src.voice_pipeline import VoiceTranslationPipeline
from src.tts import OfflineTTSManager
from src.asr import OfflineASRManager
from src.pipeline import TranslationPipeline
from src.config import TTS_OUTPUT_DIR
from src.audio_validator import validate_audio_file

def run_benchmarks():
    print("\n" + "=" * 70, flush=True)
    print("      VAANISHIKSHA AI - TTS & FULL PIPELINE BENCHMARK SUITE", flush=True)
    print("=" * 70 + "\n", flush=True)

    # 1. Initialize Pipeline & Preload
    pipeline = VoiceTranslationPipeline.get_instance()
    preload_results = pipeline.preload_all()
    print(f"Preload Status: {preload_results}\n", flush=True)

    # -------------------------------------------------------------
    # PART A: DIRECT SANTALI TTS BENCHMARK (Step 10 & Step 9: 5 Runs)
    # -------------------------------------------------------------
    print("=" * 70, flush=True)
    print("PART A: DIRECT SANTALI TTS BENCHMARK (5 CONSECUTIVE RUNS)", flush=True)
    print("=" * 70, flush=True)

    tts_mgr = OfflineTTSManager.get_instance()
    # Santali Ol Chiki test sentence (~3.5-3.7s audio target)
    # "ᱥᱟᱱᱟᱢ ᱠᱚ ᱜᱮ ᱥᱟᱹᱜᱩᱱ ᱡᱚᱦᱟᱨ, ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱥᱮᱪᱮᱫ ᱵᱟᱵᱚᱛ ᱵᱚᱱ ᱜᱟᱞᱢᱟᱨᱟᱣᱟ᱾"
    # ("Warm greetings to all, today we will discuss education.")
    santali_test_sentence = "ᱥᱟᱱᱟᱢ ᱠᱚ ᱜᱮ ᱥᱟᱹᱜᱩᱱ ᱡᱚᱦᱟᱨ, ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱥᱮᱪᱮᱫ ᱵᱟᱵᱚᱛ ᱵᱚᱱ ᱜᱟᱞᱢᱟᱨᱟᱣᱟ᱾"
    
    direct_tts_latencies = []
    direct_tts_durations = []

    for r in range(1, 6):
        print(f"\n--- Direct TTS Run {r}/5 ---", flush=True)
        res = tts_mgr.synthesize_santali(santali_test_sentence, output_filename=f"direct_bench_{r}.wav")
        if res.get("success"):
            lat = res["latency"]
            dur = res["duration_sec"]
            val = res["validation"]
            direct_tts_latencies.append(lat)
            direct_tts_durations.append(dur)
            print(f"  [RESULT] Status: SUCCESS | Latency: {lat:.2f}s | Audio Dur: {dur:.2f}s | RMS: {val.get('rms', 0):.4f}", flush=True)
        else:
            print(f"  [RESULT] Status: FAILED | Error: {res.get('error')}", flush=True)

    avg_direct_tts = sum(direct_tts_latencies) / len(direct_tts_latencies) if direct_tts_latencies else 0.0
    print(f"\n>>> Direct TTS Average Latency: {avg_direct_tts:.2f}s (Runs: {[round(x, 2) for x in direct_tts_latencies]}) <<<\n", flush=True)

    # -------------------------------------------------------------
    # PART B: STEP 16 ACCURACY REGRESSION TEST (7 TEST CASES)
    # -------------------------------------------------------------
    print("=" * 70, flush=True)
    print("PART B: STEP 16 ACCURACY REGRESSION & FULL PIPELINE TESTS", flush=True)
    print("=" * 70, flush=True)

    test_cases = [
        {
            "id": 1,
            "type": "Short Hindi sentence",
            "hindi": "नमस्ते, आप कैसे हैं?",
        },
        {
            "id": 2,
            "type": "Medium Hindi sentence",
            "hindi": "आज का मौसम बहुत अच्छा है और सब लोग खुश हैं।",
        },
        {
            "id": 3,
            "type": "Longer Hindi sentence",
            "hindi": "भारत एक विशाल देश है जहाँ कई भाषाएँ और संस्कृतियाँ एक साथ रहती हैं।",
        },
        {
            "id": 4,
            "type": "Sentence containing numbers",
            "hindi": "इस गाँव में 500 छात्र और 25 शिक्षक हैं।",
        },
        {
            "id": 5,
            "type": "Sentence containing common Indian names",
            "hindi": "राहुल और संजना आज स्कूल जा रहे हैं।",
        },
        {
            "id": 6,
            "type": "Educational sentence",
            "hindi": "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है।",
        },
        {
            "id": 7,
            "type": "Multiple consecutive requests (Consecutive check)",
            "hindi": "हम सब मिलकर इस काम को पूरा करेंगे।",
        },
    ]

    # Generate synthetic audio for ASR input or run through translation + TTS
    pipeline_results = []
    tts_hindi = tts_mgr.synthesize_hindi

    for tc in test_cases:
        print(f"\n------------------------------------------------------------", flush=True)
        print(f"Test Case {tc['id']}: [{tc['type']}]", flush=True)
        print(f"Input Hindi Text: '{tc['hindi']}'", flush=True)
        print(f"------------------------------------------------------------", flush=True)

        # 1. Create clean audio sample for ASR test
        h_audio_res = tts_hindi(tc["hindi"], output_filename=f"input_tc_{tc['id']}.wav")
        audio_path = h_audio_res["audio_path"]

        t_pipe_start = time.time()
        res = pipeline.run(
            audio_input=audio_path,
            source_language="Hindi",
            target_language="Santali",
            tts_output_filename=f"output_tc_{tc['id']}.wav",
            speaker="female",
        )
        total_pipe_time = time.time() - t_pipe_start

        asr_time = res["asr_time"]
        trans_time = res["translation_time"]
        tts_time = res["tts_time"]
        rec_text = res["recognized_text"]
        trans_text = res["translated_text"]
        val = res.get("audio_validation", {})
        valid_audio = val.get("valid_audio", False)
        dur = val.get("duration", 0.0)
        rms = val.get("rms", 0.0)

        # Criteria check
        target_met = total_pipe_time < 20.0
        status_str = "PASS (<20s)" if target_met and valid_audio else ("FAIL (>20s)" if not target_met else "FAIL (Invalid Audio)")

        print(f"\n[SUMMARY TC {tc['id']}]", flush=True)
        print(f"  ASR Recognized : '{rec_text}' ({asr_time:.2f}s)", flush=True)
        print(f"  Translation    : '{trans_text}' ({trans_time:.2f}s)", flush=True)
        print(f"  TTS Output     : {res['tts_audio_path']} ({tts_time:.2f}s)", flush=True)
        print(f"  Audio Duration : {dur:.2f}s (RMS: {rms:.4f})", flush=True)
        print(f"  TOTAL LATENCY  : {total_pipe_time:.2f}s", flush=True)
        print(f"  TARGET (<20s)  : {status_str}", flush=True)

        pipeline_results.append({
            "id": tc["id"],
            "type": tc["type"],
            "hindi": tc["hindi"],
            "santali": trans_text,
            "asr_time": asr_time,
            "trans_time": trans_time,
            "tts_time": tts_time,
            "total_time": total_pipe_time,
            "duration": dur,
            "rms": rms,
            "valid": valid_audio,
            "pass": target_met and valid_audio,
        })

    # Final Overall Summary Table
    print("\n" + "=" * 80, flush=True)
    print("                    FINAL BENCHMARK SUMMARY TABLE", flush=True)
    print("=" * 80, flush=True)
    print(f"{'TC':<3} | {'Type':<28} | {'ASR':<6} | {'Trans':<6} | {'TTS':<6} | {'Total':<7} | {'Dur':<5} | {'Status'}", flush=True)
    print("-" * 80, flush=True)
    for r in pipeline_results:
        status = "PASS" if r["pass"] else "FAIL"
        print(f"{r['id']:<3} | {r['type']:<28} | {r['asr_time']:<6.2f} | {r['trans_time']:<6.2f} | {r['tts_time']:<6.2f} | {r['total_time']:<7.2f} | {r['duration']:<5.2f} | {status}", flush=True)
    print("-" * 80, flush=True)

    avg_total = sum(r["total_time"] for r in pipeline_results) / len(pipeline_results)
    avg_tts = sum(r["tts_time"] for r in pipeline_results) / len(pipeline_results)
    avg_asr = sum(r["asr_time"] for r in pipeline_results) / len(pipeline_results)
    avg_trans = sum(r["trans_time"] for r in pipeline_results) / len(pipeline_results)

    print(f"AVERAGES: ASR: {avg_asr:.2f}s | Translation: {avg_trans:.2f}s | TTS: {avg_tts:.2f}s | TOTAL: {avg_total:.2f}s", flush=True)
    print(f"ALL TESTS PASSED: {all(r['pass'] for r in pipeline_results)}", flush=True)
    print("=" * 80 + "\n", flush=True)

if __name__ == "__main__":
    run_benchmarks()
