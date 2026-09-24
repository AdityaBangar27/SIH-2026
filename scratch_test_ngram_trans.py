import sys
import numpy as np
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent / "VaniShiksha" / "android_ai" / "conversion"))
from benchmark_translation import StandaloneOnnxTranslator, _past_feed

sys.stdout.reconfigure(encoding="utf-8")

translator = StandaloneOnnxTranslator()

def translate_with_ngram_blocking(text, no_repeat_ngram_size=3):
    prefixed = f"hin_Deva sat_Olck {text.strip()}"
    encoded = translator.src_tok.encode(prefixed)
    input_ids = np.array([[i if i < translator.meta["src_dict_size"] else translator.meta["unk_id"] for i in encoded.ids]], dtype=np.int64)
    attn_mask = np.array([encoded.attention_mask], dtype=np.int64)

    enc_out = translator.enc_sess.run(["last_hidden_state"], {"input_ids": input_ids, "attention_mask": attn_mask})[0]

    decoder_input_ids = np.array([[translator.decoder_start_id]], dtype=np.int64)
    output_ids = [translator.decoder_start_id]
    past_outputs = None

    for step in range(64):
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
        last_l = logits[0, -1, :].copy()

        # Apply no_repeat_ngram_size
        if no_repeat_ngram_size > 0 and len(output_ids) >= no_repeat_ngram_size:
            prefix = tuple(output_ids[-(no_repeat_ngram_size - 1):])
            for i in range(len(output_ids) - no_repeat_ngram_size + 1):
                if tuple(output_ids[i : i + no_repeat_ngram_size - 1]) == prefix:
                    blocked = output_ids[i + no_repeat_ngram_size - 1]
                    last_l[blocked] = -1e9

        next_id = int(np.argmax(last_l))
        output_ids.append(next_id)
        if next_id == translator.eos_id:
            break
        decoder_input_ids = np.array([[next_id]], dtype=np.int64)

    return translator.tgt_tok.decode(output_ids, skip_special_tokens=True).strip()

for s in ["नमस्ते", "मेरा नाम आदित्य है", "मेरा भारत महान", "आज हम पढ़ाई करेंगे", "किताब खोलो और पाठ पढ़ो", "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है और यह हमें आगे बढ़ाती है"]:
    print(f"\nHindi:   '{s}'")
    for ng in [0, 3, 4]:
        out = translate_with_ngram_blocking(s, no_repeat_ngram_size=ng)
        print(f"  ngram={ng}: '{out}'")
