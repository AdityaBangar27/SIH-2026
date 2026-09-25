package com.vernacular.learning.ui.curriculum;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.study.StudyModels.CurriculumChapter;

import java.util.List;

public class CurriculumAdapter extends RecyclerView.Adapter<CurriculumAdapter.ChapterViewHolder> {

    public interface OnChapterClickListener {
        void onChapterClick(CurriculumChapter chapter);
    }

    private final List<CurriculumChapter> chapters;
    private final OnChapterClickListener listener;

    public CurriculumAdapter(List<CurriculumChapter> chapters, OnChapterClickListener listener) {
        this.chapters = chapters;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_curriculum_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        CurriculumChapter chapter = chapters.get(position);
        holder.tvChapterNumberBadge.setText("अध्याय " + chapter.chapterNumber);
        holder.tvChapterTitle.setText(chapter.title);
        holder.tvChapterObjective.setText(chapter.learningObjective != null ? chapter.learningObjective : "");

        int partsCount = chapter.studyMaterial != null ? chapter.studyMaterial.size() : 0;
        int questionsCount = chapter.questions != null ? chapter.questions.size() : 0;
        int flashcardsCount = chapter.flashcards != null ? chapter.flashcards.size() : 0;

        holder.tvConceptsCount.setText("🌱 " + partsCount + " भाग");
        holder.tvQuestionsCount.setText("📝 " + questionsCount + " प्रश्न");
        holder.tvFlashcardsCount.setText("🎴 " + flashcardsCount + " कार्ड");

        holder.cardChapterItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChapterClick(chapter);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardChapterItem;
        TextView tvChapterNumberBadge;
        TextView tvChapterTitle;
        TextView tvChapterObjective;
        TextView tvConceptsCount;
        TextView tvQuestionsCount;
        TextView tvFlashcardsCount;

        ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            cardChapterItem = itemView.findViewById(R.id.cardChapterItem);
            tvChapterNumberBadge = itemView.findViewById(R.id.tvChapterNumberBadge);
            tvChapterTitle = itemView.findViewById(R.id.tvChapterTitle);
            tvChapterObjective = itemView.findViewById(R.id.tvChapterObjective);
            tvConceptsCount = itemView.findViewById(R.id.tvConceptsCount);
            tvQuestionsCount = itemView.findViewById(R.id.tvQuestionsCount);
            tvFlashcardsCount = itemView.findViewById(R.id.tvFlashcardsCount);
        }
    }
}
