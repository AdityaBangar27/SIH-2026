import os
import sys
from pathlib import Path
from transformers import WhisperTokenizer

# Ensure utf-8 output
sys.stdout.reconfigure(encoding="utf-8")

tok_dir = "app/src/main/assets/tokenizers/asr"
tok = WhisperTokenizer.from_pretrained(tok_dir)

test_sentences = [
    "मेरा भारत महान जय हिंद",
    "नमस्ते",
    "आज हम पढ़ाई करेंगे",
    "भारत एक महान देश है",
    "किताब खोलो और पाठ पढ़ो",
    "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है",
    "मेरा नाम आदित्य है",
    "मेरा भारत महान"
]

print("=== TOKENIZER TEST ===")
for text in test_sentences:
    ids = tok.encode(text, add_special_tokens=False)
    decoded_together = tok.decode(ids)
    decoded_individually = [tok.decode([i]) for i in ids]
    raw_tokens = tok.convert_ids_to_tokens(ids)
    print(f"\nOriginal: '{text}'")
    print(f"IDs: {ids}")
    print(f"Decoded together: '{decoded_together}'")
    print(f"Decoded per token: {decoded_individually}")
    print(f"Raw tokens: {raw_tokens}")
