package com.example.doctruyen.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class TrangChuFragment extends Fragment implements StoryManager.StoryLoadListener, SearchView.OnQueryTextListener {

    private static final String TAG = "TrangChuFragment";

    private SearchView searchView;
    private RecyclerView recyclerFeatured, recyclerAllStories;
    private StoryAdapter featuredAdapter, allStoriesAdapter;
    private ArrayList<Story> allStories;
    private TextView tvLoading;

    private ImageView ivExpandFeatured, ivExpandAll;
    private boolean isFeaturedExpanded = true;
    private boolean isAllExpanded = true;

    // --- Phương thức hỗ trợ tải lại dữ liệu ban đầu ---
    private void loadInitialData() {
        if (allStories == null || allStories.isEmpty()) return;

        List<Story> topStories = StoryManager.getInstance(getContext()).getTopRatedStories();
        if (topStories.size() > 5) topStories = topStories.subList(0, 5);
        featuredAdapter.updateList(new ArrayList<>(topStories));

        allStoriesAdapter.updateList(new ArrayList<>(allStories));

        recyclerFeatured.setVisibility(isFeaturedExpanded ? View.VISIBLE : View.GONE);
        recyclerAllStories.setVisibility(isAllExpanded ? View.VISIBLE : View.GONE);
    }
    // ----------------------------------------------------


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trang_chu, container, false);

        searchView = view.findViewById(R.id.search_view);
        recyclerFeatured = view.findViewById(R.id.recycler_featured);
        recyclerAllStories = view.findViewById(R.id.recycler_all_stories);
        ivExpandFeatured = view.findViewById(R.id.iv_expand_featured);
        ivExpandAll = view.findViewById(R.id.iv_expand_all);
        tvLoading = view.findViewById(R.id.tv_loading);

        allStories = new ArrayList<>();

        setupRecyclerViews();
        setupSearch();
        setupExpandCollapse();

        loadStoriesAsync();

        return view;
    }

    private void setupRecyclerViews() {
        recyclerFeatured.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerAllStories.setLayoutManager(new GridLayoutManager(getContext(), 2));

        featuredAdapter = new StoryAdapter(getContext(), new ArrayList<>());
        featuredAdapter.setOnItemClickListener(new StoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Story story = featuredAdapter.getStoryAt(position);
                if (story != null) {
                    Intent intent = new Intent(getContext(), StoryDetailActivity.class);
                    intent.putExtra("storyId", story.getId());
                    startActivity(intent);
                }
            }
        });
        recyclerFeatured.setAdapter(featuredAdapter);
        
        allStoriesAdapter = new StoryAdapter(getContext(), new ArrayList<>());
        allStoriesAdapter.setOnItemClickListener(new StoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Story story = allStoriesAdapter.getStoryAt(position);
                if (story != null) {
                    Intent intent = new Intent(getContext(), StoryDetailActivity.class);
                    intent.putExtra("storyId", story.getId());
                    startActivity(intent);
                }
            }
        });
        recyclerAllStories.setAdapter(allStoriesAdapter);

        recyclerFeatured.setItemAnimator(null);
        recyclerAllStories.setItemAnimator(null);
    }

    private void loadStoriesAsync() {
        if (tvLoading != null) tvLoading.setVisibility(View.VISIBLE);
        recyclerFeatured.setVisibility(View.GONE);
        recyclerAllStories.setVisibility(View.GONE);

        StoryManager manager = StoryManager.getInstance(getContext());
        manager.loadStoriesAsync(this);
    }

    @Override
    public void onStoriesLoaded() {
        if (!isAdded()) return;

        StoryManager manager = StoryManager.getInstance(getContext());
        allStories = manager.getAllStories();

        if (allStories.isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy truyện nào. Kiểm tra file Assets.", Toast.LENGTH_LONG).show();
            if (tvLoading != null) tvLoading.setText("Không có dữ liệu.");
            return;
        }

        if (tvLoading != null) tvLoading.setVisibility(View.GONE);

        loadInitialData();

        Log.d(TAG, "Stories loaded successfully. Total: " + allStories.size());
    }

    @Override
    public void onStoriesLoadFailed(String error) {
        if (!isAdded()) return;
        Toast.makeText(getContext(), "Lỗi tải truyện: " + error, Toast.LENGTH_LONG).show();
        if (tvLoading != null) tvLoading.setText("Lỗi: " + error);
        Log.e(TAG, "Story loading failed: " + error);
    }

    private void setupSearch() {
        searchView.setQueryHint("Tìm kiếm truyện...");
        searchView.clearFocus();

        // Áp dụng Listener đã triển khai trong class này
        searchView.setOnQueryTextListener(this);
    }

    // --- PHƯƠNG THỨC XỬ LÝ SEARCHVIEW.ONQUERYTEXTLISTENER ---
    @Override
    public boolean onQueryTextSubmit(String query) {
        filterStories(query);
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filterStories(newText);
        return true;
    }
    // --------------------------------------------------------


    private void filterStories(String query) {
        if (allStories == null) return;

        String lowerQuery = query.toLowerCase().trim();

        if (lowerQuery.isEmpty()) {
            loadInitialData();
            return;
        }

        ArrayList<Story> filteredList = new ArrayList<>();

        for (Story story : allStories) {

            // Lấy các trường cần tìm kiếm
            String author = story.getAuthor() != null ? story.getAuthor().toLowerCase() : "";
            String title = story.getTitle() != null ? story.getTitle().toLowerCase() : "";
            // THÊM: Lấy folderName (Cần đảm bảo getFolderName() tồn tại trong Story.java)
            String folderName = story.getFolderName() != null ? story.getFolderName().toLowerCase() : "";

            // LOGIC LỌC: Tìm kiếm theo tên thư mục, Title, hoặc Author
            if (folderName.contains(lowerQuery) ||
                    title.contains(lowerQuery) ||
                    author.contains(lowerQuery)) {

                filteredList.add(story);
            }
        }

        allStoriesAdapter.updateList(filteredList);

        recyclerFeatured.setVisibility(View.GONE);
        recyclerAllStories.setVisibility(View.VISIBLE);
        fadeIn(recyclerAllStories);


    }


    private void setupExpandCollapse() {
        ivExpandFeatured.setRotation(180);
        ivExpandAll.setRotation(180);

        ivExpandFeatured.setOnClickListener(v -> isFeaturedExpanded = toggleSection(recyclerFeatured, ivExpandFeatured, isFeaturedExpanded));
        ivExpandAll.setOnClickListener(v -> isAllExpanded = toggleSection(recyclerAllStories, ivExpandAll, isAllExpanded));
    }

    private boolean toggleSection(View recycler, ImageView arrow, boolean isExpanded) {
        if (isExpanded) {
            AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeOut.setDuration(200);
            recycler.startAnimation(fadeOut);
            recycler.setVisibility(View.GONE);
            arrow.animate().rotation(0).setDuration(200).start();
        } else {
            recycler.setVisibility(View.VISIBLE);
            fadeIn(recycler);
            arrow.animate().rotation(180).setDuration(200).start();
        }
        return !isExpanded;
    }

    private void fadeIn(View view) {
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(250);
        view.startAnimation(fadeIn);
    }
}