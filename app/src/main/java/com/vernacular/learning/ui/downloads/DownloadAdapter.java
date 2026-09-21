package com.vernacular.learning.ui.downloads;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.vernacular.learning.R;
import com.vernacular.learning.data.local.entities.DownloadItemEntity;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {
    private final List<DownloadItemEntity> items;
    private final OnDownloadListChangedListener changeListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface OnDownloadListChangedListener {
        void onDownloadListChanged(List<DownloadItemEntity> items);
    }

    public DownloadAdapter(List<DownloadItemEntity> items, OnDownloadListChangedListener changeListener) {
        this.items = items;
        this.changeListener = changeListener;
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_row, parent, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItemEntity item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvSize.setText(item.sizeDescription);

        if ("DOWNLOADED".equals(item.downloadStatus)) {
            holder.pbDownload.setVisibility(View.GONE);
            holder.btnAction.setText(R.string.btn_remove);
            holder.btnAction.setAlpha(0.65f);
        } else if ("DOWNLOADING".equals(item.downloadStatus)) {
            holder.pbDownload.setVisibility(View.VISIBLE);
            holder.pbDownload.setProgress(item.progressPercent);
            holder.btnAction.setText(R.string.btn_cancel);
            holder.btnAction.setAlpha(1.0f);
        } else {
            holder.pbDownload.setVisibility(View.GONE);
            holder.btnAction.setText(R.string.btn_download);
            holder.btnAction.setAlpha(1.0f);
        }

        holder.btnAction.setOnClickListener(v -> {
            if ("NOT_DOWNLOADED".equals(item.downloadStatus) || "ERROR".equals(item.downloadStatus)) {
                startSimulatedDownload(item, position);
            } else if ("DOWNLOADING".equals(item.downloadStatus)) {
                // Cancel
                item.downloadStatus = "NOT_DOWNLOADED";
                item.progressPercent = 0;
                notifyItemChanged(position);
                if (changeListener != null) changeListener.onDownloadListChanged(items);
            } else if ("DOWNLOADED".equals(item.downloadStatus)) {
                // Remove
                item.downloadStatus = "NOT_DOWNLOADED";
                item.progressPercent = 0;
                notifyItemChanged(position);
                if (changeListener != null) changeListener.onDownloadListChanged(items);
            }
        });
    }

    private void startSimulatedDownload(DownloadItemEntity item, int position) {
        item.downloadStatus = "DOWNLOADING";
        item.progressPercent = 0;
        notifyItemChanged(position);

        Runnable progressTask = new Runnable() {
            @Override
            public void run() {
                if (!"DOWNLOADING".equals(item.downloadStatus)) return;

                item.progressPercent += 20;
                if (item.progressPercent >= 100) {
                    item.downloadStatus = "DOWNLOADED";
                    item.progressPercent = 100;
                    notifyItemChanged(position);
                    if (changeListener != null) changeListener.onDownloadListChanged(items);
                } else {
                    notifyItemChanged(position);
                    handler.postDelayed(this, 350);
                }
            }
        };
        handler.postDelayed(progressTask, 350);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DownloadViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSize;
        ProgressBar pbDownload;
        MaterialButton btnAction;

        public DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivDownloadTypeIcon);
            tvTitle = itemView.findViewById(R.id.tvDownloadTitle);
            tvSize = itemView.findViewById(R.id.tvDownloadSize);
            pbDownload = itemView.findViewById(R.id.pbDownloadItem);
            btnAction = itemView.findViewById(R.id.btnDownloadAction);
        }
    }
}
