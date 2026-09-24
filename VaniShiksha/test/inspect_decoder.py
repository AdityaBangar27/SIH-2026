import torch
from parler_tts import ParlerTTSForConditionalGeneration

model = ParlerTTSForConditionalGeneration.from_pretrained("RXD03/indic-parler-tts", torch_dtype=torch.float16)
print("Decoder class:", model.decoder.__class__)
print("Decoder config attn_implementation:", getattr(model.decoder.config, "_attn_implementation", None))
print("Decoder layers[0] self_attn:", type(model.decoder.model.layers[0].self_attn))
