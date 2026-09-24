import sys
import numpy as np
import soundfile as sf
import onnxruntime as ort
from transformers import WhisperProcessor, WhisperTokenizer

sys.stdout.reconfigure(encoding="utf-8")

processor = WhisperProcessor.from_pretrained("app/src/main/assets/tokenizers/asr")
tokenizer = WhisperTokenizer.from_pretrained("app/src/main/assets/tokenizers/asr")

# Check all 6 test sentences:
enc_path = "app/src/main/assets/models/asr/encoder_model.onnx"
dec_path = "app/src/main/assets/models/asr/decoder_model_quant.onnx"

sess_opts = ort.SessionOptions()
sess_opts.intra_op_num_threads = 4
enc_sess = ort.InferenceSession(enc_path, sess_opts, providers=["CPUExecutionProvider"])
dec_sess = ort.InferenceSession(dec_path, sess_opts, providers=["CPUExecutionProvider"])

for s_num in [4, 5, 6]:
    wav_path = f"VaniShiksha/android_ai/samples/sample_audio/test_sentence_{s_num}.wav"
    audio, sr = sf.read(wav_path)
    features = processor(audio, sampling_rate=16000, return_tensors="np").input_features.astype(np.float32)
    enc_out = enc_sess.run(["last_hidden_state"], {"input_features": features})[0]
    
    tokens = [50258, 50276, 50359, 50363]
    for step in range(128):
        input_ids = np.array([tokens], dtype=np.int64)
        logits = dec_sess.run(["logits"], {"input_ids": input_ids, "encoder_hidden_states": enc_out})[0]
        last_l = logits[0, -1, :].copy()
        for sp in range(50258, 50364):
            last_l[sp] = -1e9
        nxt = int(np.argmax(last_l))
        if nxt == 50257 or nxt >= 50364:
            tokens.append(nxt)
            break
        tokens.append(nxt)
    gen = tokens[4:-1] if (tokens[-1] >= 50257) else tokens[4:]
    dec = tokenizer.decode(gen, skip_special_tokens=True).strip()
    print(f"\n--- S{s_num} ---")
    print(f"Decoded ({len(gen)} tokens): '{dec}'")
    print(f"Tokens: {tokens}")
