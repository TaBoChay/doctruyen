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
import android.util.SparseBooleanArray;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doctruyen.R;
import com.example.doctruyen.activities.StoryDetailActivity;
import com.example.doctruyen.models.Story;

import java.util.ArrayList;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private final Context context;
    private List<Story> storyList;

    // --- LOGIC ĐA CHỌN MỚI ---
    private final SparseBooleanArray selectedItems = new SparseBooleanArray();
    private boolean isMultiSelectMode = false;
    private OnItemLongClickListener longClickListener;
    private OnItemClickListener itemClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    public Story getStoryAt(int position) {
        if (storyList != null && position >= 0 && position < storyList.size()) {
            return storyList.get(position);
        }
        return null;
    }
    // -------------------------

    public StoryAdapter(Context context, List<Story> storyList) {
        this.context = context;
        this.storyList = new ArrayList<>(storyList);
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        if (storyList == null || storyList.isEmpty() || position >= storyList.size()) return;

        Story story = storyList.get(position);

        // 1. Gán dữ liệu
        holder.tvTitle.setText(story.getTitle());

        String author = story.getAuthor();
        holder.tvAuthor.setText(author != null && !author.isEmpty() ? author : "Tác giả ẩn danh");

        String category = story.getCategory();
        holder.tvCategory.setText(category != null && !category.isEmpty() ? category : "Chưa phân loại");

        float rating = story.getRating();
        holder.ratingBar.setRating(rating);
        holder.tvRating.setText(String.format("%.1f", rating));
        
        // Set views and reviews
        holder.tvViews.setText(String.valueOf(story.getViews()));
        holder.tvReviews.setText(String.valueOf(story.getTotalReviews()));

        // 2. Glide load ảnh từ assets (SỬ DỤNG ĐƯỜNG DẪN ĐÃ SỬA: BỎ Data/ lặp lại)
        String relativePath = story.getCoverImagePath();
        String fullAssetPath = "file:///android_asset/" + relativePath;

        Log.d("STORY_ADAPTER_GLIDE", "Loading: " + fullAssetPath);

        Glide.with(context)
                .load(Uri.parse(fullAssetPath))
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(holder.ivCover);

        // 3. Xử lý trạng thái và sự kiện đa chọn
        boolean isSelected = selectedItems.get(position, false);
        if (isMultiSelectMode && isSelected) {
            // Đặt màu nền khi ở chế độ chọn và mục được chọn
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.accent_light));
        } else {
            // Đặt màu nền mặc định (Giả sử màu CardView là màu trong suốt hoặc màu nền)
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }

        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                toggleSelection(position);
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(position);
                }
            } else {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(position);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(position);
            }
            return true; // Tiêu thụ sự kiện long click để kích hoạt chế độ chọn
        });
    }

    @Override
    public int getItemCount() {
        return storyList != null ? storyList.size() : 0;
    }

    public void updateList(List<Story> newList) {
        this.storyList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
        // Xóa tất cả trạng thái chọn khi danh sách được cập nhật
        selectedItems.clear();
        notifyDataSetChanged();
    }

    // --- Các hàm hỗ trợ chế độ đa chọn ---

    public void setMultiSelectMode(boolean isEnabled) {
        this.isMultiSelectMode = isEnabled;
        if (!isEnabled) {
            selectedItems.clear();
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(int position) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    public List<Story> getSelectedStories() {
        List<Story> selectedStories = new ArrayList<>();
        for (int i = 0; i < storyList.size(); i++) {
            if (selectedItems.get(i)) {
                selectedStories.add(storyList.get(i));
            }
        }
        return selectedStories;
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    // -------------------------------------

    static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvAuthor, tvCategory, tvRating, tvViews, tvReviews;
        RatingBar ratingBar;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_story_cover);
            tvTitle = itemView.findViewById(R.id.tv_story_title);
            tvAuthor = itemView.findViewById(R.id.tv_story_author);
            tvCategory = itemView.findViewById(R.id.tv_story_category);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvViews = itemView.findViewById(R.id.tv_views);
            tvReviews = itemView.findViewById(R.id.tv_reviews);
            ratingBar = itemView.findViewById(R.id.rating_bar);
        }
    }
}