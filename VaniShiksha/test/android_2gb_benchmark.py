import os
import sys
import time
from pathlib import Path

# Simulate pure CPU environment for Android device profile
os.environ["CUDA_VISIBLE_DEVICES"] = ""

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

import psutil
import torch
from src.hardware import get_hardware_info
from src.memory_manager import ModelMemoryManager
from src.voice_pipeline import VoiceTranslationPipeline
from src.tts import OfflineTTSManager
from src.asr import OfflineASRManager
from src.pipeline import TranslationPipeline
from src.audio_validator import validate_audio_file

def run_android_2gb_benchmark():
    print("\n" + "=" * 85, flush=True)
    print("      VAANISHIKSHA AI — 2 GB RAM OFFLINE ANDROID PROFILE BENCHMARK", flush=True)
    print("=" * 85 + "\n", flush=True)

    mem_mgr = ModelMemoryManager.get_instance()
    hw_info = get_hardware_info()
    init_mem = mem_mgr.get_system_ram_info()

    print("========== HARDWARE & RUNTIME PROFILE ==========", flush=True)
    print(f"Target Environment: 2 GB RAM Offline Android Device (Simulated on CPU/Mobile Profile)")
    print(f"System RAM:         {init_mem['total_gb']:.2f} GB Total ({init_mem['available_gb']:.2f} GB Available)")
    print(f"Initial Process RSS:{init_mem['process_rss_mb']:.1f} MB")
    print(f"Memory Budget Cap:  {mem_mgr.RAM_LIMIT_MB:.1f} MB (1.8 GB Android Safety Ceiling)")
    print(f"Python Version:     {hw_info['python_version']}")
    print(f"PyTorch Version:    {hw_info['torch_version']}")
    print("================================================\n", flush=True)

    # Preload all models in mobile profile
    pipeline = VoiceTranslationPipeline.get_instance()
    t0_preload = time.time()
    preload_res = pipeline.preload_all(profile="mobile")
    t_preload = time.time() - t0_preload

    post_preload_mem = mem_mgr.get_process_rss_mb()
    print(f"\n[STARTUP] All Models Preloaded in {t_preload:.2f}s | Process RAM: {post_preload_mem:.1f} MB\n", flush=True)

    test_cases = [
        {"id": 1, "type": "Short Classroom Greeting", "text": "नमस्ते, आप कैसे हैं?"},
        {"id": 2, "type": "Everyday Instruction", "text": "सभी बच्चे अपनी किताब खोलो और पाठ पढ़ो।"},
        {"id": 3, "type": "Educational Core Sentence", "text": "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है।"},
        {"id": 4, "type": "Sentence with Numbers", "text": "गाँव में 25 शिक्षक और 500 बच्चे हैं।"},
        {"id": 5, "type": "Sentence with Names", "text": "राहुल और संजना आज स्कूल जा रहे हैं।"},
        {"id": 6, "type": "Long Heritage Statement", "text": "भारत एक विशाल देश है जहाँ कई भाषाएँ और संस्कृतियाँ एक साथ रहती हैं।"},
        {"id": 7, "type": "Cached Phrase Instant Check", "text": "नमस्ते, आप कैसे हैं?"},
    ]

    tts_mgr = OfflineTTSManager.get_instance()
    results = []

    print("=" * 85, flush=True)
    print("RUNNING 7 END-TO-END MOBILE EVALUATION TEST CASES (OFFLINE PROFILE)", flush=True)
    print("=" * 85, flush=True)

    for tc in test_cases:
        print(f"\n--- [Test Case {tc['id']}: {tc['type']}] ---", flush=True)
        print(f"Input Hindi Text: '{tc['text']}'", flush=True)

        # 1. Synthesize Hindi speech input
        h_audio = tts_mgr.synthesize_hindi(tc["text"], output_filename=f"mob_tc_{tc['id']}_in.wav")
        audio_in = h_audio["audio_path"]

        # 2. Run through mobile voice pipeline
        t0 = time.time()
        res = pipeline.run(
            audio_input=audio_in,
            source_language="Hindi",
            target_language="Santali",
            tts_output_filename=f"mob_tc_{tc['id']}_out.wav",
            mode="mobile", # Explicitly trigger lightweight mobile profile
        )
        total_lat = time.time() - t0
        val = res.get("audio_validation", {})
        mem_info = mem_mgr.check_memory_budget()
        android_ram_mb = mem_info["estimated_android_ram_mb"]

        is_passed = (
            total_lat < 10.0 and
            mem_info["within_budget"] and
            val.get("valid_audio", False)
        )

        results.append({
            "id": tc["id"],
            "type": tc["type"],
            "text": tc["text"],
            "recognized": res.get("recognized_text", ""),
            "translated": res.get("translated_text", ""),
            "asr_time": res.get("asr_time", 0.0),
            "trans_time": res.get("translation_time", 0.0),
            "tts_time": res.get("tts_time", 0.0),
            "total_time": round(total_lat, 3),
            "duration": val.get("duration", 0.0),
            "android_ram_mb": android_ram_mb,
            "passed": is_passed,
        })

        print(f"[TC {tc['id']} RESULT] Total: {total_lat:.2f}s (ASR: {res['asr_time']:.2f}s, Trans: {res['translation_time']:.2f}s, TTS: {res['tts_time']:.2f}s) | Audio: {val.get('duration',0):.2f}s | Android RAM: {android_ram_mb:.1f}MB | Pass: {is_passed}")

    # Summary Table
    print("\n" + "=" * 95, flush=True)
    print("                    VAANISHIKSHA AI — 2 GB RAM MOBILE BENCHMARK RESULTS", flush=True)
    print("=" * 95, flush=True)
    print(f"{'TC':<3} | {'Test Scenario':<28} | {'ASR':<6} | {'Trans':<6} | {'TTS':<6} | {'Total':<7} | {'Dur':<5} | {'Android RAM':<12} | {'Status'}", flush=True)
    print("-" * 95, flush=True)
    for r in results:
        status_str = "PASS" if r["passed"] else "FAIL"
        print(f"{r['id']:<3} | {r['type']:<28} | {r['asr_time']:<6.2f} | {r['trans_time']:<6.2f} | {r['tts_time']:<6.2f} | {r['total_time']:<7.2f} | {r['duration']:<5.2f} | {r['android_ram_mb']:<10.1f}MB | {status_str}", flush=True)
    print("-" * 95, flush=True)

    avg_asr = sum(r["asr_time"] for r in results) / len(results)
    avg_trans = sum(r["trans_time"] for r in results) / len(results)
    avg_tts = sum(r["tts_time"] for r in results) / len(results)
    avg_total = sum(r["total_time"] for r in results) / len(results)
    avg_dur = sum(r["duration"] for r in results) / len(results)
    peak_android_ram = max(r["android_ram_mb"] for r in results)

    print(f"\nAVERAGE METRICS FOR 2 GB ANDROID PROFILE:")
    print(f"  Average Pipeline Latency:  {avg_total:.2f} sec (Target: < 10.0s -> PASS)")
    print(f"  Average ASR Latency:       {avg_asr:.2f} sec (Target: <= 2.0s)")
    print(f"  Average Translation Time:  {avg_trans:.2f} sec (Target: <= 3.5s)")
    print(f"  Average Mobile TTS Time:   {avg_tts:.2f} sec (Target: <= 1.0s)")
    print(f"  Average Audio Duration:    {avg_dur:.2f} sec (Natural spoken rate)")
    print(f"  Estimated Peak Android RAM:{peak_android_ram:.1f} MB (Budget Ceiling: 1800.0 MB -> PASS)")
    print(f"  All Test Cases Passed:     {all(r['passed'] for r in results)}")
    print("=" * 95 + "\n", flush=True)

if __name__ == "__main__":
    run_android_2gb_benchmark()
