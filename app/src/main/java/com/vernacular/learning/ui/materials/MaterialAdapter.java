package com.vernacular.learning.ui.materials;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.MaterialItem;
import java.util.List;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder> {
    private final List<MaterialItem> items;
    private final OnMaterialClickListener listener;

    public interface OnMaterialClickListener {
        void onMaterialClick(MaterialItem item);
    }

    public MaterialAdapter(List<MaterialItem> items, OnMaterialClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material_card, parent, false);
        return new MaterialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        MaterialItem item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvSubtitle.setText(item.subtitle);
        holder.ivIcon.setImageResource(item.iconResId);

        if (item.isOfflineAvailable) {
            holder.tvBadge.setText(R.string.offline_badge);
            holder.tvBadge.setAlpha(1.0f);
        } else {
            holder.tvBadge.setText(R.string.need_download_badge);
            holder.tvBadge.setAlpha(0.7f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMaterialClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MaterialViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvBadge;

        public MaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivMaterialIcon);
            tvTitle = itemView.findViewById(R.id.tvMaterialTitle);
            tvSubtitle = itemView.findViewById(R.id.tvMaterialSubtitle);
            tvBadge = itemView.findViewById(R.id.tvMaterialStatusBadge);
        }
    }
}
