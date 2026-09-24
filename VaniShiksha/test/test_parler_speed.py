import os
import sys
import time
from pathlib import Path
import torch
import soundfile as sf
from parler_tts import ParlerTTSForConditionalGeneration
from transformers import AutoTokenizer

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent

num_threads = max(1, min(8, os.cpu_count() or 4))
torch.set_num_threads(num_threads)

print(f"[Init] Loading Parler-TTS with {num_threads} threads...", flush=True)
model = ParlerTTSForConditionalGeneration.from_pretrained("RXD03/indic-parler-tts")
model.eval()
tokenizer = AutoTokenizer.from_pretrained("RXD03/indic-parler-tts")

prompt = "ᱡᱚᱦᱟᱨ"
desc = "A female speaker speaks Santali."

desc_inputs = tokenizer(desc, return_tensors="pt")
prompt_inputs = tokenizer(prompt, return_tensors="pt")

for max_tok in [30, 50, 80]:
    t0 = time.time()
    with torch.no_grad():
        gen = model.generate(
            input_ids=desc_inputs.input_ids,
            attention_mask=desc_inputs.attention_mask,
            prompt_input_ids=prompt_inputs.input_ids,
            prompt_attention_mask=prompt_inputs.attention_mask,
            max_new_tokens=max_tok,
            do_sample=False,
        )
    elapsed = time.time() - t0
    audio_arr = gen.cpu().numpy().squeeze()
    sr = model.config.sampling_rate
    dur = len(audio_arr) / sr
    print(f"[max_new_tokens={max_tok}] Generated {dur:.2f}s audio in {elapsed:.2f}s (Speed: {dur/elapsed:.2f}x real-time)", flush=True)
