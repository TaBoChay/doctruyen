package com.example.doctruyen.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doctruyen.R;
import com.example.doctruyen.adapters.RankingAdapter; // ĐÃ SỬA: Import RankingAdapter
import com.example.doctruyen.models.Story;
import com.example.doctruyen.utils.StoryManager;
import java.util.ArrayList;

// Triển khai StoryLoadListener
public class XepHangFragment extends Fragment implements StoryManager.StoryLoadListener {

    private RecyclerView recyclerView;
    private RankingAdapter adapter; // ĐÃ SỬA: Sử dụng RankingAdapter

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_xep_hang, container, false);

        recyclerView = view.findViewById(R.id.recycler_ranking);
        setupRecyclerView();

        // Bắt đầu tải dữ liệu bất đồng bộ
        StoryManager.getInstance(getContext()).loadStoriesAsync(this);

        return view;
    }

    private void setupRecyclerView() {
        // Ranking thường dùng LinearLayoutManager (vertical)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // ĐÃ SỬA: Khởi tạo RankingAdapter với danh sách rỗng
        adapter = new RankingAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    // Phương thức được gọi khi tải dữ liệu xong
    @Override
    public void onStoriesLoaded() {
        if (!isAdded()) return;

        // Lấy danh sách truyện đã được sắp xếp
        ArrayList<Story> topStories = StoryManager.getInstance(getContext()).getTopRatedStories();

        // Dùng updateList của RankingAdapter
        adapter.updateList(topStories);

        if (topStories.isEmpty()) {
            Toast.makeText(getContext(), "Không có truyện nào để xếp hạng.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStoriesLoadFailed(String error) {
        if (!isAdded()) return;
        Toast.makeText(getContext(), "Lỗi tải xếp hạng: " + error, Toast.LENGTH_LONG).show();
    }
}