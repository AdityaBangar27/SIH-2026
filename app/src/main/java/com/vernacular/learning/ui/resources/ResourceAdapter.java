package com.vernacular.learning.ui.resources;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.study.StudyModels.UserResourceItem;

import java.util.List;

public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {

    public interface OnResourceActionListener {
        void onOpen(UserResourceItem item);
        void onDelete(UserResourceItem item);
    }

    private final List<UserResourceItem> resources;
    private final OnResourceActionListener listener;

    public ResourceAdapter(List<UserResourceItem> resources, OnResourceActionListener listener) {
        this.resources = resources;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resource_card, parent, false);
        return new ResourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        UserResourceItem item = resources.get(position);

        holder.tvResourceName.setText(item.name);
        holder.tvResourceDetails.setText(item.className + " • " + item.subject +
                (item.topic != null && !item.topic.isEmpty() ? " • " + item.topic : ""));
        holder.tvResourceLanguage.setText("Language: " + (item.language != null ? item.language : "Hindi & Santhali"));

        if (item.name.toLowerCase().endsWith(".pdf")) {
            holder.ivResourceTypeIcon.setImageResource(R.drawable.ic_nav_pencil);
        } else if (item.name.toLowerCase().endsWith(".jpg") || item.name.toLowerCase().endsWith(".png")) {
            holder.ivResourceTypeIcon.setImageResource(R.drawable.ic_apple_single);
        } else {
            holder.ivResourceTypeIcon.setImageResource(R.drawable.ic_toolkit_books);
        }

        holder.btnOpenResource.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(item);
        });

        holder.btnDeleteResource.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });

        holder.cardResourceRoot.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(item);
        });
    }

    @Override
    public int getItemCount() {
        return resources.size();
    }

    static class ResourceViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardResourceRoot;
        ImageView ivResourceTypeIcon;
        TextView tvResourceName;
        TextView tvResourceDetails;
        TextView tvResourceLanguage;
        MaterialButton btnOpenResource;
        ImageView btnDeleteResource;

        ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardResourceRoot = itemView.findViewById(R.id.cardResourceRoot);
            ivResourceTypeIcon = itemView.findViewById(R.id.ivResourceTypeIcon);
            tvResourceName = itemView.findViewById(R.id.tvResourceName);
            tvResourceDetails = itemView.findViewById(R.id.tvResourceDetails);
            tvResourceLanguage = itemView.findViewById(R.id.tvResourceLanguage);
            btnOpenResource = itemView.findViewById(R.id.btnOpenResource);
            btnDeleteResource = itemView.findViewById(R.id.btnDeleteResource);
        }
    }
}
