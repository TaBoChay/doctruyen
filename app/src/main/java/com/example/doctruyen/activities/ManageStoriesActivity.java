package com.example.doctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doctruyen.R;
import com.example.doctruyen.adapters.StoryAdapter;
import com.example.doctruyen.models.Story;
import com.example.doctruyen.utils.SharedPrefsHelper;
import com.example.doctruyen.utils.StoryManager;
import java.util.ArrayList;
import android.view.ViewGroup;

public class ManageStoriesActivity extends AppCompatActivity implements
        StoryManager.StoryLoadListener,
        StoryAdapter.OnItemLongClickListener,
        StoryAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private StoryAdapter adapter;
    private View layoutEmpty;
    private SharedPrefsHelper prefsHelper;
    private StoryManager storyManager;

    private LinearLayout toolbarDefault, toolbarMultiSelect;
    private TextView btnEnterSelectMode, tvSelectionCount;
    private View btnDeleteSelectedContainer;

    private boolean isMultiSelectMode = false;

    private void applyToolbarMargin() {
        if (toolbarDefault == null || toolbarMultiSelect == null || recyclerView == null || layoutEmpty == null) return;
        toolbarMultiSelect.post(() -> {
            int toolbarHeight = Math.max(toolbarDefault.getHeight(), toolbarMultiSelect.getHeight());
            ViewGroup.MarginLayoutParams recyclerParams = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
            recyclerParams.topMargin = toolbarHeight;
            recyclerView.setLayoutParams(recyclerParams);
            ViewGroup.MarginLayoutParams emptyParams = (ViewGroup.MarginLayoutParams) layoutEmpty.getLayoutParams();
            emptyParams.topMargin = toolbarHeight;
            layoutEmpty.setLayoutParams(emptyParams);
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_stories);

        prefsHelper = SharedPrefsHelper.getInstance(this);
        storyManager = StoryManager.getInstance(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        loadTranslatorStories();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_stories);
        layoutEmpty = findViewById(R.id.layout_empty);

        toolbarDefault = findViewById(R.id.toolbar_default);
        toolbarMultiSelect = findViewById(R.id.toolbar_multi_select);
        btnEnterSelectMode = findViewById(R.id.btn_enter_select_mode);
        tvSelectionCount = findViewById(R.id.tv_selection_count);
        btnDeleteSelectedContainer = findViewById(R.id.cv_delete_selected);

        // Set title
        TextView tvHeader = findViewById(R.id.tv_header);
        tvHeader.setText("Truyện đã đăng");

        // Áp dụng margin cho RecyclerView và layout trống để tránh bị che
        applyToolbarMargin();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new StoryAdapter(this, new ArrayList<>());

        adapter.setOnItemLongClickListener(this);
        adapter.setOnItemClickListener(this);

        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        if (btnEnterSelectMode != null) {
            btnEnterSelectMode.setOnClickListener(v -> enterMultiSelectMode());
        }

        if (btnDeleteSelectedContainer != null) {
            btnDeleteSelectedContainer.setOnClickListener(v -> showDeleteConfirmationDialog());
        }
    }

    private void loadTranslatorStories() {
        String translatorEmail = prefsHelper.getUserEmail();
        ArrayList<Story> translatorStories = storyManager.getStoriesByTranslator(translatorEmail);

        if (translatorStories.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            if (toolbarDefault != null) toolbarDefault.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateList(translatorStories);
            if (!isMultiSelectMode && toolbarDefault != null) toolbarDefault.setVisibility(View.VISIBLE);
        }
    }

    // ----------------------------------------------------
    // --- QUẢN LÝ CHẾ ĐỘ CHỌN ---
    // ----------------------------------------------------

    private void enterMultiSelectMode() {
        isMultiSelectMode = true;
        adapter.setMultiSelectMode(true);
        if (toolbarDefault != null) toolbarDefault.setVisibility(View.GONE);
        if (toolbarMultiSelect != null) toolbarMultiSelect.setVisibility(View.VISIBLE);
        updateSelectionCount(0);
    }

    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        adapter.setMultiSelectMode(false);
        if (toolbarDefault != null) toolbarDefault.setVisibility(View.VISIBLE);
        if (toolbarMultiSelect != null) toolbarMultiSelect.setVisibility(View.GONE);
    }

    private void updateSelectionCount(int count) {
        if (tvSelectionCount != null) {
            tvSelectionCount.setText("Đã chọn " + count);
        }
        if (btnDeleteSelectedContainer != null) {
            btnDeleteSelectedContainer.setEnabled(count > 0);
            btnDeleteSelectedContainer.setAlpha(count > 0 ? 1.0f : 0.5f);
        }
    }

    @Override
    public void onItemLongClick(int position) {
        if (!isMultiSelectMode) {
            enterMultiSelectMode();
        }
        adapter.toggleSelection(position);
        updateSelectionCount(adapter.getSelectedItemCount());
    }

    @Override
    public void onItemClick(int position) {
        if (isMultiSelectMode) {
            adapter.toggleSelection(position);
            updateSelectionCount(adapter.getSelectedItemCount());
            if (adapter.getSelectedItemCount() == 0) {
                exitMultiSelectMode();
            }
        } else {
            // Open edit activity for the selected story
            Story story = adapter.getStoryAt(position);
            if (story != null) {
                Intent intent = new Intent(this, EditStoryActivity.class);
                intent.putExtra("story_id", story.getId());
                startActivity(intent);
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        final int count = adapter.getSelectedItemCount();
        if (count == 0) return;

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa " + count + " truyện đã đăng không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteSelectedStories())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteSelectedStories() {
        java.util.List<Story> selectedStories = adapter.getSelectedStories();
        int deletedCount = 0;
        
        for (Story story : selectedStories) {
            boolean success = storyManager.deleteLocalStory(story.getId());
            if (success) {
                deletedCount++;
            }
        }
        
        Toast.makeText(this, "Đã xóa " + deletedCount + " truyện.", Toast.LENGTH_LONG).show();
        exitMultiSelectMode();
        loadTranslatorStories();
    }

    @Override
    public void onStoriesLoaded() {
        loadTranslatorStories();
    }

    @Override
    public void onStoriesLoadFailed(String error) {
        Toast.makeText(this, "Lỗi tải truyện: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTranslatorStories();
    }
}