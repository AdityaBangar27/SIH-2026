from transformers import AutoTokenizer
from optimum.onnxruntime import ORTModelForSeq2SeqLM


MODEL_NAME = "TigreGotico/indictrans2-indic-indic-dist-320M-onnx"


print("Loading tokenizer...")

tokenizer = AutoTokenizer.from_pretrained(
    MODEL_NAME,
    trust_remote_code=True
)


print("Loading ONNX model...")

model = ORTModelForSeq2SeqLM.from_pretrained(
    MODEL_NAME,
    subfolder="int8",
    trust_remote_code=True
)


print("Model ready!\n")


def translate(text):

    # IndicTrans language codes
    source_lang = "hin_Deva"
    target_lang = "sat_Olck"

    # Add language information to input
    input_text = f"{source_lang} {target_lang} {text}"

    print("Processed input:")
    print(input_text)

    inputs = tokenizer(
        input_text,
        return_tensors="pt",
        truncation=True,
        max_length=256
    )

    print("\nGenerating translation...")

    output_tokens = model.generate(
        **inputs,
        max_new_tokens=128,
        num_beams=4
    )

    output = tokenizer.decode(
        output_tokens[0],
        skip_special_tokens=True
    )

    return output


if __name__ == "__main__":

    hindi_text = input("Enter Hindi sentence: ")

    translation = translate(hindi_text)

    print("\n==========================")

    print("Hindi:")
    print(hindi_text)

    print("\nSantali Translation:")
    print(translation)

    print("==========================")