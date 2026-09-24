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

def test_token_bounds():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    repo_id = "RXD03/indic-parler-tts"
    
    # Test with medium and long sentences
    sentences = [
        "ᱛᱮᱦᱮᱧ ᱦᱚᱭᱦᱩᱫᱤᱥ ᱫᱚ ᱟᱹᱰᱤ ᱱᱟᱯᱟᱭ ᱟᱨ ᱡᱚᱛᱚ ᱦᱚᱲ ᱜᱮ ᱱᱟᱯᱟᱭ ᱾",  # TC 2
        "ᱵᱷᱟᱨᱚᱛ ᱢᱤᱫ ᱢᱟᱨᱟᱝ ᱫᱤᱥᱚᱢ ᱠᱟᱱᱟ ᱡᱟᱦᱟᱸ ᱨᱮ ᱟᱭᱢᱟ ᱯᱟᱹᱨᱥᱤ ᱟᱨ ᱞᱟᱠᱪᱟᱨ ᱢᱤᱫ ᱥᱟᱶᱛᱮ ᱛᱟᱦᱮᱸᱱᱟ ᱾", # TC 3
    ]
    
    description = SANTALI_SPEAKER_PROMPTS.get("female")
    prompt_tok = AutoTokenizer.from_pretrained(repo_id)
    desc_tok = AutoTokenizer.from_pretrained("google/flan-t5-large")
    
    desc_inputs = desc_tok(description, return_tensors="pt")
    desc_input_ids = desc_inputs.input_ids.to(device)
    desc_attn = desc_inputs.attention_mask.to(device)
    
    model = ParlerTTSForConditionalGeneration.from_pretrained(
        repo_id,
        torch_dtype=torch.float16,
    ).to(device)
    model.eval()
    
    for idx, text in enumerate(sentences, 1):
        prompt_inputs = prompt_tok(text, return_tensors="pt")
        prompt_input_ids = prompt_inputs.input_ids.to(device)
        prompt_attn = prompt_inputs.attention_mask.to(device)
        
        char_count = len(text)
        expected_sec = max(1.8, min(5.5, char_count * 0.055 + 0.6))
        min_new_tokens = max(50, int(expected_sec * 0.65 * 86.13))
        max_new_tokens = max(140, min(420, int(expected_sec * 1.15 * 86.13 + 20)))
        
        print(f"\nSentence {idx} ({char_count} chars): '{text}'", flush=True)
        print(f"  expected_sec: {expected_sec:.2f}s | min_tokens: {min_new_tokens} | max_tokens: {max_new_tokens}", flush=True)
        
        torch.cuda.synchronize()
        t0 = time.time()
        with torch.inference_mode():
            gen = model.generate(
                input_ids=desc_input_ids,
                attention_mask=desc_attn,
                prompt_input_ids=prompt_input_ids,
                prompt_attention_mask=prompt_attn,
                do_sample=True,
                temperature=0.95,
                min_new_tokens=min_new_tokens,
                max_new_tokens=max_new_tokens,
            )
        torch.cuda.synchronize()
        t_gen = time.time() - t0
        
        audio_arr = gen.cpu().numpy().squeeze().astype(np.float32)
        dur = len(audio_arr) / float(model.config.sampling_rate)
        
        out_p = TTS_OUTPUT_DIR / f"test_bound_{idx}.wav"
        sf.write(str(out_p), (audio_arr / max(1e-6, np.max(np.abs(audio_arr)))) * 0.95, model.config.sampling_rate)
        val = validate_audio_file(out_p)
        
        print(f"  --> Gen Latency: {t_gen:.2f}s | Audio Dur: {dur:.2f}s | RMS: {val.get('rms', 0):.4f} | Valid: {val['valid_audio']}", flush=True)

if __name__ == "__main__":
    test_token_bounds()
