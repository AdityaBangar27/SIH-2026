import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent / "VaniShiksha"))
import numpy as np
import soundfile as sf
import onnxruntime as ort
from transformers import WhisperProcessor, WhisperTokenizer
from src.tts import OfflineTTSManager

sys.stdout.reconfigure(encoding="utf-8")

tts = OfflineTTSManager.get_instance()
wav_path = "VaniShiksha/audio/tts_output/test_aditya.wav"

processor = WhisperProcessor.from_pretrained("app/src/main/assets/tokenizers/asr")
tokenizer = WhisperTokenizer.from_pretrained("app/src/main/assets/tokenizers/asr")

enc_path = "app/src/main/assets/models/asr/encoder_model.onnx"
dec_path = "app/src/main/assets/models/asr/decoder_model_quant.onnx"

sess_opts = ort.SessionOptions()
sess_opts.intra_op_num_threads = 4
enc_sess = ort.InferenceSession(enc_path, sess_opts, providers=["CPUExecutionProvider"])
dec_sess = ort.InferenceSession(dec_path, sess_opts, providers=["CPUExecutionProvider"])

audio, sr = sf.read(wav_path)
if audio.ndim > 1: audio = np.mean(audio, axis=1)

features = processor(audio, sampling_rate=16000, return_tensors="np").input_features.astype(np.float32)
enc_out = enc_sess.run(["last_hidden_state"], {"input_features": features})[0]

prompt_tokens = [50258, 50276, 50359, 50363]
tokens = list(prompt_tokens)
for _ in range(64):
    input_ids = np.array([tokens], dtype=np.int64)
    logits = dec_sess.run(["logits"], {"input_ids": input_ids, "encoder_hidden_states": enc_out})[0]
    last_logits = logits[0, -1, :].copy()
    for sp in range(50258, 50364):
        last_logits[sp] = -1e9
    next_tok = int(np.argmax(last_logits))
    if next_tok == 50257 or next_tok >= 50364:
        break
    tokens.append(next_tok)

gen_tokens = tokens[len(prompt_tokens):]
decoded = tokenizer.decode(gen_tokens, skip_special_tokens=True).strip()
print("Expected: मेरा नाम आदित्य है")
print(f"Decoded:  {decoded}")
print(f"Tokens:   {gen_tokens}")
