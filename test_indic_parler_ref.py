import os
import sys

# Auto-detect and switch to VaniShiksha .venv if torch is missing in current Python
try:
    import torch
except ImportError:
    venv_python = r"C:\Users\aliba\Desktop\VaniShiksha\.venv\Scripts\python.exe"
    if os.path.exists(venv_python) and os.path.normcase(sys.executable) != os.path.normcase(venv_python):
        import subprocess
        result = subprocess.run([venv_python] + sys.argv)
        sys.exit(result.returncode)
    else:
        raise

import time
import soundfile as sf
import numpy as np
import psutil
from parler_tts import ParlerTTSForConditionalGeneration
from transformers import AutoTokenizer

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

process = psutil.Process(os.getpid())
ram0 = process.memory_info().rss / (1024 * 1024)

print("=== PHASE 5: INDIC PARLER-TTS REFERENCE BENCHMARK ===")
device = "cuda" if torch.cuda.is_available() else "cpu"
dtype = torch.float16 if device == "cuda" else torch.float32
print(f"Using device: {device} ({dtype})")

t0 = time.time()
model = ParlerTTSForConditionalGeneration.from_pretrained("RXD03/indic-parler-tts", torch_dtype=dtype).to(device)
tokenizer = AutoTokenizer.from_pretrained("RXD03/indic-parler-tts")
t_load = time.time() - t0
ram_loaded = process.memory_info().rss / (1024 * 1024)
print(f"Model loaded in {t_load:.2f}s. RAM: {ram_loaded:.2f} MB (Delta: {ram_loaded - ram0:.2f} MB)")

test_cases = [
    {
        "lang": "Hindi",
        "text": "मेरा भारत महान जय हिंद।",
        "desc": "Divya speaks with a clear, warm voice at a moderate pace in a quiet room.",
        "speaker": "Divya (Hindi)",
        "out_file": "ref_hindi_parler.wav"
    },
    {
        "lang": "Santali",
        "text": "ᱥᱟᱱᱟᱢ ᱠᱚ ᱜᱮ ᱥᱟᱹᱜᱩᱱ ᱡᱚᱦᱟᱨ",
        "desc": "A female speaker speaks with a clear and pleasant tone at a normal pace in a quiet room.",
        "speaker": "Santali Female",
        "out_file": "ref_santali_parler.wav"
    }
]

for tc in test_cases:
    print(f"\n--- Testing {tc['lang']} ---")
    print(f"Text: {tc['text']}")
    print(f"Speaker/Description: {tc['desc']}")
    
    t_gen_start = time.time()
    desc_inputs = tokenizer(tc['desc'], return_tensors="pt")
    input_ids = desc_inputs.input_ids.to(device)
    attention_mask = desc_inputs.attention_mask.to(device)

    text_inputs = tokenizer(tc['text'], return_tensors="pt")
    prompt_input_ids = text_inputs.input_ids.to(device)
    prompt_attention_mask = text_inputs.attention_mask.to(device)
    
    with torch.no_grad():
        generation = model.generate(
            input_ids=input_ids,
            attention_mask=attention_mask,
            prompt_input_ids=prompt_input_ids,
            prompt_attention_mask=prompt_attention_mask,
            do_sample=True,
            temperature=1.0,
            min_new_tokens=40,
            max_new_tokens=250
        )
    t_gen = time.time() - t_gen_start
    ram_peak = process.memory_info().rss / (1024 * 1024)
    
    audio_arr = generation.cpu().numpy().squeeze().astype(np.float32)
    sr = model.config.sampling_rate
    dur = len(audio_arr) / sr
    
    max_val = np.max(np.abs(audio_arr))
    if max_val > 1e-6:
        audio_norm = (audio_arr / max_val) * 0.95
    else:
        audio_norm = audio_arr
        
    rms = float(np.sqrt(np.mean(audio_norm**2)))
    peak = float(np.max(np.abs(audio_norm)))
    
    sf.write(tc['out_file'], audio_norm, sr, subtype='PCM_16')
    file_size = os.path.getsize(tc['out_file'])
    
    print(f"Language: {tc['lang']}")
    print(f"Text: {tc['text']}")
    print(f"Model: AI4Bharat Indic Parler-TTS (RXD03/indic-parler-tts)")
    print(f"Speaker: {tc['speaker']}")
    print(f"Sample rate: {sr} Hz")
    print(f"Channels: 1 (Mono)")
    print(f"Bit depth: 16-bit PCM")
    print(f"Audio duration: {dur:.2f} s")
    print(f"Inference time: {t_gen:.2f} s")
    print(f"Model memory: {ram_peak:.2f} MB")
    print(f"Output file size: {file_size} bytes")
    print(f"RMS: {rms:.4f}")
    print(f"Peak: {peak:.4f}")
