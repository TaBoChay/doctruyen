package com.example.doctruyen.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Thêm thư viện để xử lý màu sắc/drawable
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doctruyen.R;
import com.example.doctruyen.activities.StoryDetailActivity;
import com.example.doctruyen.models.Story;

import java.util.ArrayList;
import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {

    private final Context context;
    private List<Story> storyList;

    public RankingAdapter(Context context, List<Story> storyList) {
        this.context = context;
        this.storyList = new ArrayList<>(storyList);
    }

    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_story_rank.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_story_rank, parent, false);
        return new RankingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        if (storyList == null || storyList.isEmpty() || position >= storyList.size()) return;

        Story story = storyList.get(position);
        int rank = position + 1; // Thứ hạng thực tế (bắt đầu từ 1)

        // 1. XỬ LÝ SỐ THỨ TỰ VÀ MÀU SẮC (Logic màu nóng/lạnh)
        holder.tvRankNumber.setText(String.valueOf(rank));

        if (rank <= 3) {
            // Hạng 1, 2, 3: Màu nóng (HOT)
            holder.tvRankNumber.setBackgroundResource(R.drawable.bg_rank_hot);
            // Màu chữ trắng để nổi bật trên nền nóng (ví dụ: vàng, cam)
            holder.tvRankNumber.setTextColor(ContextCompat.getColor(context, R.color.text_white));
        } else {
            // Hạng 4 trở đi: Màu lạnh (COLD)
            holder.tvRankNumber.setBackgroundResource(R.drawable.bg_rank_cold);
            // Giữ chữ trắng (hoặc có thể dùng text_primary nếu nền lạnh là xám nhạt)
            holder.tvRankNumber.setTextColor(ContextCompat.getColor(context, R.color.text_white));
        }

        // 2. Gán dữ liệu truyện
        holder.tvTitle.setText(story.getTitle());
        holder.tvAuthor.setText(story.getAuthor());

        // 3. Gán Rating
        float rating = story.getRating();
        holder.ratingBar.setRating(rating);
        holder.tvRating.setText(String.format("%.1f", rating));

        String coverPath = story.getCoverImagePath();
        long coverLastModified = 0;
        if (coverPath != null && coverPath.startsWith("file:")) {
            coverLastModified = new java.io.File(coverPath.substring(5)).lastModified();
        }
        Uri coverUri = (coverPath != null && coverPath.startsWith("file:")) ? 
                Uri.parse(coverPath) : Uri.parse("file:///android_asset/" + coverPath);

        Glide.with(context)
                .load(coverUri)
                .signature(new com.bumptech.glide.signature.ObjectKey(String.valueOf(coverLastModified)))
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(holder.ivCover);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, StoryDetailActivity.class);
            intent.putExtra("storyId", story.getId());
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return storyList != null ? storyList.size() : 0;
    }

    public void updateList(List<Story> newList) {
        this.storyList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class RankingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRankNumber;
        ImageView ivCover;
        TextView tvTitle, tvAuthor, tvRating;
        RatingBar ratingBar;

        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các View từ item_story_rank.xml
            tvRankNumber = itemView.findViewById(R.id.tv_rank_number);
            ivCover = itemView.findViewById(R.id.iv_story_cover);
            tvTitle = itemView.findViewById(R.id.tv_story_title);
            tvAuthor = itemView.findViewById(R.id.tv_story_author);
            tvRating = itemView.findViewById(R.id.tv_rating);
            ratingBar = itemView.findViewById(R.id.rating_bar);
        }
    }
}