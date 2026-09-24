import sys
import numpy as np
import soundfile as sf
import onnxruntime as ort
from transformers import WhisperProcessor, WhisperTokenizer

sys.stdout.reconfigure(encoding="utf-8")

processor = WhisperProcessor.from_pretrained("app/src/main/assets/tokenizers/asr")
tokenizer = WhisperTokenizer.from_pretrained("app/src/main/assets/tokenizers/asr")

enc_path = "app/src/main/assets/models/asr/encoder_model.onnx"
dec_path = "app/src/main/assets/models/asr/decoder_model_quant.onnx"

sess_opts = ort.SessionOptions()
sess_opts.intra_op_num_threads = 4
enc_sess = ort.InferenceSession(enc_path, sess_opts, providers=["CPUExecutionProvider"])
dec_sess = ort.InferenceSession(dec_path, sess_opts, providers=["CPUExecutionProvider"])

audio, sr = sf.read("VaniShiksha/android_ai/samples/sample_audio/test_sentence_4.wav")
features = processor(audio, sampling_rate=16000, return_tensors="np").input_features.astype(np.float32)
enc_out = enc_sess.run(["last_hidden_state"], {"input_features": features})[0]

prompt_tokens = [50258, 50276, 50359, 50363]
# Force step 19 to be 31945 (ं)
s4_base_tokens = [8485, 255, 17937, 25411, 36158, 8485, 237, 41858, 48449, 44500, 17937, 35082, 8485, 99, 21981, 3941, 114, 37139, 43372, 31945]
tokens = list(prompt_tokens) + s4_base_tokens

print("=== TRACING AFTER 31945 ===")
for step in range(25):
    input_ids = np.array([tokens], dtype=np.int64)
    logits = dec_sess.run(["logits"], {"input_ids": input_ids, "encoder_hidden_states": enc_out})[0]
    last_logits = logits[0, -1, :].copy()
    
    for sp in range(50258, 50364):
        last_logits[sp] = -1e9
        
    raw_top5 = np.argsort(last_logits)[-5:][::-1]
    next_tok = int(np.argmax(last_logits))
    print(f"Step {step}: next={next_tok} ({tokenizer.decode([next_tok]) if next_tok < 50257 else 'SPECIAL'}) | Top 5: {[(int(t), float(logits[0,-1,t])) for t in raw_top5]}")
    if next_tok == 50257 or next_tok >= 50364:
        print("STOPPED at token", next_tok)
        break
    tokens.append(next_tok)

print("Decoded:", tokenizer.decode(tokens[4:], skip_special_tokens=True))
