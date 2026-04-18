package com.example.doctruyen.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doctruyen.R;
import com.example.doctruyen.activities.StoryDetailActivity;
import com.example.doctruyen.adapters.StoryAdapter;
import com.example.doctruyen.models.Story;
import com.example.doctruyen.utils.StoryManager;

import java.util.ArrayList;
import java.util.List;

public class TuTruyenFragment extends Fragment implements
        StoryManager.StoryLoadListener,
        StoryAdapter.OnItemLongClickListener,
        StoryAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private StoryAdapter adapter;
    private View layoutEmpty;

    private LinearLayout toolbarDefault, toolbarMultiSelect;
    private TextView btnEnterSelectMode, tvSelectionCount;
    private ImageView btnCancelSelectMode;
    private View btnDeleteSelectedContainer;

    private boolean isMultiSelectMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tu_truyen, container, false);

        // 1. Ánh xạ View
        recyclerView = view.findViewById(R.id.recycler_view_library);
        layoutEmpty = view.findViewById(R.id.layout_empty);

        toolbarDefault = view.findViewById(R.id.toolbar_default);
        toolbarMultiSelect = view.findViewById(R.id.toolbar_multi_select);
        btnEnterSelectMode = view.findViewById(R.id.btn_enter_select_mode);
        tvSelectionCount = view.findViewById(R.id.tv_selection_count);
        btnCancelSelectMode = view.findViewById(R.id.btn_cancel_select_mode);

        btnDeleteSelectedContainer = view.findViewById(R.id.cv_delete_selected);

        // FIX LỖI ĐÈ LÊN: Gọi hàm áp dụng Margin ngay sau khi ánh xạ
        applyToolbarMargin();

        // 2. Setup RecyclerView và Adapter
        setupRecyclerView();

        // 3. Thiết lập Listeners cho các nút
        if (btnEnterSelectMode != null) {
            btnEnterSelectMode.setOnClickListener(v -> enterMultiSelectMode());
        }
        if (btnCancelSelectMode != null) {
            btnCancelSelectMode.setOnClickListener(v -> exitMultiSelectMode());
        }

        if (btnDeleteSelectedContainer != null) {
            btnDeleteSelectedContainer.setOnClickListener(v -> showDeleteConfirmationDialog());
        }

        StoryManager.getInstance(getContext()).loadStoriesAsync(this);

        return view;
    }

    // --- PHƯƠNG THỨC FIX LỖI ĐÈ LÊN ---
    private void applyToolbarMargin() {
        if (toolbarDefault == null || toolbarMultiSelect == null || recyclerView == null || layoutEmpty == null) return;

        // Đợi View được đo đạc (post to run after layout pass)
        toolbarMultiSelect.post(() -> {
            // Lấy chiều cao lớn nhất giữa hai Toolbar
            // Chiều cao MultiSelect thường là chiều cao lớn nhất (do padding/icon)
            int toolbarHeight = Math.max(toolbarDefault.getHeight(), toolbarMultiSelect.getHeight());

            // Áp dụng Margin Top cho RecyclerView
            ViewGroup.MarginLayoutParams recyclerParams =
                    (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
            recyclerParams.topMargin = toolbarHeight;
            recyclerView.setLayoutParams(recyclerParams);

            // Áp dụng Margin Top cho Layout Trống
            ViewGroup.MarginLayoutParams emptyParams =
                    (ViewGroup.MarginLayoutParams) layoutEmpty.getLayoutParams();
            emptyParams.topMargin = toolbarHeight;
            layoutEmpty.setLayoutParams(emptyParams);

            // Debugging: Log.d("MARGIN_FIX", "Applied margin: " + toolbarHeight);
        });
    }
    // ------------------------------------

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new StoryAdapter(getContext(), new ArrayList<>());

        adapter.setOnItemLongClickListener(this);
        adapter.setOnItemClickListener(this);

        recyclerView.setAdapter(adapter);
    }

    private void loadLibraryStories() {
        ArrayList<Story> libraryStories = StoryManager.getInstance(getContext()).getStoriesInLibrary();

        if (libraryStories.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            if (toolbarDefault != null) toolbarDefault.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateList(libraryStories);
            if (!isMultiSelectMode && toolbarDefault != null) toolbarDefault.setVisibility(View.VISIBLE);
        }
    }

    // ----------------------------------------------------
    // --- QUẢN LÝ CHẾ ĐỘ CHỌN (Giữ nguyên) ---
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
            updateSelectionCount(adapter.getSelectedItemCount());
            if (adapter.getSelectedItemCount() == 0) {
                exitMultiSelectMode();
            }
        } else {
            Story story = adapter.getStoryAt(position);
            if (story != null) {
                Intent intent = new Intent(getContext(), StoryDetailActivity.class);
                intent.putExtra("storyId", story.getId());
                startActivity(intent);
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        if (getContext() == null) return;
        final int count = adapter.getSelectedItemCount();
        if (count == 0) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa " + count + " truyện khỏi Tủ truyện không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteSelectedStories())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteSelectedStories() {
        if (getContext() == null) return;
        List<Story> selectedStories = adapter.getSelectedStories();

        StoryManager.getInstance(getContext()).removeStoriesFromLibrary(selectedStories);

        loadLibraryStories();

        exitMultiSelectMode();

        Toast.makeText(getContext(), "Đã xóa " + selectedStories.size() + " truyện khỏi Tủ truyện.", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onStoriesLoaded() {
        if (!isAdded()) return;
        loadLibraryStories();
    }

    @Override
    public void onStoriesLoadFailed(String error) {
        if (!isAdded()) return;
        Toast.makeText(getContext(), "Lỗi tải Tủ truyện: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (StoryManager.getInstance(getContext()).getAllStories().size() > 0) {
            loadLibraryStories();
        }
    }
}