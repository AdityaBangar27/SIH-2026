package com.vernacular.learning.ui.worksheets;

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
import com.vernacular.learning.data.models.WorksheetItem;
import java.util.List;

public class WorksheetAdapter extends RecyclerView.Adapter<WorksheetAdapter.WorksheetViewHolder> {
    private final List<WorksheetItem> items;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public WorksheetAdapter(List<WorksheetItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public WorksheetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worksheet_card, parent, false);
        return new WorksheetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorksheetViewHolder holder, int position) {
        WorksheetItem item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvSubtitle.setText(item.subtitle);

        if (item.imagePreviewResId != 0) {
            holder.ivPreview.setVisibility(View.VISIBLE);
            holder.tvMathPreview.setVisibility(View.GONE);
            holder.ivPreview.setImageResource(item.imagePreviewResId);
        } else if (item.textPreview != null) {
            holder.ivPreview.setVisibility(View.GONE);
            holder.tvMathPreview.setVisibility(View.VISIBLE);
            holder.tvMathPreview.setText(item.textPreview);
        }

        if (item.isDownloaded) {
            holder.pbDownload.setVisibility(View.GONE);
            holder.btnDownload.setText(R.string.downloaded_state);
            holder.btnDownload.setEnabled(false);
            holder.btnDownload.setAlpha(0.85f);
        } else if (item.isDownloading) {
            holder.pbDownload.setVisibility(View.VISIBLE);
            holder.pbDownload.setProgress(item.downloadProgress);
            holder.btnDownload.setText("Downloading... " + item.downloadProgress + "%");
            holder.btnDownload.setEnabled(false);
        } else {
            holder.pbDownload.setVisibility(View.GONE);
            holder.btnDownload.setText(R.string.btn_download_pdf);
            holder.btnDownload.setEnabled(true);
            holder.btnDownload.setAlpha(1.0f);
        }

        holder.btnDownload.setOnClickListener(v -> {
            if (!item.isDownloaded && !item.isDownloading) {
                simulateDownload(item, position);
            }
        });
    }

    private void simulateDownload(WorksheetItem item, int position) {
        item.isDownloading = true;
        item.downloadProgress = 0;
        notifyItemChanged(position);

        Runnable progressStep = new Runnable() {
            @Override
            public void run() {
                item.downloadProgress += 25;
                if (item.downloadProgress >= 100) {
                    item.isDownloading = false;
                    item.isDownloaded = true;
                    notifyItemChanged(position);
                } else {
                    notifyItemChanged(position);
                    handler.postDelayed(this, 300);
                }
            }
        };
        handler.postDelayed(progressStep, 300);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class WorksheetViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;
        ImageView ivPreview;
        TextView tvMathPreview;
        ProgressBar pbDownload;
        MaterialButton btnDownload;

        public WorksheetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvWorksheetTitle);
            tvSubtitle = itemView.findViewById(R.id.tvWorksheetSubtitle);
            ivPreview = itemView.findViewById(R.id.ivWorksheetPreview);
            tvMathPreview = itemView.findViewById(R.id.tvWorksheetMathPreview);
            pbDownload = itemView.findViewById(R.id.pbWorksheetDownload);
            btnDownload = itemView.findViewById(R.id.btnDownloadPdf);
        }
    }
}
