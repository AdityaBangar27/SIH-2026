import os
import sys
import time
from pathlib import Path

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

import torch
import numpy as np
import soundfile as sf
from parler_tts import ParlerTTSForConditionalGeneration
from transformers import AutoTokenizer

from src.config import SANTALI_SPEAKER_PROMPTS, TTS_OUTPUT_DIR
from src.audio_validator import validate_audio_file

def test_dtype_and_sdpa():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    repo_id = "RXD03/indic-parler-tts"
    
    test_text = "ᱡᱚᱦᱟᱨ, ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱢᱟ?"
    description = SANTALI_SPEAKER_PROMPTS.get("female", "A female speaker delivers speech with moderate speed and natural tone.")
    
    prompt_tok = AutoTokenizer.from_pretrained(repo_id)
    desc_tok = AutoTokenizer.from_pretrained("google/flan-t5-large")
    
    desc_inputs = desc_tok(description, return_tensors="pt")
    prompt_inputs = prompt_tok(test_text, return_tensors="pt")
    
    desc_input_ids = desc_inputs.input_ids.to(device)
    desc_attn = desc_inputs.attention_mask.to(device)
    prompt_input_ids = prompt_inputs.input_ids.to(device)
    prompt_attn = prompt_inputs.attention_mask.to(device)
    
    char_count = len(test_text)
    expected_sec = max(2.0, char_count * 0.08 + 0.8)
    min_new_tokens = max(60, int(expected_sec * 0.5 * 86.13))
    max_new_tokens = max(200, min(800, int(expected_sec * 1.5 * 86.13 + 60)))
    
    configs = [
        {"name": "FP32 Default", "dtype": torch.float32, "attn": "eager"},
        {"name": "FP32 SDPA", "dtype": torch.float32, "attn": "sdpa"},
        {"name": "FP16 SDPA", "dtype": torch.float16, "attn": "sdpa"},
        {"name": "BF16 SDPA", "dtype": torch.bfloat16, "attn": "sdpa"},
    ]
    
    for cfg in configs:
        print(f"\n==========================================")
        print(f"Testing Config: {cfg['name']}")
        print(f"==========================================")
        try:
            torch.cuda.empty_cache()
            t0_load = time.time()
            kwargs = {"torch_dtype": cfg["dtype"]}
            if cfg["attn"] == "sdpa":
                kwargs["attn_implementation"] = "sdpa"
                
            model = ParlerTTSForConditionalGeneration.from_pretrained(
                repo_id,
                **kwargs
            ).to(device)
            model.eval()
            t_load = time.time() - t0_load
            print(f"Loaded in {t_load:.2f}s | Allocated VRAM: {torch.cuda.memory_allocated() / (1024**2):.1f} MB")
            
            # Warm up
            print("Warming up...")
            with torch.inference_mode():
                _ = model.generate(
                    input_ids=desc_input_ids,
                    attention_mask=desc_attn,
                    prompt_input_ids=prompt_input_ids,
                    prompt_attention_mask=prompt_attn,
                    do_sample=True,
                    temperature=1.0,
                    min_new_tokens=min_new_tokens,
                    max_new_tokens=max_new_tokens,
                )
            torch.cuda.synchronize()
            
            # 3 benchmark runs
            latencies = []
            durations = []
            for r in range(3):
                torch.cuda.synchronize()
                t0 = time.time()
                with torch.inference_mode():
                    gen = model.generate(
                        input_ids=desc_input_ids,
                        attention_mask=desc_attn,
                        prompt_input_ids=prompt_input_ids,
                        prompt_attention_mask=prompt_attn,
                        do_sample=True,
                        temperature=1.0,
                        min_new_tokens=min_new_tokens,
                        max_new_tokens=max_new_tokens,
                    )
                torch.cuda.synchronize()
                t_gen = time.time() - t0
                
                audio_arr = gen.cpu().numpy().squeeze().astype(np.float32)
                sample_rate = model.config.sampling_rate
                dur = len(audio_arr) / float(sample_rate)
                
                # Check for NaN / Inf
                has_nan = np.isnan(audio_arr).any() or np.isinf(audio_arr).any()
                
                # Max and RMS
                max_val = float(np.max(np.abs(audio_arr)))
                rms = float(np.sqrt(np.mean(audio_arr**2)))
                
                out_path = TTS_OUTPUT_DIR / f"test_opt_{cfg['name'].replace(' ', '_')}_{r}.wav"
                if max_val > 1e-6:
                    audio_arr = (audio_arr / max_val) * 0.95
                sf.write(str(out_path), audio_arr, sample_rate)
                
                val = validate_audio_file(out_path)
                
                latencies.append(t_gen)
                durations.append(dur)
                print(f"  Run {r+1}: {t_gen:.2f}s | Dur: {dur:.2f}s | RMS: {rms:.4f} | Has NaN: {has_nan} | Valid: {val['valid_audio']}")
                
            avg_lat = sum(latencies) / len(latencies)
            print(f"  --> Average Latency: {avg_lat:.2f}s")
            del model
            torch.cuda.empty_cache()
            
        except Exception as e:
            print(f"  FAILED Config {cfg['name']}: {e}")

if __name__ == "__main__":
    test_dtype_and_sdpa()
