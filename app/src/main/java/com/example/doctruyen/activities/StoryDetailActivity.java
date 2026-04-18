package com.example.doctruyen.activities;

import android.content.Intent; // Cần cho chuyển hướng
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doctruyen.R;
import com.example.doctruyen.adapters.ChapterAdapter;
import com.example.doctruyen.adapters.CommentAdapter;
import com.example.doctruyen.adapters.ReviewAdapter;
import com.example.doctruyen.models.Story;
import com.example.doctruyen.utils.StoryManager;
import com.example.doctruyen.utils.SharedPrefsHelper; // <-- THÊM IMPORT NÀY

import com.example.doctruyen.models.Review;
import com.example.doctruyen.models.Comment;
import com.example.doctruyen.dialogs.AddCommentDialogFragment;
import com.example.doctruyen.dialogs.AddReviewDialogFragment;

import java.util.ArrayList;
import java.util.List;

// Giả định OnDataSubmittedListener đã được định nghĩa trong file riêng (hoặc ngay trên lớp này)

public class StoryDetailActivity extends AppCompatActivity implements OnDataSubmittedListener {

    private ImageView ivBanner, ivCover, ivBack;
    private TextView tvTitle, tvAuthor, tvCategory, tvRating, tvDescription;
    private RatingBar ratingBar;
    private Button btnGioiThieu, btnDanhGia, btnBinhLuan, btnDanhSachChuong, btnAddToLibrary, btnAddReview, btnAddComment;
    private android.widget.ImageButton btnSaveSmall;
    private RecyclerView recyclerContent;
    private Story story;

