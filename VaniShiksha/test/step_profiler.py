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

from src.hardware import get_hardware_info, get_system_device
from src.config import SANTALI_SPEAKER_PROMPTS, TTS_OUTPUT_DIR
from src.audio_validator import validate_audio_file

def run_tts_diagnostic():
    print("\n=======================================================")
    print("STEP 1 & 2: TTS SYSTEM & PIPELINE DETAILED INSTRUMENTATION")
    print("=======================================================\n")
    
    hw = get_hardware_info()
    device_str = get_system_device()
    device = torch.device(device_str)
    
    print(f"[TTS] Device: {device_str.upper()}")
    print(f"[TTS] GPU: {hw.get('device_name', 'N/A')}")
    if torch.cuda.is_available():
        vram = torch.cuda.get_device_properties(0).total_memory / (1024**3)
        print(f"[TTS] VRAM: {vram:.2f} GB")
    else:
        print("[TTS] VRAM: N/A (CPU)")
    print(f"[TTS] PyTorch: {hw.get('torch_version')}")
    print(f"[TTS] CUDA: {hw.get('cuda_version')}")
    
    # Model Loading (Measure Load Time once)
    print("\n--- Model Loading ---")
    t0_load = time.time()
    repo_id = "RXD03/indic-parler-tts"
    
    print(f"[TTS] Loading model from {repo_id}...")
    model = ParlerTTSForConditionalGeneration.from_pretrained(repo_id)
    model.to(device)
    model.eval()
    
    prompt_tok = AutoTokenizer.from_pretrained(repo_id)
    desc_repo = model.config.text_encoder._name_or_path
    desc_tok = AutoTokenizer.from_pretrained(desc_repo)
    t_load = time.time() - t0_load
    print(f"[TTS] Model load time: {t_load:.2f} sec")
    
    print(f"[TTS] Model dtype: {next(model.parameters()).dtype}")
    print(f"[TTS] Model device: {next(model.parameters()).device}")
    
    # Short Santali test text (Ol Chiki)
    # "ᱡᱚᱦᱟᱨ, ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱢᱟ?" -> "Johar, am ched leka menama?" (Hello, how are you?)
    test_text = "ᱡᱚᱦᱟᱨ, ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱢᱟ?"
    description = SANTALI_SPEAKER_PROMPTS.get("female", "A female speaker delivers speech with moderate speed and natural tone.")
    
    print(f"\n--- STEP 10: DIRECT TTS BENCHMARK ---")
    print(f"[TTS BENCHMARK] Input text: {test_text}")
    print(f"[TTS BENCHMARK] Input characters: {len(test_text)}")
    
    # Run multiple times to check Step 3 & 9 (First run vs subsequent runs)
    for run_idx in range(1, 6):
        print(f"\n==================== RUN {run_idx} ====================")
        t_start = time.time()
        
        # 1. Processor / Tokenization
        t0 = time.time()
        desc_inputs = desc_tok(description, return_tensors="pt")
        prompt_inputs = prompt_tok(test_text, return_tensors="pt")
        t_tokenize = time.time() - t0
        
        # 2. Input preparation & Device transfer
        t0 = time.time()
        desc_input_ids = desc_inputs.input_ids.to(device)
        desc_attn = desc_inputs.attention_mask.to(device)
        prompt_input_ids = prompt_inputs.input_ids.to(device)
        prompt_attn = prompt_inputs.attention_mask.to(device)
        t_transfer = time.time() - t0
        
        print(f"[TTS] Input device: {prompt_input_ids.device}")
        print(f"[TTS] Input tokens: {prompt_input_ids.shape[-1]}")
        print(f"[TTS] Dtype: {next(model.parameters()).dtype}")
        
        # 3. Model Generation
        char_count = len(test_text)
        expected_sec = max(2.0, char_count * 0.08 + 0.8)
        min_new_tokens = max(60, int(expected_sec * 0.5 * 86.13))
        max_new_tokens = max(200, min(800, int(expected_sec * 1.5 * 86.13 + 60)))
        
        t0 = time.time()
        if device.type == "cuda":
            torch.cuda.synchronize()
        with torch.inference_mode():
            generation = model.generate(
                input_ids=desc_input_ids,
                attention_mask=desc_attn,
                prompt_input_ids=prompt_input_ids,
                prompt_attention_mask=prompt_attn,
                do_sample=True,
                temperature=1.0,
                min_new_tokens=min_new_tokens,
                max_new_tokens=max_new_tokens,
            )
        if device.type == "cuda":
            torch.cuda.synchronize()
        t_generation = time.time() - t0
        
        # 4. Waveform processing
        t0 = time.time()
        audio_arr = generation.cpu().numpy().squeeze().astype(np.float32)
        sample_rate = model.config.sampling_rate
        num_samples = len(audio_arr)
        dur = num_samples / float(sample_rate)
        
        # Normalization
        max_val = float(np.max(np.abs(audio_arr)))
        if max_val > 1e-6:
            audio_arr = (audio_arr / max_val) * 0.95
        t_waveform = time.time() - t0
        
        # 5. Audio Encoding
        t0 = time.time()
        out_path = TTS_OUTPUT_DIR / f"bench_run_{run_idx}.wav"
        sf.write(str(out_path), audio_arr, sample_rate)
        t_encoding = time.time() - t0
        
        t_total = time.time() - t_start
        
        # Calculate tokens per second (audio codec tokens)
        generated_audio_tokens = int(dur * 86.13)
        tokens_per_sec = generated_audio_tokens / max(0.001, t_generation)
        
        # Audio validation
        val = validate_audio_file(out_path)
        
        print(f"[TTS] Processor/tokenization: {t_tokenize:.4f} sec")
        print(f"[TTS] Device transfer:        {t_transfer:.4f} sec")
        print(f"[TTS] Model generation:       {t_generation:.4f} sec")
        print(f"[TTS] Waveform processing:    {t_waveform:.4f} sec")
        print(f"[TTS] Audio encoding:         {t_encoding:.4f} sec")
        print(f"[TTS] Total:                  {t_total:.4f} sec")
        print(f"[TTS] Generated audio tokens: {generated_audio_tokens}")
        print(f"[TTS] Generation time:        {t_generation:.2f} sec")
        print(f"[TTS] Tokens/sec:             {tokens_per_sec:.2f}")
        print(f"[TTS] Audio Duration:         {dur:.2f} sec")
        print(f"[TTS] RMS Energy:             {val.get('rms', 0):.4f}")
        print(f"[TTS] Validation Valid:       {val.get('valid_audio')}")

if __name__ == "__main__":
    run_tts_diagnostic()
