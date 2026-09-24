import sys
import numpy as np
import soundfile as sf
from transformers import WhisperProcessor

sys.stdout.reconfigure(encoding="utf-8")

for num in [1, 2, 3, 4, 5, 6]:
    audio_path = f"VaniShiksha/android_ai/samples/sample_audio/test_sentence_{num}.wav"
    audio, sr = sf.read(audio_path)
    if audio.ndim > 1: audio = np.mean(audio, axis=1)

    processor = WhisperProcessor.from_pretrained("app/src/main/assets/tokenizers/asr")
    hf_features = processor(audio, sampling_rate=16000, return_tensors="np").input_features[0]

    with open("app/src/main/assets/tokenizers/asr/mel_filters.bin", "rb") as f:
        mel_filterbank = np.frombuffer(f.read(), dtype=np.float32).reshape(80, 201)

    N_FFT = 400
    HOP_LENGTH = 160
    N_MELS = 80
    N_FRAMES = 3000
    FFT_BINS = 201

    hann = 0.5 * (1.0 - np.cos(2.0 * np.pi * np.arange(N_FFT) / N_FFT))

    def get_padded_sample(audio_arr, idx):
        length = len(audio_arr)
        if length == 0: return 0.0
        if length == 1: return audio_arr[0]
        if idx < 0:
            ref = -idx
            if ref >= length: ref = length - 1
            return audio_arr[ref]
        elif idx >= length:
            ref = 2 * (length - 1) - idx
            if ref < 0: ref = 0
            return audio_arr[ref]
        return audio_arr[idx]

    active_frames = min(N_FRAMES, max(1, (len(audio) + 200) // HOP_LENGTH + 1))
    stft_frames = np.zeros((active_frames, FFT_BINS), dtype=np.float32)

    for frame in range(active_frames):
        offset = frame * HOP_LENGTH - 200
        windowed = np.zeros(N_FFT, dtype=np.float32)
        for i in range(N_FFT):
            windowed[i] = get_padded_sample(audio, offset + i) * hann[i]
        fft_res = np.fft.rfft(windowed, n=N_FFT)
        stft_frames[frame] = (fft_res.real**2 + fft_res.imag**2)

    mel_spec = np.zeros((N_MELS, N_FRAMES), dtype=np.float32)
    for m in range(N_MELS):
        filt = mel_filterbank[m]
        for frame in range(active_frames):
            mel_val = np.dot(filt, stft_frames[frame])
            mel_spec[m, frame] = np.log10(max(mel_val, 1e-10))
        for frame in range(active_frames, N_FRAMES):
            mel_spec[m, frame] = -10.0

    max_val = np.max(mel_spec)
    clip_floor = max_val - 8.0
    java_features = np.zeros((N_MELS, N_FRAMES), dtype=np.float32)
    for m in range(N_MELS):
        for frame in range(N_FRAMES):
            val = max(mel_spec[m, frame], clip_floor)
            java_features[m, frame] = (val + 4.0) / 4.0

    diff = np.abs(hf_features - java_features)
    print(f"S{num}: Max abs diff: {np.max(diff):.4f}, Mean abs diff: {np.mean(diff):.4f}")
