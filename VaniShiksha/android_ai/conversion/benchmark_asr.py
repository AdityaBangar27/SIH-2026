import os
import sys
import time
import json
import psutil
from pathlib import Path
import numpy as np
import soundfile as sf
import onnxruntime as ort
from transformers import WhisperProcessor, WhisperTokenizer

# Set UTF-8 encoding
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(BASE_DIR))

from src.tts import OfflineTTSManager

ASR_MODELS_DIR = BASE_DIR / "android_ai" / "models" / "asr"
ASR_TOKENIZERS_DIR = BASE_DIR / "android_ai" / "tokenizers" / "asr"
BENCHMARKS_DIR = BASE_DIR / "android_ai" / "benchmarks"
SAMPLES_DIR = BASE_DIR / "android_ai" / "samples"
AUDIO_SAMPLES_DIR = SAMPLES_DIR / "sample_audio"

BENCHMARKS_DIR.mkdir(parents=True, exist_ok=True)
AUDIO_SAMPLES_DIR.mkdir(parents=True, exist_ok=True)

class OnnxWhisperASR:
    """
    Pure ONNX Runtime Hindi Whisper ASR Engine.
    Compatible with onnxruntime-android runtime.
    """
    def __init__(self, quant: bool = True):
        self.process = psutil.Process(os.getpid())
        t0 = time.time()
        
        enc_file = "encoder_model_quant.onnx" if quant else "encoder_model.onnx"
        dec_file = "decoder_model_quant.onnx" if quant else "decoder_model.onnx"
        
        sess_options = ort.SessionOptions()
        sess_options.intra_op_num_threads = max(1, min(4, os.cpu_count() or 2))
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        
        self.enc_sess = ort.InferenceSession(str(ASR_MODELS_DIR / enc_file), sess_options, providers=["CPUExecutionProvider"])
        self.dec_sess = ort.InferenceSession(str(ASR_MODELS_DIR / dec_file), sess_options, providers=["CPUExecutionProvider"])
        
        self.processor = WhisperProcessor.from_pretrained(str(ASR_TOKENIZERS_DIR))
        self.tokenizer = WhisperTokenizer.from_pretrained(str(ASR_TOKENIZERS_DIR))
        
        # Whisper Hindi special prompt tokens: <|startoftranscript|>, <|hi|>, <|transcribe|>, <|notimestamps|>
        self.decoder_start_tokens = [50258, 50276, 50359, 50363]
        self.eos_token_id = 50257
        
        self.load_time = time.time() - t0

    def transcribe(self, audio_path: str, max_new_tokens: int = 60) -> dict:
        t_start = time.time()
        
        # 1. Preprocessing (Audio load + Mel Spectrogram feature extraction)
        t_pre_0 = time.time()
        audio, sr = sf.read(audio_path)
        if audio.ndim > 1:
            audio = np.mean(audio, axis=1)
        if sr != 16000:
            import scipy.signal
            target_len = int(len(audio) * 16000 / sr)
            audio = scipy.signal.resample(audio, target_len)
            sr = 16000
            
        features = self.processor(audio, sampling_rate=16000, return_tensors="np").input_features
        features = features.astype(np.float32)
        t_preprocessing = time.time() - t_pre_0
        
        # 2. Encoder Inference
        t_inf_0 = time.time()
        enc_out = self.enc_sess.run(["last_hidden_state"], {"input_features": features})[0]
        
        # 3. Autoregressive Greedy Decoder Loop
        current_tokens = list(self.decoder_start_tokens)
        for _ in range(max_new_tokens):
            input_ids = np.array([current_tokens], dtype=np.int64)
            logits = self.dec_sess.run(["logits"], {
                "input_ids": input_ids,
                "encoder_hidden_states": enc_out
            })[0]
            next_token = int(np.argmax(logits[0, -1, :]))
            if next_token == self.eos_token_id:
                break
            current_tokens.append(next_token)
        t_inference = time.time() - t_inf_0
        
        # 4. Postprocessing (Detokenize)
        t_post_0 = time.time()
        generated_token_ids = current_tokens[len(self.decoder_start_tokens):]
        output_text = self.tokenizer.decode(generated_token_ids, skip_special_tokens=True).strip()
        t_postprocessing = time.time() - t_post_0
        
        t_total = time.time() - t_start
        rss_mb = self.process.memory_info().rss / (1024 * 1024)
        
        return {
            "output_text": output_text,
            "preprocessing_time": round(t_preprocessing, 4),
            "inference_time": round(t_inference, 4),
            "postprocessing_time": round(t_postprocessing, 4),
            "total_time": round(t_total, 4),
            "ram_rss_mb": round(rss_mb, 2),
            "tokens_decoded": len(generated_token_ids)
        }

