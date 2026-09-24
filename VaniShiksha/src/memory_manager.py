import os
import sys
import gc
import psutil
from typing import Dict, Any, Optional
import torch

class ModelMemoryManager:
    """
    Centralized Memory & Lifecycle Manager for VaaniShiksha AI.
    Enforces a <=1.8 GB memory budget for low-end (2 GB RAM) offline Android devices.
    """

    _instance: Optional["ModelMemoryManager"] = None
    RAM_LIMIT_MB = 1800.0  # 1.8 GB Safety Ceiling for 2 GB Android devices

    def __init__(self):
        self.process = psutil.Process(os.getpid())
        self.peak_rss_mb = 0.0
        self.initial_rss_mb = self.get_process_rss_mb()
        self.peak_rss_mb = self.initial_rss_mb

    @classmethod
    def get_instance(cls) -> "ModelMemoryManager":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def get_process_rss_mb(self) -> float:
        """Returns the current process Resident Set Size (RSS) in MB."""
        rss_bytes = self.process.memory_info().rss
        rss_mb = rss_bytes / (1024.0 * 1024.0)
        if rss_mb > self.peak_rss_mb:
            self.peak_rss_mb = rss_mb
        return round(rss_mb, 2)

    def get_system_ram_info(self) -> Dict[str, float]:
        """Returns total, available, and used system RAM in GB and MB."""
        vm = psutil.virtual_memory()
        return {
            "total_gb": round(vm.total / (1024.0**3), 2),
            "available_gb": round(vm.available / (1024.0**3), 2),
            "used_gb": round(vm.used / (1024.0**3), 2),
            "percent_used": vm.percent,
            "process_rss_mb": self.get_process_rss_mb(),
            "peak_rss_mb": round(self.peak_rss_mb, 2),
        }

    def cleanup_memory(self, force_cuda: bool = True):
        """Forces Python garbage collection and frees unreferenced PyTorch / CUDA memory."""
        gc.collect()
        if force_cuda and torch.cuda.is_available():
            torch.cuda.empty_cache()

    def check_memory_budget(self) -> Dict[str, Any]:
        """
        Checks whether the process memory is within the 1.8 GB Android safety budget.
        Calculates both raw OS process RSS and estimated mobile device working footprint.
        """
        current_rss = self.get_process_rss_mb()
        model_working_set = max(0.0, current_rss - self.initial_rss_mb)
        
        # On Android OS, base app runtime is ~180 MB + loaded models & working buffers
        estimated_android_footprint_mb = 180.0 + model_working_set
        within_budget = estimated_android_footprint_mb <= self.RAM_LIMIT_MB

        return {
            "current_rss_mb": current_rss,
            "peak_rss_mb": self.peak_rss_mb,
            "model_working_set_mb": round(model_working_set, 2),
            "estimated_android_ram_mb": round(estimated_android_footprint_mb, 2),
            "budget_limit_mb": self.RAM_LIMIT_MB,
            "within_budget": within_budget,
            "headroom_mb": round(max(0.0, self.RAM_LIMIT_MB - estimated_android_footprint_mb), 2),
            "status": "HEALTHY" if within_budget else "WARNING_OVER_BUDGET",
        }
