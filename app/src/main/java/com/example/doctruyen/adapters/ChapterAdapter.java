package com.example.doctruyen.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doctruyen.R;
import com.example.doctruyen.activities.ReadChapterActivity;
import com.example.doctruyen.models.Chapter;
import java.util.ArrayList;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    private Context context;
    private ArrayList<Chapter> chapterList;
    private String storyTitle;

    public ChapterAdapter(Context context, ArrayList<Chapter> chapterList, String storyTitle) {
        this.context = context;
        this.chapterList = chapterList;
        this.storyTitle = storyTitle;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);

        holder.tvChapterTitle.setText(chapter.getTitle());
        holder.tvChapterNumber.setText("Chương " + chapter.getChapterNumber());

        if (chapter.isRead()) {
            holder.itemView.setAlpha(0.6f);
            holder.tvChapterTitle.setTextColor(context.getResources().getColor(R.color.text_secondary));
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.tvChapterTitle.setTextColor(context.getResources().getColor(R.color.text_primary));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReadChapterActivity.class);
            intent.putExtra("chapter", chapter);
            intent.putExtra("storyTitle", storyTitle);
            intent.putExtra("chapterList", chapterList);
            intent.putExtra("currentPosition", position);
            context.startActivity(intent);
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