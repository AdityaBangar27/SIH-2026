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

# Whisper prompt: <|startoftranscript|>, <|hi|>, <|transcribe|>, <|notimestamps|>
prompt_tokens = [50258, 50276, 50359, 50363]
eos_id = 50257

test_files = [
    (1, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_1.wav", "मेरा भारत महान जय हिंद"),
    (2, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_2.wav", "नमस्ते"),
    (3, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_3.wav", "आज हम पढ़ाई करेंगे"),
    (4, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_4.wav", "भारत एक महान देश है"),
    (5, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_5.wav", "किताब खोलो और पाठ पढ़ो"),
    (6, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_6.wav", "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है और यह हमें आगे बढ़ाती है"),
]

for tc_id, wav_path, expected in test_files:
    audio, sr = sf.read(wav_path)
    if audio.ndim > 1: audio = np.mean(audio, axis=1)
    
    features = processor(audio, sampling_rate=16000, return_tensors="np").input_features.astype(np.float32)
    enc_out = enc_sess.run(["last_hidden_state"], {"input_features": features})[0]
    
    # 1. Pure greedy search (standard Whisper)
    tokens = list(prompt_tokens)
    for _ in range(64):
        input_ids = np.array([tokens], dtype=np.int64)
        logits = dec_sess.run(["logits"], {"input_ids": input_ids, "encoder_hidden_states": enc_out})[0]
        # argmax on last token logits
        next_tok = int(np.argmax(logits[0, -1, :]))
        if next_tok == eos_id:
            break
        tokens.append(next_tok)
    
    gen_tokens = tokens[len(prompt_tokens):]
    decoded_text = tokenizer.decode(gen_tokens, skip_special_tokens=True).strip()
    
    # Now simulate what Android's ASRManager did:
    # 2. Android's repetition penalty + token-by-token decode
    android_tokens = list(prompt_tokens)
    for _ in range(64):
        input_ids = np.array([android_tokens], dtype=np.int64)
        logits = dec_sess.run(["logits"], {"input_ids": input_ids, "encoder_hidden_states": enc_out})[0]
        last_logits = logits[0, -1, :].copy()
        
        # Repetition penalty 1.2
        for t in range(4, len(android_tokens)):
            prev_tok = android_tokens[t]
            val = last_logits[prev_tok]
            if val > 0:
                last_logits[prev_tok] = val / 1.2
            else:
                last_logits[prev_tok] = val * 1.2
                
        # 3-gram blocking
        if len(android_tokens) >= 6:
            t_m2 = android_tokens[-2]
            t_m1 = android_tokens[-1]
            for i in range(4, len(android_tokens) - 2):
                if android_tokens[i] == t_m2 and android_tokens[i+1] == t_m1:
                    last_logits[android_tokens[i+2]] = -1e9
                    
        # argmax
        # suppress special control tokens if not EOS
        for sp in range(50257, len(last_logits)):
            if sp != eos_id:
                last_logits[sp] = -1e9
                
        next_tok = int(np.argmax(last_logits))
        if next_tok == eos_id:
            break
        android_tokens.append(next_tok)
        
    android_gen_tokens = android_tokens[len(prompt_tokens):]
    android_decoded_text = tokenizer.decode(android_gen_tokens, skip_special_tokens=True).strip()
    
    print(f"\n--- TC {tc_id} ---")
    print(f"Expected:       '{expected}'")
    print(f"Pure Greedy:    '{decoded_text}' (Tokens: {gen_tokens})")
    print(f"With RepPenalty:'{android_decoded_text}' (Tokens: {android_gen_tokens})")
