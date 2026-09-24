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
from parler_tts import ParlerTTSForConditionalGeneration
from transformers import AutoTokenizer

device = torch.device("cuda")
repo_id = "RXD03/indic-parler-tts"

tok = AutoTokenizer.from_pretrained(repo_id)
desc_tok = AutoTokenizer.from_pretrained("google/flan-t5-large")

model = ParlerTTSForConditionalGeneration.from_pretrained(repo_id, torch_dtype=torch.float16).to(device)
model.eval()

# Explicitly ensure use_cache is enabled
model.config.use_cache = True
model.decoder.config.use_cache = True

desc = "Sumitra speaks with a clear, warm voice at a moderate pace in a quiet room."
desc_in = desc_tok(desc, return_tensors="pt")
d_ids = desc_in.input_ids.to(device)
d_att = desc_in.attention_mask.to(device)

sentences = [
    "ᱡᱚᱦᱟᱨ, ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱢᱟ?",
    "ᱛᱮᱦᱮᱧ ᱦᱚᱭᱦᱩᱫᱤᱥ ᱫᱚ ᱟᱹᱰᱤ ᱱᱟᱯᱟᱭ ᱟᱨ ᱡᱚᱛᱚ ᱦᱚᱲ ᱜᱮ ᱱᱟᱯᱟᱭ ᱾",
    "ᱥᱮᱪᱮᱫ ᱦᱩᱭᱩᱜ ᱠᱟᱱᱟ ᱦᱚᱲ ᱠᱚᱣᱟᱜ ᱡᱤᱭᱚᱱ ᱨᱮᱭᱟᱜ ᱡᱚᱛᱚ ᱠᱷᱚᱱ ᱞᱟᱹᱠᱛᱤᱭᱟᱱ ᱫᱟᱲᱮ ᱾",
    "ᱟᱢ ᱥᱟᱱᱟᱢᱟᱜ ᱢᱤᱫ ᱥᱟᱶᱛᱮ ᱱᱚᱶᱟ ᱠᱟᱹᱢᱤ ᱠᱚᱨᱟᱣ ᱢᱮ ᱾",
]

for s in sentences:
    p_in = tok(s, return_tensors="pt")
    p_ids = p_in.input_ids.to(device)
    p_att = p_in.attention_mask.to(device)
    
    char_count = len(s)
    expected_sec = max(1.8, min(5.0, char_count * 0.05 + 0.5))
    min_new_tokens = max(40, int(expected_sec * 0.6 * 86.13))
    max_new_tokens = max(120, min(380, int(expected_sec * 1.1 * 86.13 + 10)))
    
    torch.cuda.synchronize()
    t0 = time.time()
    with torch.inference_mode():
        gen = model.generate(
            input_ids=d_ids,
            attention_mask=d_att,
            prompt_input_ids=p_ids,
            prompt_attention_mask=p_att,
            do_sample=True,
            temperature=1.0,
            use_cache=True,
            min_new_tokens=min_new_tokens,
            max_new_tokens=max_new_tokens,
        )
    torch.cuda.synchronize()
    lat = time.time() - t0
    dur = len(gen.cpu().numpy().squeeze()) / 44100.0
    toks = int(dur * 86.13)
    print(f"Text ({char_count} chars): '{s[:25]}...' | Gen: {lat:.2f}s | Audio: {dur:.2f}s | Speed: {toks/lat:.1f} tok/s")
