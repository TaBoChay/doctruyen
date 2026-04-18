package com.example.doctruyen.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doctruyen.R;
import com.example.doctruyen.models.Chapter;
import java.util.ArrayList;

public class ChapterListAdapter extends RecyclerView.Adapter<ChapterListAdapter.ChapterViewHolder> {

    private ArrayList<Chapter> chapterList;
    private int currentPosition;
    private int startIndex;
    private OnChapterClickListener listener;

    public interface OnChapterClickListener {
        void onChapterClick(int position, Chapter chapter);
    }

    public ChapterListAdapter(ArrayList<Chapter> chapterList, int currentPosition, int startIndex, OnChapterClickListener listener) {
        this.chapterList = chapterList;
        this.currentPosition = currentPosition;
        this.startIndex = startIndex;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter_popup, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);
        int actualPosition = startIndex + position;

        holder.tvChapterTitle.setText(chapter.getTitle());
        holder.tvChapterNumber.setText("Chương " + chapter.getChapterNumber());

        if (actualPosition == currentPosition) {
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.button_accent));
            holder.tvChapterTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
            holder.tvChapterNumber.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
        } else {
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.background));
            if (chapter.isRead()) {
                holder.itemView.setAlpha(0.6f);
                holder.tvChapterTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
            } else {
                holder.itemView.setAlpha(1.0f);
                holder.tvChapterTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
            }
            holder.tvChapterNumber.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
        }

        final int finalPosition = actualPosition;
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChapterClick(finalPosition, chapter);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chapterList.size();
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvChapterNumber, tvChapterTitle;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterNumber = itemView.findViewById(R.id.tv_chapter_number);
            tvChapterTitle = itemView.findViewById(R.id.tv_chapter_title);
        }
    }
}