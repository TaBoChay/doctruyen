package com.example.doctruyen.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doctruyen.R;
import com.example.doctruyen.adapters.ChapterManageAdapter;
import com.example.doctruyen.models.Chapter;
import com.example.doctruyen.models.Story;
import com.example.doctruyen.utils.SharedPrefsHelper;
import com.example.doctruyen.utils.StoryManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ManageChaptersActivity extends AppCompatActivity implements ChapterManageAdapter.OnChapterEditListener {

    public static final String EXTRA_STORY_ID = "story_id";

    private StoryManager storyManager;
    private SharedPrefsHelper prefsHelper;
    private Story story;
    private ArrayList<Chapter> chapters;

    private RecyclerView recyclerChapters;
    private LinearLayout layoutEmpty;
    private TextView tvChapterCount;
    private ChapterManageAdapter adapter;

    private static final int PICK_FILE_REQUEST = 100;

    private int editingChapterPosition = -1;
    private String pendingFileContent = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_chapters);

        storyManager = StoryManager.getInstance(this);
        prefsHelper = SharedPrefsHelper.getInstance(this);

        String storyId = getIntent().getStringExtra(EXTRA_STORY_ID);
        if (storyId == null) {
            Toast.makeText(this, "Không tìm thấy truyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        story = storyManager.getStoryById(storyId);
        if (story == null) {
            Toast.makeText(this, "Không tìm thấy truyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chapters = storyManager.getChaptersByStoryId(storyId);

        initViews();
        setupRecyclerView();
        setupListeners();
        updateUI();
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        recyclerChapters = findViewById(R.id.recycler_chapters);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvChapterCount = findViewById(R.id.tv_chapter_count);
        findViewById(R.id.fab_add_chapter).setOnClickListener(v -> showAddChapterDialog());
    }

    private void setupRecyclerView() {
        adapter = new ChapterManageAdapter(chapters, this);
        recyclerChapters.setLayoutManager(new LinearLayoutManager(this));
        recyclerChapters.setAdapter(adapter);
    }

    private void setupListeners() {
    }

    private void updateUI() {
        if (chapters == null || chapters.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerChapters.setVisibility(View.GONE);
            tvChapterCount.setText("0 chương");
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerChapters.setVisibility(View.VISIBLE);
            tvChapterCount.setText(chapters.size() + " chương");
            adapter.updateList(chapters);
        }
    }

    private void showAddChapterDialog() {
        int newChapterNum = (chapters != null ? chapters.size() : 0) + 1;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_chapter_edit, null);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvChapterNumber = dialogView.findViewById(R.id.tv_chapter_number);
        EditText etChapterTitle = dialogView.findViewById(R.id.et_chapter_title);
        Button btnPickFile = dialogView.findViewById(R.id.btn_pick_file);
        TextView tvFileName = dialogView.findViewById(R.id.tv_file_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        tvDialogTitle.setText("Thêm chương mới");
        tvChapterNumber.setText("Chương " + newChapterNum);

        pendingFileContent = "";
        editingChapterPosition = -1;

        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/plain");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Chọn file .txt"), PICK_FILE_REQUEST);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etChapterTitle.getText().toString().trim();
            String content = pendingFileContent;

            if (content.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn file .txt", Toast.LENGTH_SHORT).show();
                return;
            }

            if (title.isEmpty()) {
                title = "Chương " + newChapterNum;
            }

            addNewChapter(title, content, newChapterNum);
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onEditChapter(int position, Chapter chapter) {
        showEditChapterDialog(position, chapter);
    }

    private void showEditChapterDialog(int position, Chapter chapter) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_chapter_edit, null);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvChapterNumber = dialogView.findViewById(R.id.tv_chapter_number);
        EditText etChapterTitle = dialogView.findViewById(R.id.et_chapter_title);
        Button btnPickFile = dialogView.findViewById(R.id.btn_pick_file);
        TextView tvFileName = dialogView.findViewById(R.id.tv_file_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        tvDialogTitle.setText("Chỉnh sửa chương");
        tvChapterNumber.setText("Chương " + chapter.getChapterNumber());
        etChapterTitle.setText(chapter.getTitle());
        tvFileName.setText("Đã có nội dung");

        editingChapterPosition = position;
        pendingFileContent = chapter.getContent();

        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/plain");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Chọn file .txt"), PICK_FILE_REQUEST);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etChapterTitle.getText().toString().trim();

            if (title.isEmpty()) {
                title = "Chương " + chapter.getChapterNumber();
            }

            chapter.setTitle(title);
            if (!pendingFileContent.isEmpty()) {
                chapter.setContent(pendingFileContent);
            }

            storyManager.updateChapter(chapter);

            chapters.set(position, chapter);
            adapter.notifyItemChanged(position);

            Toast.makeText(this, "Đã cập nhật chương", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addNewChapter(String title, String content, int chapterNum) {
        String chapterId = story.getId() + "_" + chapterNum;

        Chapter newChapter = new Chapter(
                chapterId,
                story.getId(),
                title,
                chapterNum,
                ""
        );
        newChapter.setContent(content);

        storyManager.addChapter(newChapter);

        if (chapters == null) {
            chapters = new ArrayList<>();
        }
        chapters.add(newChapter);

        updateUI();
        Toast.makeText(this, "Đã thêm chương " + chapterNum, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }
                        reader.close();
                        inputStream.close();

                        pendingFileContent = stringBuilder.toString();

                        String fileName = uri.getLastPathSegment();
                        if (fileName == null) {
                            fileName = uri.getPath();
                            if (fileName != null) {
                                int lastSlash = fileName.lastIndexOf('/');
                                if (lastSlash != -1) {
                                    fileName = fileName.substring(lastSlash + 1);
                                }
                            }
                        }

                        Toast.makeText(this, "Đã chọn: " + fileName, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Lỗi đọc file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
