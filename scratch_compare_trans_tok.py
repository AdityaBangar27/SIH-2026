import sys
import json
from pathlib import Path
from tokenizers import Tokenizer

sys.stdout.reconfigure(encoding="utf-8")

trans_dir = Path("app/src/main/assets/tokenizers/translation")
src_tok = Tokenizer.from_file(str(trans_dir / "tokenizer_src.json"))
with open(trans_dir / "dict.SRC.json", "r", encoding="utf-8") as f:
    src_dict = json.load(f)

meta = json.loads((trans_dir / "tokenizer_meta.json").read_text(encoding="utf-8"))

def java_tokenize_source(input_str):
    ids = [8, 29925] # HIN_DEVA_ID, SAT_OLCK_ID
    words = input_str.split()
    for w in words:
        if not w: continue
        candidate = "\u2581" + w
        if candidate in src_dict:
            ids.append(src_dict[candidate])
        else:
            i = 0
            is_first = True
            while i < len(w):
                best_len = 0
                best_id = 3 # UNK
                for j in range(len(w), i, -1):
                    sub = ("\u2581" if is_first else "") + w[i:j]
                    if sub in src_dict:
                        best_len = j - i
                        best_id = src_dict[sub]
                        break
                if best_len > 0:
                    ids.append(best_id)
                    i += best_len
                else:
                    ids.append(3)
                    i += 1
                is_first = False
    ids.append(2) # EOS
    return ids

sentences = [
    "मेरा भारत महान जय हिंद",
    "नमस्ते",
    "आज हम पढ़ाई करेंगे",
    "भारत एक महान देश है",
    "किताब खोलो और पाठ पढ़ो",
    "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है और यह हमें आगे बढ़ाती है",
    "मेरा नाम आदित्य है",
    "मेरा भारत महान"
]

print("=== SOURCE TOKENIZER COMPARISON ===")
for s in sentences:
    prefixed = f"hin_Deva sat_Olck {s.strip()}"
    hf_ids = src_tok.encode(prefixed).ids
    java_ids = java_tokenize_source(s)
    match = (hf_ids == java_ids)
    print(f"\nSentence: '{s}'")
    print(f"Match: {match}")
    if not match:
        print(f"  HF:   {hf_ids}")
        print(f"  Java: {java_ids}")
