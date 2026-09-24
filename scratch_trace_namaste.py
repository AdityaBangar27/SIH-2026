import sys
import numpy as np
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent / "VaniShiksha" / "android_ai" / "conversion"))
from benchmark_translation import StandaloneOnnxTranslator, _past_feed

sys.stdout.reconfigure(encoding="utf-8")

translator = StandaloneOnnxTranslator()
text = "नमस्ते"
prefixed = f"hin_Deva sat_Olck {text.strip()}"
encoded = translator.src_tok.encode(prefixed)
input_ids = np.array([[i if i < translator.meta["src_dict_size"] else translator.meta["unk_id"] for i in encoded.ids]], dtype=np.int64)
attn_mask = np.array([encoded.attention_mask], dtype=np.int64)

enc_out = translator.enc_sess.run(["last_hidden_state"], {"input_ids": input_ids, "attention_mask": attn_mask})[0]

decoder_input_ids = np.array([[translator.decoder_start_id]], dtype=np.int64)
output_ids = [translator.decoder_start_id]
past_outputs = None

print("=== STEP BY STEP DECODING FOR 'नमस्ते' ===")
for step in range(15):
    if step == 0:
        dec_out = translator.dec_sess.run(
            None,
            {
                "input_ids": decoder_input_ids,
                "encoder_hidden_states": enc_out,
                "encoder_attention_mask": attn_mask
            }
        )
    else:
        dec_out = translator.dec_past_sess.run(
            None,
            {
                "input_ids": decoder_input_ids,
                "encoder_attention_mask": attn_mask,
                **_past_feed(past_outputs, translator.num_layers)
            }
        )
    logits = dec_out[0]
    past_outputs = list(dec_out[1:])
    last_l = logits[0, -1, :]
    
    # Top 5 tokens
    top5_idx = np.argsort(last_l)[-5:][::-1]
    top5_toks = [(idx, translator.tgt_tok.id_to_token(int(idx)), float(last_l[idx])) for idx in top5_idx]
    
    next_id = int(np.argmax(last_l))
    tok_str = translator.tgt_tok.id_to_token(next_id)
    print(f"Step {step}: next_id={next_id} ('{tok_str}') | Top 5: {top5_toks}")
    output_ids.append(next_id)
    if next_id == translator.eos_id:
        print("EOS reached!")
        break
    decoder_input_ids = np.array([[next_id]], dtype=np.int64)

print(f"Final translated text: '{translator.tgt_tok.decode(output_ids, skip_special_tokens=True)}'")
