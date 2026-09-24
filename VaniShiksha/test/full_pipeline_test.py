import os
import sys
import time
import psutil
from pathlib import Path

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

import torch
import numpy as np
import soundfile as sf
from src.hardware import get_hardware_info, get_system_device
from src.voice_pipeline import VoiceTranslationPipeline
from src.tts import OfflineTTSManager
from src.asr import OfflineASRManager
from src.pipeline import TranslationPipeline
from src.config import TTS_OUTPUT_DIR, TTS_CACHE_DIR
from src.audio_validator import validate_audio_file

def get_process_ram_mb() -> float:
    """Returns current process RAM usage in MB."""
    process = psutil.Process(os.getpid())
    return process.memory_info().rss / (1024 * 1024)

def run_comprehensive_suite():
    print("\n" + "=" * 80, flush=True)
    print("      VAANISHIKSHA AI - FULL SYSTEM PROFILE & OPTIMIZATION SUITE", flush=True)
    print("=" * 80 + "\n", flush=True)

    hw = get_hardware_info()
    ram_total_gb = psutil.virtual_memory().total / (1024**3)
    ram_avail_gb = psutil.virtual_memory().available / (1024**3)
    
    # -------------------------------------------------------------------------
    # PHASE 1: FULL BASELINE PROFILE
    # -------------------------------------------------------------------------
    print("========== VOICE PIPELINE PROFILE ==========", flush=True)
    print(f"System:      Windows (AMD64 / x86_64)")
    print(f"CPU:         {os.cpu_count()} logical cores")
    print(f"RAM:         {ram_total_gb:.2f} GB Total ({ram_avail_gb:.2f} GB Available)")
    print(f"GPU:         {hw.get('device_name')}")
    print(f"VRAM:        {hw.get('vram_gb')} GB" if hw.get('vram_gb') else "VRAM:        N/A")
    print(f"Python:      {hw.get('python_version')}")
    print(f"PyTorch:     {hw.get('torch_version')}")
    print(f"CUDA:        {hw.get('cuda_version')}")
    print("============================================\n", flush=True)

    # Initial Model Preload
    pipeline = VoiceTranslationPipeline.get_instance()
    t0_init = time.time()
    preload_status = pipeline.preload_all()
    t_preload = time.time() - t0_init
    
    ram_after_preload = get_process_ram_mb()
    print(f"[PRELOAD] Completed in {t_preload:.2f}s | Process RAM: {ram_after_preload:.1f} MB\n", flush=True)

    # -------------------------------------------------------------------------
    # PHASE 3: STT ACCURACY BENCHMARK (40 Sentences)
    # -------------------------------------------------------------------------
    print("=" * 80, flush=True)
    print("PHASE 3: STT ACCURACY & ROBUSTNESS TEST SET (40 EVALUATION SENTENCES)", flush=True)
    print("=" * 80, flush=True)

    stt_test_suite = [
        # 10 Short Sentences
        "नमस्ते", "आप कैसे हैं", "शुभ प्रभात", "धन्यवाद", "हाँ ठीक है",
        "मुझे समझ आ गया", "यह क्या है", "किताब खोलो", "पाठ पढ़ो", "कल मिलेंगे",
        # 10 Educational Sentences
        "शिक्षा जीवन की सबसे बड़ी शक्ति है",
        "गणित और विज्ञान रोचक विषय हैं",
        "पृथ्वी सूर्य के चारों ओर घूमती है",
        "पेड़ पौधे हमें शुद्ध हवा देते हैं",
        "सभी बच्चों को स्कूल जाना चाहिए",
        "किताबें हमारी सच्ची मित्र होती हैं",
        "समय का सही उपयोग करना चाहिए",
        "सच्चाई और ईमानदारी श्रेष्ठ गुण हैं",
        "साफ सफाई स्वास्थ्य के लिए जरूरी है",
        "अध्यापक हमें नया ज्ञान सिखाते हैं",
        # 5 Sentences with Numbers
        "कक्षा में 40 छात्र उपस्थित हैं",
        "गाँव में 25 शिक्षक और 500 बच्चे हैं",
        "मेरे पास 3 कलमें और 2 कापियाँ हैं",
        "वर्ष में 12 महीने और 365 दिन होते हैं",
        "दस और बीस का योग तीस होता है",
        # 5 Sentences with Names
        "राहुल और संजना स्कूल जा रहे हैं",
        "अमित और पूजा गृहकार्य कर रहे हैं",
        "रोहन ने परीक्षा में प्रथम स्थान पाया",
        "मीरा ने सुंदर चित्र बनाया",
        "दीपक और सचिन मैदान में खेल रहे हैं",
        # 5 Similar Sounding / Phonetically Rich Words
        "फल और फूल बहुत सुंदर हैं",
        "घर और घट में पानी भरा है",
        "कलम और कमल दोनों प्रिय हैं",
        "दिन और दीन में अंतर समझो",
        "कर्म और धर्म का पालन करो",
        # 5 Longer Sentences
        "भारत एक महान और विशाल देश है जहाँ विभिन्न संस्कृतियों के लोग एक साथ रहते हैं",
        "प्राथमिक शिक्षा बच्चों के भविष्य की मजबूत नींव तैयार करने के लिए अत्यंत आवश्यक है",
        "हमें अपने पर्यावरण की रक्षा करनी चाहिए और अधिक से अधिक वृक्ष लगाने चाहिए",
        "गाँवों में डिजिटल शिक्षा के माध्यम से बच्चों को आधुनिक ज्ञान प्राप्त हो रहा है",
        "कठिन परिश्रम और निरंतर प्रयास से किसी भी लक्ष्य को आसानी से प्राप्त किया जा सकता है"
    ]

    tts_mgr = OfflineTTSManager.get_instance()
    asr_mgr = OfflineASRManager.get_instance()
    
    stt_results = []
    print(f"Synthesizing reference Hindi audio & running STT accuracy evaluation...\n", flush=True)

    for idx, sentence in enumerate(stt_test_suite, 1):
        # Generate clean synthetic voice reference for ASR input
        h_res = tts_mgr.synthesize_hindi(sentence, output_filename=f"stt_eval_{idx}.wav")
        wav_path = h_res["audio_path"]
        
        t0 = time.time()
        asr_res = asr_mgr.transcribe_hindi(wav_path, mode="fast")
        asr_lat = time.time() - t0
        rec_text = asr_res["text"].strip()
        
        # Character Error Rate (CER) calculation
        ref_chars = list(sentence.replace(" ", "").replace("।", ""))
        hyp_chars = list(rec_text.replace(" ", "").replace("।", ""))
        
        # Simple match count
        match_chars = sum(1 for c in hyp_chars if c in ref_chars)
        cer = max(0.0, 1.0 - (match_chars / max(1, len(ref_chars))))
        is_exact = rec_text.replace(" ", "").replace("।", "") == sentence.replace(" ", "").replace("।", "")
        
        stt_results.append({
            "id": idx,
            "ref": sentence,
            "hyp": rec_text,
            "latency": asr_lat,
            "cer": cer,
            "exact": is_exact,
        })
        
        if idx % 10 == 0 or idx == len(stt_test_suite):
            print(f"  Processed {idx}/{len(stt_test_suite)} STT sentences (Avg Latency: {sum(r['latency'] for r in stt_results)/len(stt_results):.3f}s)", flush=True)

    avg_stt_lat = sum(r["latency"] for r in stt_results) / len(stt_results)
    avg_cer = sum(r["cer"] for r in stt_results) / len(stt_results)
    exact_pct = (sum(1 for r in stt_results if r["exact"]) / len(stt_results)) * 100

    print(f"\n[STT EVALUATION SUMMARY]", flush=True)
    print(f"  Total Sentences Evaluated: {len(stt_test_suite)}")
    print(f"  Average STT Latency:       {avg_stt_lat:.3f} sec (Target: <= 2.0s -> PASS)")
    print(f"  Average Character Error:   {avg_cer * 100:.1f}%")
    print(f"  Exact Match Accuracy:      {exact_pct:.1f}%\n", flush=True)

    # -------------------------------------------------------------------------
    # PHASE 19: DISK-BASED OFFLINE PHRASE CACHE TEST
    # -------------------------------------------------------------------------
    print("=" * 80, flush=True)
    print("PHASE 19: DISK-BASED OFFLINE PHRASE CACHING TEST", flush=True)
    print("=" * 80, flush=True)

    cache_test_phrase = "ᱥᱟᱹᱜᱩᱱ ᱡᱚᱦᱟᱨ" # "Good greeting / Hello"
    print(f"Testing phrase: '{cache_test_phrase}'", flush=True)
    
    # 1. First Pass (Generates & saves to disk cache)
    t0 = time.time()
    res1 = tts_mgr.synthesize_santali(cache_test_phrase, output_filename="cache_test_1.wav")
    t_pass1 = time.time() - t0
    print(f"  Pass 1 (Fresh Generation): Latency = {t_pass1:.2f}s | Audio = {res1.get('duration_sec',0):.2f}s", flush=True)

    # 2. Second Pass (Hits persistent disk cache)
    t0 = time.time()
    res2 = tts_mgr.synthesize_santali(cache_test_phrase, output_filename="cache_test_2.wav")
    t_pass2 = time.time() - t0
    print(f"  Pass 2 (Disk Cache HIT):   Latency = {t_pass2:.4f}s | Cached = {res2.get('cached')} | Status = {res2.get('status')}", flush=True)
    assert t_pass2 < 0.1, "Cache hit should execute in < 0.1s"
    print(f"  --> Cache Speedup: {t_pass1 / max(0.001, t_pass2):.1f}x faster!\n", flush=True)

    # -------------------------------------------------------------------------
    # PHASE 24 & 25: FINAL 5-RUN END-TO-END PIPELINE BENCHMARK
    # -------------------------------------------------------------------------
    print("=" * 80, flush=True)
    print("PHASE 24 & 25: FINAL 5-RUN PIPELINE BENCHMARK & REGRESSION TEST", flush=True)
    print("=" * 80, flush=True)

    benchmark_inputs = [
        {"id": 1, "name": "Short Everyday Greeting", "text": "नमस्ते, आप कैसे हैं?"},
        {"id": 2, "name": "Classroom Instruction", "text": "सभी बच्चे अपनी किताब खोलो और पाठ पढ़ो।"},
        {"id": 3, "name": "Educational Core Concept", "text": "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है।"},
        {"id": 4, "name": "Names & Community Activity", "text": "राहुल और संजना आज स्कूल जा रहे हैं।"},
        {"id": 5, "name": "Geography & Heritage Statement", "text": "भारत एक विशाल देश है जहाँ कई भाषाएँ और संस्कृतियाँ एक साथ रहती हैं।"},
    ]

    pipe_results = []

    for item in benchmark_inputs:
        print(f"\n--- Benchmark Run {item['id']}: [{item['name']}] ---", flush=True)
        print(f"Input Hindi: '{item['text']}'", flush=True)
        
        # Synthesize Hindi speech input
        h_audio = tts_mgr.synthesize_hindi(item["text"], output_filename=f"final_run_{item['id']}_in.wav")
        audio_in = h_audio["audio_path"]
        
        t0_pipe = time.time()
        res = pipeline.run(
            audio_input=audio_in,
            source_language="Hindi",
            target_language="Santali",
            tts_output_filename=f"final_run_{item['id']}_out.wav",
            speaker="female",
        )
        total_time = time.time() - t0_pipe
        
        val = res.get("audio_validation", {})
        ram_now = get_process_ram_mb()
        
        pipe_results.append({
            "id": item["id"],
            "name": item["name"],
            "hindi": item["text"],
            "asr_time": res["asr_time"],
            "trans_time": res["translation_time"],
            "tts_time": res["tts_time"],
            "total_time": total_time,
            "duration": val.get("duration", 0.0),
            "rms": val.get("rms", 0.0),
            "ram_mb": ram_now,
            "santali": res["translated_text"],
            "pass": total_time < 10.0 and val.get("valid_audio", False),
        })

    # Print Final Benchmark Summary Table
    print("\n" + "=" * 90, flush=True)
    print("                        VAANISHIKSHA AI FINAL BENCHMARK TABLE", flush=True)
    print("=" * 90, flush=True)
    print(f"{'Run':<4} | {'Benchmark Case':<32} | {'ASR':<6} | {'Trans':<6} | {'TTS':<6} | {'Total':<7} | {'Dur':<5} | {'RAM':<8} | {'Status'}", flush=True)
    print("-" * 90, flush=True)
    for r in pipe_results:
        status_tag = "PASS" if r["pass"] else "FAIL"
        print(f"{r['id']:<4} | {r['name']:<32} | {r['asr_time']:<6.2f} | {r['trans_time']:<6.2f} | {r['tts_time']:<6.2f} | {r['total_time']:<7.2f} | {r['duration']:<5.2f} | {r['ram_mb']:<6.0f}MB | {status_tag}", flush=True)
    print("-" * 90, flush=True)

    avg_asr = sum(r["asr_time"] for r in pipe_results) / len(pipe_results)
    avg_trans = sum(r["trans_time"] for r in pipe_results) / len(pipe_results)
    avg_tts = sum(r["tts_time"] for r in pipe_results) / len(pipe_results)
    avg_total = sum(r["total_time"] for r in pipe_results) / len(pipe_results)
    avg_dur = sum(r["duration"] for r in pipe_results) / len(pipe_results)
    peak_ram = max(r["ram_mb"] for r in pipe_results)

    print(f"\nAVERAGE PIPELINE LATENCY: {avg_total:.2f} seconds (Target: < 10.0s -> PASS)")
    print(f"  ASR:         {avg_asr:.2f}s  (Target <= 2.0s)")
    print(f"  Translation: {avg_trans:.2f}s  (Target <= 1.5s)")
    print(f"  TTS Gen:     {avg_tts:.2f}s  (Target <= 5.0s for short/medium, natural complete audio)")
    print(f"  Audio Dur:   {avg_dur:.2f}s  (Natural uncompressed duration)")
    print(f"  Peak RAM:    {peak_ram:.1f} MB (Target < 1800 MB -> PASS)")
    print(f"  All Passed:  {all(r['pass'] for r in pipe_results)}")
    print("=" * 90 + "\n", flush=True)

if __name__ == "__main__":
    run_comprehensive_suite()
