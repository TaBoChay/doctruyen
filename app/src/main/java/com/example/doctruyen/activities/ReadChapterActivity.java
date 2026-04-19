package com.example.doctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doctruyen.R;
import com.example.doctruyen.activities.StoryDetailActivity;
import com.example.doctruyen.adapters.ChapterListAdapter;
import com.example.doctruyen.models.Chapter;
import com.example.doctruyen.utils.StoryManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.EditText;

import com.example.doctruyen.models.Comment;
import com.example.doctruyen.utils.SharedPrefsHelper;
import com.example.doctruyen.adapters.CommentAdapter;

public class ReadChapterActivity extends AppCompatActivity {

    private TextView tvTitle, tvChapterTitle, tvContent, tvChapterSelector;
    private ImageButton btnPrev, btnNext, btnReload, btnComments;
    private ImageView ivBack, ivMenu;
    private View chapterSelectorView;
    private ScrollView scrollView;
    private Chapter currentChapter;
    private ArrayList<Chapter> chapterList;
    private int currentPosition;
    private String storyTitle;
    private String storyId;
    private PopupWindow chapterPopupWindow;
    
    private Button btnAddChapterComment;
    private RecyclerView rvChapterComments;
    private ArrayList<Comment> chapterComments;
    private CommentAdapter commentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_chapter);

        currentChapter = (Chapter) getIntent().getSerializableExtra("chapter");
        chapterList = (ArrayList<Chapter>) getIntent().getSerializableExtra("chapterList");
        currentPosition = getIntent().getIntExtra("currentPosition", 0);
        storyTitle = getIntent().getStringExtra("storyTitle");
        storyId = getIntent().getStringExtra("storyId");

        initViews();
        loadChapter();
        setupButtons();
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvChapterTitle = findViewById(R.id.tv_chapter_title);
        tvContent = findViewById(R.id.tv_content);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnReload = findViewById(R.id.btn_reload);
        btnComments = findViewById(R.id.btn_comments);
        ivBack = findViewById(R.id.iv_back);
        ivMenu = findViewById(R.id.iv_menu);
        scrollView = findViewById(R.id.scroll_view);
        
        btnAddChapterComment = findViewById(R.id.btn_add_chapter_comment);
        rvChapterComments = findViewById(R.id.rv_chapter_comments);
        
        chapterComments = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, chapterComments);
        rvChapterComments.setLayoutManager(new LinearLayoutManager(this));
        rvChapterComments.setAdapter(commentAdapter);
    }

    private void loadChapter() {
        tvTitle.setText(storyTitle);
        tvChapterTitle.setText(currentChapter.getTitle());
        
        String content = StoryManager.getInstance(this).readChapterContent(currentChapter.getFilePath());
        tvContent.setText(content);

        currentChapter.setRead(true);
        StoryManager.getInstance(this).markChapterRead(
                currentChapter.getId(),
                currentChapter.getStoryId()
        );

        StoryManager.getInstance(this).getChapterComments(currentChapter.getId(), new StoryManager.CommentCallback() {
            @Override
            public void onCommentsLoaded(List<Comment> comments) {
                chapterComments.clear();
                chapterComments.addAll(comments);
                commentAdapter.notifyDataSetChanged();
            }
        });

        scrollView.post(() -> {
            scrollView.scrollTo(0, 0);
            if (tvContent != null) tvContent.setScrollY(0);
        });
    }


    private void setupButtons() {
        ivBack.setOnClickListener(v -> finish());
        
        ivMenu.setOnClickListener(v -> showChapterPopup());
        
        btnPrev.setEnabled(currentPosition > 0);
        btnNext.setEnabled(currentPosition < chapterList.size() - 1);

        btnPrev.setAlpha(currentPosition > 0 ? 1f : 0.3f);
        btnNext.setAlpha(currentPosition < chapterList.size() - 1 ? 1f : 0.3f);

        btnPrev.setOnClickListener(v -> {
            if (currentPosition > 0) {
                currentPosition--;
                currentChapter = chapterList.get(currentPosition);
                loadChapter();
                updateNavigationButtons();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPosition < chapterList.size() - 1) {
                currentPosition++;
                currentChapter = chapterList.get(currentPosition);
                loadChapter();
                updateNavigationButtons();
            }
        });
        
        btnReload.setOnClickListener(v -> loadChapter());
        
        btnComments.setOnClickListener(v -> showAddCommentDialog());
        
        btnAddChapterComment.setOnClickListener(v -> showAddCommentDialog());
    }

    private void showAddCommentDialog() {
        SharedPrefsHelper prefs = SharedPrefsHelper.getInstance(this);
        if (!prefs.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_comment, null);
        EditText etComment = dialogView.findViewById(R.id.et_comment_content);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String content = etComment.getText().toString().trim();
                    if (!content.isEmpty()) {
                        String commentId = UUID.randomUUID().toString();
                        String userId = prefs.getLoggedInEmail();
                        String userName = prefs.getUserName();

                        Comment newComment = new Comment(
                                commentId,
                                currentChapter.getId(),
                                userId,
                                userName,
                                content
                        );

                        StoryManager.getInstance(this).addChapterComment(newComment);
                        
                        chapterComments.add(0, newComment);
                        commentAdapter.notifyDataSetChanged();

                        Toast.makeText(this, "Đã thêm bình luận", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Nội dung bình luận không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void updateNavigationButtons() {
        btnPrev.setEnabled(currentPosition > 0);
        btnNext.setEnabled(currentPosition < chapterList.size() - 1);
        btnPrev.setAlpha(currentPosition > 0 ? 1f : 0.3f);
        btnNext.setAlpha(currentPosition < chapterList.size() - 1 ? 1f : 0.3f);
    }
    
    private void showChapterPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_chapter_list, null);
        RecyclerView recyclerView = popupView.findViewById(R.id.rv_chapters);
        
        ChapterListAdapter adapter = new ChapterListAdapter(chapterList, currentPosition, 0, new ChapterListAdapter.OnChapterClickListener() {
            @Override
            public void onChapterClick(int position, Chapter chapter) {
                currentPosition = position;
                currentChapter = chapterList.get(position);
                loadChapter();
                chapterPopupWindow.dismiss();
                updateNavigationButtons();
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int headerHeight = 56;
        int footerHeight = 56;
        int maxPopupHeight = screenHeight - headerHeight - footerHeight - 32;
        
        chapterPopupWindow = new PopupWindow(popupView, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                maxPopupHeight, true);
        
        chapterPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_popup, getTheme()));
        chapterPopupWindow.setOutsideTouchable(true);
        
        chapterPopupWindow.showAsDropDown(ivMenu, -200, 0);
    }
}