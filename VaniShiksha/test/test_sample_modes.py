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

def test_sampling_vs_greedy():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    repo_id = "RXD03/indic-parler-tts"
    
    test_text = "ᱡᱚᱦᱟᱨ, ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱢᱟ?"
    description = SANTALI_SPEAKER_PROMPTS.get("female", "Sumitra speaks with a clear, warm voice at a moderate pace in a quiet room.")
    
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
    
    model = ParlerTTSForConditionalGeneration.from_pretrained(
        repo_id,
        torch_dtype=torch.float16,
    ).to(device)
    model.eval()
    
    # Warmup
    with torch.inference_mode():
        _ = model.generate(
            input_ids=desc_input_ids,
            attention_mask=desc_attn,
            prompt_input_ids=prompt_input_ids,
            prompt_attention_mask=prompt_attn,
            do_sample=True,
            min_new_tokens=min_new_tokens,
            max_new_tokens=max_new_tokens,
        )
    torch.cuda.synchronize()
    
    print("\n--- Testing do_sample=True (Sampling) ---", flush=True)
    for r in range(2):
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
        dur = len(audio_arr) / float(model.config.sampling_rate)
        out_p = TTS_OUTPUT_DIR / f"test_sample_true_{r}.wav"
        sf.write(str(out_p), (audio_arr / max(1e-6, np.max(np.abs(audio_arr)))) * 0.95, model.config.sampling_rate)
        val = validate_audio_file(out_p)
        print(f"  Run {r+1}: {t_gen:.2f}s | Dur: {dur:.2f}s | RMS: {val.get('rms', 0):.4f} | Valid: {val['valid_audio']}", flush=True)

    print("\n--- Testing do_sample=False (Greedy Search) ---", flush=True)
    for r in range(2):
        torch.cuda.synchronize()
        t0 = time.time()
        with torch.inference_mode():
            gen = model.generate(
                input_ids=desc_input_ids,
                attention_mask=desc_attn,
                prompt_input_ids=prompt_input_ids,
                prompt_attention_mask=prompt_attn,
                do_sample=False,
                min_new_tokens=min_new_tokens,
                max_new_tokens=max_new_tokens,
            )
        torch.cuda.synchronize()
        t_gen = time.time() - t0
        audio_arr = gen.cpu().numpy().squeeze().astype(np.float32)
        dur = len(audio_arr) / float(model.config.sampling_rate)
        out_p = TTS_OUTPUT_DIR / f"test_sample_false_{r}.wav"
        sf.write(str(out_p), (audio_arr / max(1e-6, np.max(np.abs(audio_arr)))) * 0.95, model.config.sampling_rate)
        val = validate_audio_file(out_p)
        print(f"  Run {r+1}: {t_gen:.2f}s | Dur: {dur:.2f}s | RMS: {val.get('rms', 0):.4f} | Valid: {val['valid_audio']}", flush=True)

if __name__ == "__main__":
    test_sampling_vs_greedy()
