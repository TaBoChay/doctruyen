package com.example.doctruyen.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doctruyen.R;
import com.example.doctruyen.models.Chapter;

import java.util.ArrayList;

public class ChapterManageAdapter extends RecyclerView.Adapter<ChapterManageAdapter.ChapterViewHolder> {

    private ArrayList<Chapter> chapterList;
    private OnChapterEditListener listener;

    public interface OnChapterEditListener {
        void onEditChapter(int position, Chapter chapter);
    }

    public ChapterManageAdapter(ArrayList<Chapter> chapterList, OnChapterEditListener listener) {
        this.chapterList = chapterList;
        this.listener = listener;
    }

    public void updateList(ArrayList<Chapter> newList) {
        this.chapterList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter_manage, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);

        holder.tvChapterNumber.setText("Chương " + chapter.getChapterNumber());
        holder.tvChapterTitle.setText(chapter.getTitle());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditChapter(position, chapter);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chapterList != null ? chapterList.size() : 0;
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvChapterNumber, tvChapterTitle;
        ImageView btnEdit;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterNumber = itemView.findViewById(R.id.tv_chapter_number);
            tvChapterTitle = itemView.findViewById(R.id.tv_chapter_title);
            btnEdit = itemView.findViewById(R.id.btn_edit);
        }
    }
}