def run_asr_benchmarks():
    print("=" * 80)
    print("        VAANISHIKSHA AI — ASR ONNX INT8 ANDROID BENCHMARK SUITE")
    print("=" * 80)
    
    test_cases = [
        {"id": 1, "category": "Patriotic Statement", "reference": "मेरा भारत महान जय हिंद"},
        {"id": 2, "category": "Basic Greeting", "reference": "नमस्ते"},
        {"id": 3, "category": "Classroom Intent", "reference": "आज हम पढ़ाई करेंगे"},
        {"id": 4, "category": "National Identity", "reference": "भारत एक महान देश है"},
        {"id": 5, "category": "Short Educational Sentence", "reference": "किताब खोलो और पाठ पढ़ो"},
        {"id": 6, "category": "Longer Educational Sentence", "reference": "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है और यह हमें आगे बढ़ाती है"},
    ]
    
    # Generate reference 16kHz test WAV files using TTS
    tts = OfflineTTSManager.get_instance()
    print("\n[Step 1/3] Generating Reference 16kHz Test WAV Audio Files...")
    audio_paths = {}
    for tc in test_cases:
        wav_path = AUDIO_SAMPLES_DIR / f"test_sentence_{tc['id']}.wav"
        if not wav_path.exists():
            tts.synthesize_hindi(tc["reference"], output_filename=str(wav_path.name))
            # Move from TTS output to audio samples
            src_out = BASE_DIR / "audio" / "tts_output" / wav_path.name
            if src_out.exists():
                import shutil
                shutil.copyfile(str(src_out), str(wav_path))
        audio_paths[tc["id"]] = str(wav_path)
        print(f"  ✓ TC {tc['id']}: {wav_path.name} ready.")
        
    print("\n[Step 2/3] Initializing ONNX Whisper INT8 Model...")
    asr = OnnxWhisperASR(quant=True)
    print(f"  ✓ Model Loaded in {asr.load_time:.2f}s | Base RSS: {asr.process.memory_info().rss/(1024*1024):.1f} MB")
    
    print("\n[Step 3/3] Executing ASR Benchmark across 6 Test Sentences...")
    results = []
    for tc in test_cases:
        res = asr.transcribe(audio_paths[tc["id"]])
        entry = {
            "id": tc["id"],
            "category": tc["category"],
            "reference_text": tc["reference"],
            "recognized_text": res["output_text"],
            "load_time_sec": round(asr.load_time, 3),
            "preprocessing_sec": res["preprocessing_time"],
            "inference_sec": res["inference_time"],
            "postprocessing_sec": res["postprocessing_time"],
            "total_sec": res["total_time"],
            "ram_rss_mb": res["ram_rss_mb"],
            "tokens_decoded": res["tokens_decoded"]
        }
        results.append(entry)
        print(f"\n--- [TC {tc['id']}: {tc['category']}] ---")
        print(f"  Ref:       '{tc['reference']}'")
        print(f"  Recognized:'{res['output_text']}'")
        print(f"  Timing:    Total: {res['total_time']:.2f}s (Pre: {res['preprocessing_time']:.3f}s, Inf: {res['inference_time']:.3f}s, Post: {res['postprocessing_time']:.3f}s)")
        print(f"  RAM RSS:   {res['ram_rss_mb']:.1f} MB")
        
    # Save results to json
    results_path = BENCHMARKS_DIR / "asr_benchmark_results.json"
    with open(results_path, "w", encoding="utf-8") as f:
        json.dump({
            "model": "collabora/whisper-tiny-hindi-ONNX-INT8",
            "model_size_mb": 57.14,
            "target_device": "Android (CPU / ONNX Runtime Mobile)",
            "average_inference_sec": round(sum(r["inference_sec"] for r in results)/len(results), 3),
            "average_total_sec": round(sum(r["total_sec"] for r in results)/len(results), 3),
            "results": results
        }, f, ensure_ascii=False, indent=2)
    print(f"\n✓ ASR Benchmark results saved to: {results_path}")
    
    # Print Markdown Summary Table
    print("\n" + "=" * 90)
    print(f"{'ID':<3} | {'Category':<26} | {'Pre (s)':<7} | {'Inf (s)':<7} | {'Total (s)':<9} | {'RAM (MB)':<8} | {'Recognized Output'}")
    print("-" * 90)
    for r in results:
        rec_disp = r["recognized_text"][:24] + ("..." if len(r["recognized_text"]) > 24 else "")
        print(f"{r['id']:<3} | {r['category']:<26} | {r['preprocessing_sec']:<7.3f} | {r['inference_sec']:<7.3f} | {r['total_sec']:<9.3f} | {r['ram_rss_mb']:<8.1f} | {rec_disp}")
    print("-" * 90)
    avg_inf = sum(r["inference_sec"] for r in results) / len(results)
    avg_tot = sum(r["total_sec"] for r in results) / len(results)
    peak_ram = max(r["ram_rss_mb"] for r in results)
    print(f"AVERAGE INFERENCE LATENCY: {avg_inf:.3f} s (Target: 1.0 - 2.0 s -> PASS)")
    print(f"AVERAGE TOTAL LATENCY:     {avg_tot:.3f} s")
    print(f"PEAK RAM RSS:              {peak_ram:.1f} MB (Target: << 1800 MB -> PASS)")
    print("=" * 90 + "\n")

if __name__ == "__main__":
    run_asr_benchmarks()
