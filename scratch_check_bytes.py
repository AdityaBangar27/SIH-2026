import sys
from transformers.models.whisper.tokenization_whisper import bytes_to_unicode

sys.stdout.reconfigure(encoding="utf-8")

b2u = bytes_to_unicode()
u2b = {v: k for k, v in b2u.items()}

print(f"Total mappings: {len(b2u)}")

# Test how Whisper decodes tokens by collecting bytes across tokens
from transformers import WhisperTokenizer
tok = WhisperTokenizer.from_pretrained("app/src/main/assets/tokenizers/asr")

for test_text in ["मेरा नाम आदित्य है", "नमस्ते", "मेरा भारत महान जय हिंद"]:
    ids = tok.encode(test_text, add_special_tokens=False)
    raw_tokens = tok.convert_ids_to_tokens(ids)
    
    # Simulate full byte collection:
    all_bytes = bytearray()
    for t in raw_tokens:
        for ch in t:
            if ch in u2b:
                all_bytes.append(u2b[ch])
            else:
                print(f"WARNING: char {ch} ({ord(ch)}) not in u2b")
    decoded_text = all_bytes.decode('utf-8', errors='replace')
    print(f"\nTarget: {test_text}")
    print(f"Byte-collected: {decoded_text}")
    print(f"Match: {decoded_text == test_text}")
