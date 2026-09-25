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

    public interface OnWorksheetClickListener {
        void onWorksheetClick(WorksheetItem item, int position);
    }

    private final List<WorksheetItem> items;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private OnWorksheetClickListener listener;

    public WorksheetAdapter(List<WorksheetItem> items) {
        this.items = items;
    }

    public WorksheetAdapter(List<WorksheetItem> items, OnWorksheetClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setOnWorksheetClickListener(OnWorksheetClickListener listener) {
        this.listener = listener;
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
        } else {
            holder.ivPreview.setVisibility(View.VISIBLE);
            holder.tvMathPreview.setVisibility(View.GONE);
            holder.ivPreview.setImageResource(R.drawable.ic_worksheet_quick);
        }

        if (item.isDownloaded) {
            holder.pbDownload.setVisibility(View.GONE);
            holder.btnDownload.setText("Practice / View Worksheet");
            holder.btnDownload.setEnabled(true);
            holder.btnDownload.setAlpha(1.0f);
        } else if (item.isDownloading) {
            holder.pbDownload.setVisibility(View.VISIBLE);
            holder.pbDownload.setProgress(item.downloadProgress);
            holder.btnDownload.setText("Preparing... " + item.downloadProgress + "%");
            holder.btnDownload.setEnabled(false);
        } else {
            holder.pbDownload.setVisibility(View.GONE);
            holder.btnDownload.setText("Practice / View Worksheet");
            holder.btnDownload.setEnabled(true);
            holder.btnDownload.setAlpha(1.0f);
        }

        View.OnClickListener clickAction = v -> {
            if (listener != null) {
                listener.onWorksheetClick(item, position);
            }
        };

        holder.itemView.setOnClickListener(clickAction);
        holder.btnDownload.setOnClickListener(clickAction);
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

        WorksheetViewHolder(@NonNull View itemView) {
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
