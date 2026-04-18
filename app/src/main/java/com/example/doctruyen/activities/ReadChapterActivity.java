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
        
        btnComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, StoryDetailActivity.class);
            intent.putExtra("story_id", storyId);
            intent.putExtra("tab", "comments");
            startActivity(intent);
        });
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
        
        int[] location = new int[2];
        ivMenu.getLocationOnScreen(location);
        int popupY = location[1] + location[1] - maxPopupHeight;
        chapterPopupWindow.showAtLocation(ivMenu, android.view.Gravity.NO_GRAVITY, location[0] - 200, popupY);
    }
}