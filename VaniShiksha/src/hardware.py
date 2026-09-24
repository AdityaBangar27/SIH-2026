import os
import sys
import torch
from typing import Dict, Any

def get_system_device() -> str:
    """Detects available computing device (CUDA or CPU)."""
    return "cuda" if torch.cuda.is_available() else "cpu"

def get_hardware_info() -> Dict[str, Any]:
    """Gathers comprehensive hardware and environment information."""
    python_ver = f"{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}"
    torch_ver = torch.__version__
    cuda_available = torch.cuda.is_available()
    cuda_ver = torch.version.cuda if cuda_available else None
    device_name = torch.cuda.get_device_name(0) if cuda_available else "CPU (AMD64 / x86_64)"
    vram_gb = round(torch.cuda.get_device_properties(0).total_memory / (1024**3), 2) if cuda_available else None
    cpu_count = os.cpu_count() or 4
    
    # Try importing flash_attn
    flash_attn_available = False
    try:
        import flash_attn
        flash_attn_available = True
    except (ImportError, Exception):
        flash_attn_available = False

    return {
        "python_version": python_ver,
        "torch_version": torch_ver,
        "cuda_available": cuda_available,
        "cuda_version": cuda_ver,
        "device": "CUDA" if cuda_available else "CPU",
        "device_name": device_name,
        "vram_gb": vram_gb,
        "cpu_count": cpu_count,
        "flash_attn_available": flash_attn_available,
    }

def print_system_startup():
    """Prints diagnostic system startup information to stdout."""
    info = get_hardware_info()
    print(f"[SYSTEM] Python Version: {info['python_version']}")
    print(f"[SYSTEM] PyTorch Version: {info['torch_version']}")
    vram_str = f" | VRAM: {info['vram_gb']:.2f} GB" if info['vram_gb'] else ""
    print(f"[SYSTEM] Device: {info['device']} ({info['device_name']}{vram_str})")
    
    if info["flash_attn_available"]:
        print("[SYSTEM] Flash Attention 2: Available and active.")
    else:
        print("[SYSTEM] Flash Attention 2 unavailable — using standard attention.")

    # Configure optimal CPU threads if running on CPU
    if not info["cuda_available"]:
        threads = max(1, min(8, info["cpu_count"]))
        torch.set_num_threads(threads)
        print(f"[SYSTEM] PyTorch CPU threads configured to {threads}.")
