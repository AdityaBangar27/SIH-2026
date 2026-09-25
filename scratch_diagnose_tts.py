import sys
import json

sys.stdout.reconfigure(encoding="utf-8")

cfg = json.load(open('app/src/main/assets/models/tts/sat_piper_model.onnx.json', encoding='utf-8'))
id_map = cfg['phoneme_id_map']
print('id_map size:', len(id_map))

ol_chiki_sample = 'ᱡᱚᱦᱟᱨ'
for c in ol_chiki_sample:
    print(repr(c), 'in id_map?', c in id_map, id_map.get(c))

# Now let's check what TTSManager.java does:
# In TTSManager.java:
# santhaliToIpa("ᱡᱚᱦᱟᱨ") -> converts Ol Chiki to IPA symbols:
# ᱡ -> ɟ
# ᱚ -> ɔ
# ᱦ -> h
# ᱟ -> a
# ᱨ -> r
# IPA: "ɟɔhar"
ipa_sample = "ɟɔhar"
print("Checking IPA characters for ᱡᱚᱦᱟᱨ:")
for c in ipa_sample:
    print(repr(c), 'in id_map?', c in id_map, id_map.get(c))
