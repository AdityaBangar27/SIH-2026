import os
import sys
import time
import json
import psutil
from pathlib import Path
import numpy as np
import soundfile as sf
import onnxruntime as ort
from tokenizers import Tokenizer
from transformers import WhisperTokenizer

# Configure UTF-8
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = Path(__file__).resolve().parent.parent.parent
AI_DIR = BASE_DIR / "android_ai"
MODELS_DIR = AI_DIR / "models"
TOKENIZERS_DIR = AI_DIR / "tokenizers"
CONFIGS_DIR = AI_DIR / "configs"
SAMPLES_DIR = AI_DIR / "samples"
BENCHMARKS_DIR = AI_DIR / "benchmarks"

def check_file(path: Path, min_size_kb: int = 1) -> tuple:
    if not path.exists():
        return False, 0.0, "MISSING"
    sz_kb = path.stat().st_size / 1024.0
    if sz_kb < min_size_kb:
        return False, sz_kb, "CORRUPT/EMPTY"
    return True, sz_kb, "OK"

def verify_package():
    print("=" * 80)
    print("      VAANISHIKSHA AI — ANDROID ASSET PACKAGE INTEGRITY AUDIT")
    print("=" * 80)

    # 1. Audit ASR Assets
    print("\n[1/4] Auditing ASR Assets...")
    asr_files = [
        (MODELS_DIR / "asr" / "encoder_model_quant.onnx", 5000),
        (MODELS_DIR / "asr" / "decoder_model_quant.onnx", 20000),
        (TOKENIZERS_DIR / "asr" / "vocab.json", 100),
        (TOKENIZERS_DIR / "asr" / "merges.txt", 100),
        (TOKENIZERS_DIR / "asr" / "tokenizer.json", 500),
        (CONFIGS_DIR / "asr_config.json", 0.5),
    ]
    asr_ok = True
    for p, min_sz in asr_files:
        ok, sz, status = check_file(p, min_sz)
        asr_ok = asr_ok and ok
        print(f"  {p.name:<32} {sz:>10.2f} KB [{status}]")
    print(f"  --> ASR Package Status: {'PASSED' if asr_ok else 'FAILED'}")

    # 2. Audit Translation Assets
    print("\n[2/4] Auditing Translation Assets...")
    trans_files = [
        (MODELS_DIR / "translation" / "encoder_model.onnx", 500),
        (MODELS_DIR / "translation" / "encoder_model.onnx.data", 50000),
        (MODELS_DIR / "translation" / "decoder_model.onnx", 1000),
        (MODELS_DIR / "translation" / "decoder_shared.onnx.data", 100000),
        (MODELS_DIR / "translation" / "decoder_with_past_model.onnx", 1000),
        (TOKENIZERS_DIR / "translation" / "tokenizer_src.json", 10000),
        (TOKENIZERS_DIR / "translation" / "tokenizer_tgt.json", 10000),
        (TOKENIZERS_DIR / "translation" / "dict.SRC.json", 1000),
        (TOKENIZERS_DIR / "translation" / "dict.TGT.json", 1000),
        (CONFIGS_DIR / "translation_config.json", 0.5),
    ]
    trans_ok = True
    for p, min_sz in trans_files:
        ok, sz, status = check_file(p, min_sz)
        trans_ok = trans_ok and ok
        print(f"  {p.name:<32} {sz:>10.2f} KB [{status}]")
    print(f"  --> Translation Package Status: {'PASSED' if trans_ok else 'FAILED'}")

    # 3. Audit TTS Assets & Configurations
    print("\n[3/4] Auditing TTS Configurations & Analyses...")
    tts_files = [
        (CONFIGS_DIR / "tts_config.json", 0.5),
        (MODELS_DIR / "tts" / "README_TTS_ANALYSIS.md", 1),
    ]
    tts_ok = True
    for p, min_sz in tts_files:
        ok, sz, status = check_file(p, min_sz)
        tts_ok = tts_ok and ok
        print(f"  {p.name:<32} {sz:>10.2f} KB [{status}]")
    print(f"  --> TTS Documentation & Config Status: {'PASSED' if tts_ok else 'FAILED'}")

    # 4. Measure Pure Standalone Process RAM Footprint (Zero PyTorch)
    print("\n[4/4] Measuring Standalone ONNX Runtime Memory Footprint (Pure ONNX)...")
    process = psutil.Process(os.getpid())
    init_rss = process.memory_info().rss / (1024 * 1024)
    print(f"  Process Base Python RSS: {init_rss:.1f} MB")

    sess_opts = ort.SessionOptions()
    sess_opts.intra_op_num_threads = 2
    sess_opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL

    # Load ASR ONNX models
    t0 = time.time()
    enc_asr = ort.InferenceSession(str(MODELS_DIR / "asr" / "encoder_model_quant.onnx"), sess_opts, providers=["CPUExecutionProvider"])
    dec_asr = ort.InferenceSession(str(MODELS_DIR / "asr" / "decoder_model_quant.onnx"), sess_opts, providers=["CPUExecutionProvider"])
    post_asr_rss = process.memory_info().rss / (1024 * 1024)
    asr_ram_delta = post_asr_rss - init_rss
    print(f"  ✓ ASR ONNX INT8 loaded in {time.time()-t0:.2f}s | RAM RSS: {post_asr_rss:.1f} MB (Delta: +{asr_ram_delta:.1f} MB)")

    # Load Translation ONNX models
    t0 = time.time()
    enc_trans = ort.InferenceSession(str(MODELS_DIR / "translation" / "encoder_model.onnx"), sess_opts, providers=["CPUExecutionProvider"])
    dec_trans = ort.InferenceSession(str(MODELS_DIR / "translation" / "decoder_model.onnx"), sess_opts, providers=["CPUExecutionProvider"])
    dec_past_trans = ort.InferenceSession(str(MODELS_DIR / "translation" / "decoder_with_past_model.onnx"), sess_opts, providers=["CPUExecutionProvider"])
    post_trans_rss = process.memory_info().rss / (1024 * 1024)
    trans_ram_delta = post_trans_rss - post_asr_rss
    print(f"  ✓ Translation ONNX INT8 loaded in {time.time()-t0:.2f}s | RAM RSS: {post_trans_rss:.1f} MB (Delta: +{trans_ram_delta:.1f} MB)")

    total_ai_ram = post_trans_rss
    print(f"\n==========================================================================")
    print(f"  TOTAL STANDALONE AI RAM INFERENCE FOOTPRINT: {total_ai_ram:.1f} MB")
    print(f"  2 GB ANDROID SAFETY CEILING:                  1800.0 MB")
    print(f"  AVAILABLE MEMORY HEADROOM:                    {1800.0 - total_ai_ram:.1f} MB")
    print(f"  OVERALL AUDIT VERDICT:                        {'PASS' if (asr_ok and trans_ok and tts_ok and total_ai_ram < 1800.0) else 'FAIL'}")
    print(f"==========================================================================\n")

if __name__ == "__main__":
    verify_package()