    private SharedPrefsHelper prefsHelper; // <-- KHAI BÁO BIẾN HELPER

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_detail);

        prefsHelper = SharedPrefsHelper.getInstance(this); // <-- KHỞI TẠO HELPER

        String storyId = getIntent().getStringExtra("storyId");
        story = StoryManager.getInstance(this).getStoryById(storyId);

        if (story == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy truyện.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadStoryData();
        setupButtons();
        showGioiThieu();
    }

    private void initViews() {
        ivBanner = findViewById(R.id.iv_banner);
        ivCover = findViewById(R.id.iv_cover);
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);
        tvAuthor = findViewById(R.id.tv_author);
        tvCategory = findViewById(R.id.tv_category);
        tvRating = findViewById(R.id.tv_rating);
        tvDescription = findViewById(R.id.tv_description);
        ratingBar = findViewById(R.id.rating_bar);
        btnGioiThieu = findViewById(R.id.btn_gioi_thieu);
        btnDanhGia = findViewById(R.id.btn_danh_gia);
        btnBinhLuan = findViewById(R.id.btn_binh_luan);
        btnDanhSachChuong = findViewById(R.id.btn_danh_sach_chuong);
        btnAddToLibrary = findViewById(R.id.btn_add_to_library);
        btnSaveSmall = findViewById(R.id.btn_save_small);
        recyclerContent = findViewById(R.id.recycler_content);

        btnAddReview = findViewById(R.id.btn_add_review);
        btnAddComment = findViewById(R.id.btn_add_comment);

        recyclerContent.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadStoryData() {
        tvTitle.setText(story.getTitle());
        tvAuthor.setText(story.getAuthor());
        tvCategory.setText(story.getCategory());
        tvRating.setText(String.format("%.1f", story.getRating()));
        tvDescription.setText(story.getDescription());
        ratingBar.setRating(story.getRating());

        Glide.with(this)
                .load(Uri.parse("file:///android_asset/" + story.getBannerImagePath()))
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(ivBanner);

        Glide.with(this)
                .load(Uri.parse("file:///android_asset/" + story.getCoverImagePath()))
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(ivCover);

        // Set initial state of save button based on whether story is in library
        if (story.isInLibrary()) {
            btnSaveSmall.setColorFilter(getResources().getColor(R.color.yellow));
        } else {
            btnSaveSmall.setColorFilter(getResources().getColor(android.R.color.white));
        }

        ivBack.setOnClickListener(v -> finish());
    }

    // --- PHƯƠNG THỨC KIỂM TRA ĐĂNG NHẬP (login gating) ---
    private void checkLoginAndPerformAction(Runnable action) {
        if (prefsHelper.isLoggedIn()) {
            action.run(); // Đã đăng nhập, thực hiện hành động
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, DangNhapActivity.class);
            startActivity(intent);
        }
    }
    // ----------------------------------------------------

    private void setupButtons() {
        btnGioiThieu.setOnClickListener(v -> showGioiThieu());
        btnDanhGia.setOnClickListener(v -> showDanhGia());
        btnBinhLuan.setOnClickListener(v -> showBinhLuan());
        btnDanhSachChuong.setOnClickListener(v -> showDanhSachChuong());

        // Main button now starts reading the first chapter
        btnAddToLibrary.setOnClickListener(v -> {
            if (story.getChapters() != null && !story.getChapters().isEmpty()) {
                Intent intent = new Intent(this, ReadChapterActivity.class);
                intent.putExtra("chapter", story.getChapters().get(0));
                intent.putExtra("chapterList", story.getChapters());
                intent.putExtra("currentPosition", 0);
                intent.putExtra("storyTitle", story.getTitle());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Truyện chưa có chương nào để đọc.", Toast.LENGTH_SHORT).show();
            }
        });

        // Small save button toggles library status
        btnSaveSmall.setOnClickListener(v -> checkLoginAndPerformAction(() -> {
            StoryManager storyManager = StoryManager.getInstance(this);
            if (story.isInLibrary()) {
                // Remove from library
                storyManager.removeStoriesFromLibrary(java.util.Collections.singletonList(story));
                story.setInLibrary(false);
                // Change icon color back to white
                btnSaveSmall.setColorFilter(getResources().getColor(android.R.color.white));
                Toast.makeText(this, "Đã xóa " + story.getTitle() + " khỏi tủ truyện!", Toast.LENGTH_SHORT).show();
            } else {
                // Add to library
                storyManager.addToLibrary(story);
                story.setInLibrary(true);
                // Change icon color to yellow
                btnSaveSmall.setColorFilter(getResources().getColor(R.color.yellow));
                Toast.makeText(this, "Đã thêm " + story.getTitle() + " vào tủ truyện!", Toast.LENGTH_SHORT).show();
            }
        }));

        // 2. ÁP DỤNG LOGIN GATING cho Thêm Đánh giá
        if (btnAddReview != null) {
            btnAddReview.setOnClickListener(v -> checkLoginAndPerformAction(() -> showAddReviewDialog()));
        }

        // 3. ÁP DỤNG LOGIN GATING cho Thêm Bình luận
        if (btnAddComment != null) {
            btnAddComment.setOnClickListener(v -> checkLoginAndPerformAction(() -> showAddCommentDialog()));
        }
    }

    private void resetButtonStates(Button activeBtn) {
        int inactiveColor = getResources().getColor(R.color.button_inactive);
        int activeColor = getResources().getColor(R.color.button_active);

        btnGioiThieu.setBackgroundColor(inactiveColor);
        btnDanhGia.setBackgroundColor(inactiveColor);
        btnBinhLuan.setBackgroundColor(inactiveColor);
        btnDanhSachChuong.setBackgroundColor(inactiveColor);

        if (activeBtn != null) {
            activeBtn.setBackgroundColor(activeColor);
        }

        if (btnAddReview != null) btnAddReview.setVisibility(View.GONE);
        if (btnAddComment != null) btnAddComment.setVisibility(View.GONE);
    }

    private void showGioiThieu() {
        resetButtonStates(btnGioiThieu);
        recyclerContent.setVisibility(View.GONE);
        tvDescription.setVisibility(View.VISIBLE);
        tvDescription.setText(story.getDescription());
    }

    private void showDanhGia() {
        resetButtonStates(btnDanhGia);
        tvDescription.setVisibility(View.GONE);
        recyclerContent.setVisibility(View.VISIBLE);

        List<Review> reviews = story.getReviews();
        if (reviews == null) reviews = new ArrayList<>();

        ReviewAdapter adapter = new ReviewAdapter(this, (ArrayList<Review>) reviews);
        recyclerContent.setAdapter(adapter);

        if (reviews.isEmpty()) {
            Toast.makeText(this, "Chưa có đánh giá nào cho truyện này.", Toast.LENGTH_SHORT).show();
        }

        if (btnAddReview != null) btnAddReview.setVisibility(View.VISIBLE);
    }

    private void showBinhLuan() {
        resetButtonStates(btnBinhLuan);
        tvDescription.setVisibility(View.GONE);
        recyclerContent.setVisibility(View.VISIBLE);

        List<Comment> comments = story.getComments();
        if (comments == null) comments = new ArrayList<>();

        CommentAdapter adapter = new CommentAdapter(this, (ArrayList<Comment>) comments);
        recyclerContent.setAdapter(adapter);

        if (comments.isEmpty()) {
            Toast.makeText(this, "Chưa có bình luận nào cho truyện này.", Toast.LENGTH_SHORT).show();
        }

        if (btnAddComment != null) btnAddComment.setVisibility(View.VISIBLE);
    }

    private void showDanhSachChuong() {
        resetButtonStates(btnDanhSachChuong);
        tvDescription.setVisibility(View.GONE);
        recyclerContent.setVisibility(View.VISIBLE);

        ChapterAdapter adapter = new ChapterAdapter(this, story.getChapters(), story.getTitle());
        recyclerContent.setAdapter(adapter);
    }

    private void showAddReviewDialog() {
        AddReviewDialogFragment dialog = AddReviewDialogFragment.newInstance(story.getId());
        dialog.show(getSupportFragmentManager(), "AddReview");
    }

    private void showAddCommentDialog() {
        AddCommentDialogFragment dialog = AddCommentDialogFragment.newInstance(story.getId());
        dialog.show(getSupportFragmentManager(), "AddComment");
    }

    @Override
    public void onReviewSubmitted() {
        story = StoryManager.getInstance(this).getStoryById(story.getId());
        if (story != null) {
            tvRating.setText(String.format("%.1f", story.getRating()));
            ratingBar.setRating(story.getRating());
        }
        showDanhGia();
    }

    @Override
    public void onCommentSubmitted() {
        story = StoryManager.getInstance(this).getStoryById(story.getId());
        showBinhLuan();
    }
}