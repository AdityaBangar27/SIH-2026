import sys
import time
from pathlib import Path

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

from src.tts import OfflineTTSManager
from aksharamukha import transliterate as ak

tts = OfflineTTSManager.get_instance()
olchiki_text = "ᱥᱟᱱᱟᱢ ᱠᱚ ᱜᱮ ᱥᱟᱹᱜᱩᱱ ᱡᱚᱦᱟᱨ, ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱥᱮᱪᱮᱫ ᱵᱟᱵᱚᱛ ᱵᱚᱱ ᱜᱟᱞᱢᱟᱨᱟᱣᱟ᱾"
dev_phonetics = ak.process("Santali", "Devanagari", olchiki_text)
print(f"Ol Chiki:   {olchiki_text}")
print(f"Phonetics:  {dev_phonetics}")

t0 = time.time()
res = tts.synthesize_hindi(dev_phonetics, output_filename="mms_santali_test.wav")
lat = time.time() - t0
print(f"Synthesized in: {lat:.3f}s | Audio duration: {res.get('duration_sec', 0):.2f}s | Status: {res.get('status')}")
