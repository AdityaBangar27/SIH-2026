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
    
    tokens = list(prompt_tokens)
    for _ in range(64):
        input_ids = np.array([tokens], dtype=np.int64)
        logits = dec_sess.run(["logits"], {"input_ids": input_ids, "encoder_hidden_states": enc_out})[0]
        last_logits = logits[0, -1, :].copy()
        
        # Suppress all special tokens except eos_id (50257)
        # In Whisper: vocab_size is 51865. Special tokens start at 50257.
        # Specifically, if <|notimestamps|> is set, timestamp tokens (>= 50364) should be suppressed or stop generation.
        # What happens if we stop when next_tok == eos_id or next_tok >= 50257?
        # Let's test with timestamp tokens suppressed:
        for t in range(50364, len(last_logits)):
            last_logits[t] = -1e9
        for t in range(50258, 50364):
            last_logits[t] = -1e9
            
        next_tok = int(np.argmax(last_logits))
        if next_tok == eos_id or next_tok >= 50257:
            break
        tokens.append(next_tok)
        
    gen_tokens = tokens[len(prompt_tokens):]
    decoded = tokenizer.decode(gen_tokens, skip_special_tokens=True).strip()
    print(f"\n--- TC {tc_id} ---")
    print(f"Expected: '{expected}'")
    print(f"Decoded:  '{decoded}'")
    print(f"Tokens:   {gen_tokens}")
