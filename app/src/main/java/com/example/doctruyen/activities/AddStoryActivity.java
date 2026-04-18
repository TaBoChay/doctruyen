package com.example.doctruyen.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.doctruyen.R;
import com.example.doctruyen.models.Chapter;
import com.example.doctruyen.models.Story;
import com.example.doctruyen.utils.SharedPrefsHelper;
import com.example.doctruyen.utils.StoryManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;

public class AddStoryActivity extends AppCompatActivity {

    private EditText etTitle, etAuthor, etDescription, etFolderName;
    private Spinner spinnerCategory;
    private Button btnSave, btnAddChapter, btnPickCover, btnPickBanner;
    private ImageView ivCover, ivBanner, btnBack;
    private LinearLayout layoutChapters;
    private SharedPrefsHelper prefsHelper;
    private StoryManager storyManager;

    private String coverBase64 = "";
    private String bannerBase64 = "";
    private ArrayList<EditText> chapterInputs = new ArrayList<>();

    private static final int PICK_COVER_REQUEST = 1;
    private static final int PICK_BANNER_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_story);

        prefsHelper = SharedPrefsHelper.getInstance(this);
        storyManager = StoryManager.getInstance(this);

        if (!prefsHelper.isTranslator()) {
            Toast.makeText(this, "Bạn cần đăng nhập với tài khoản dịch giả để đăng truyện.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupCategorySpinner();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_story_title);
        etAuthor = findViewById(R.id.et_story_author);
        etDescription = findViewById(R.id.et_story_description);
        etFolderName = findViewById(R.id.et_folder_name);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSave = findViewById(R.id.btn_save_story);
        btnAddChapter = findViewById(R.id.btn_add_chapter);
        btnPickCover = findViewById(R.id.btn_pick_cover);
        btnPickBanner = findViewById(R.id.btn_pick_banner);
        ivCover = findViewById(R.id.iv_cover);
        ivBanner = findViewById(R.id.iv_banner);
        btnBack = findViewById(R.id.btn_back);
        layoutChapters = findViewById(R.id.layout_chapters);
    }

    private void setupCategorySpinner() {
        String[] categories = {"Tiên Hiệp", "Ngôn Tình", "Kiếm Hiệp", "Kỳ Ảo", "Trinh Thám", "Huyền Huyễn", "Võng Du", "Đô Thị", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnPickCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_COVER_REQUEST);
        });

        btnPickBanner.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_BANNER_REQUEST);
        });

        btnAddChapter.setOnClickListener(v -> addChapterInput());

        btnSave.setOnClickListener(v -> saveStory());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            
            // Check if it's an image request (cover or banner)
            if (requestCode == PICK_COVER_REQUEST || requestCode == PICK_BANNER_REQUEST) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(selectedUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] imageBytes = baos.toByteArray();
                    String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                    if (requestCode == PICK_COVER_REQUEST) {
                        coverBase64 = base64;
                        ivCover.setImageBitmap(bitmap);
                    } else if (requestCode == PICK_BANNER_REQUEST) {
                        bannerBase64 = base64;
                        ivBanner.setImageBitmap(bitmap);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Lỗi chọn ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } 
            // Check if it's a chapter file request
            else if (requestCode > PICK_BANNER_REQUEST) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(selectedUri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    reader.close();
                    inputStream.close();
                    
                    // Find the corresponding chapter layout
                    int chapterIndex = requestCode - PICK_COVER_REQUEST - 1;
                    if (chapterIndex >= 0 && chapterIndex < layoutChapters.getChildCount()) {
                        View chapterView = layoutChapters.getChildAt(chapterIndex);
                        if (chapterView instanceof LinearLayout) {
                            LinearLayout chapterLayout = (LinearLayout) chapterView;
                            
                            // Update file name display
                            TextView tvFileName = (TextView) chapterLayout.getChildAt(3); // TextView is 4th child
                            tvFileName.setText("Đã chọn: " + getFileName(selectedUri));
                            tvFileName.setVisibility(View.VISIBLE);
                            
                            // Hide the EditText for manual content
                            EditText etContent = (EditText) chapterLayout.getChildAt(4); // EditText is 5th child
                            etContent.setVisibility(View.GONE);
                            
                            // Store the content in the EditText (even though it's hidden)
                            etContent.setText(content.toString());
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Lỗi đọc file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    // Helper method to get file name from Uri
    private String getFileName(Uri uri) {
        String fileName = "unknown.txt";
        try {
            String path = uri.getPath();
            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1) {
                    fileName = path.substring(lastSlash + 1);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return fileName;
    }

    private void addChapterInput() {
        int chapterNum = chapterInputs.size() + 1;
        
        LinearLayout chapterLayout = new LinearLayout(this);
        chapterLayout.setOrientation(LinearLayout.VERTICAL);
        chapterLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        chapterLayout.setPadding(0, 16, 0, 16);

        TextView tvChapterTitle = new TextView(this);
        tvChapterTitle.setText("Chương " + chapterNum);
        tvChapterTitle.setTextSize(16);
        tvChapterTitle.setTextColor(getResources().getColor(R.color.text_primary, null));

        EditText etChapterTitle = new EditText(this);
        etChapterTitle.setHint("Tiêu đề chương " + chapterNum);
        etChapterTitle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        etChapterTitle.setBackgroundResource(R.drawable.bg_input);
        etChapterTitle.setPadding(32, 24, 32, 24);

        // EditText for chapter content (will be hidden when using file upload)
        EditText etChapterContent = new EditText(this);
        etChapterContent.setHint("Nội dung chương " + chapterNum + "...");
        etChapterContent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        etChapterContent.setMinLines(5);
        etChapterContent.setBackgroundResource(R.drawable.bg_input);
        etChapterContent.setPadding(32, 24, 32, 24);

        // TextView to show selected file name
        TextView tvFileName = new TextView(this);
        tvFileName.setText("Chưa chọn file");
        tvFileName.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tvFileName.setPadding(32, 16, 32, 16);
        tvFileName.setTextColor(getResources().getColor(R.color.text_secondary, null));
        tvFileName.setVisibility(View.GONE); // Hidden by default

        // Button to upload file
        Button btnUploadFile = new Button(this);
        btnUploadFile.setText("Chọn file .txt");
        btnUploadFile.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        btnUploadFile.setBackgroundResource(R.drawable.bg_button_secondary);
        btnUploadFile.setPadding(32, 16, 32, 16);
        btnUploadFile.setTextColor(getResources().getColor(R.color.text_primary, null));

        // Store file content
        final String[] fileContent = {""};

        btnUploadFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/plain"); // Only allow .txt files
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Chọn file .txt"), PICK_COVER_REQUEST + chapterNum);
        });

        Button btnRemove = new Button(this);
        btnRemove.setText("Xóa chương");
        btnRemove.setTextColor(getResources().getColor(android.R.color.white, null));
        btnRemove.setBackgroundColor(getResources().getColor(R.color.button_danger, null));
        btnRemove.setOnClickListener(v -> {
            layoutChapters.removeView(chapterLayout);
            chapterInputs.remove(etChapterContent);
            renumberChapters();
        });

        chapterLayout.addView(tvChapterTitle);
        chapterLayout.addView(etChapterTitle);
        chapterLayout.addView(btnUploadFile);
        chapterLayout.addView(tvFileName);
        chapterLayout.addView(etChapterContent);
        chapterLayout.addView(btnRemove);

        layoutChapters.addView(chapterLayout);
        chapterInputs.add(etChapterContent);
    }

    private void renumberChapters() {
        int count = layoutChapters.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = layoutChapters.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) child;
                TextView tv = (TextView) ll.getChildAt(0);
                tv.setText("Chương " + (i + 1));
            }
        }
    }

    private void saveStory() {
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String folderName = etFolderName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty() || author.isEmpty() || folderName.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        folderName = folderName.replace(" ", "_").toLowerCase();
        folderName = folderName.replaceAll("[^a-z0-9_]", "");

        if (chapterInputs.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất 1 chương.", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = UUID.randomUUID().toString();
        String translatorEmail = prefsHelper.getUserEmail();

        try {
            String finalFolderName = saveToLocalStorage(id, folderName, title, author, category, description, translatorEmail);
            
            File storiesDir = new File(getFilesDir(), "Data");
            File storyFolder = new File(storiesDir, finalFolderName);
            String coverPath = "file:" + storyFolder.getAbsolutePath() + "/bia.jpg";
            String bannerPath = "file:" + storyFolder.getAbsolutePath() + "/banner.jpg";

            Story newStory = new Story(id, title, author, description, coverPath, bannerPath, category, finalFolderName);
            newStory.setTranslatorEmail(translatorEmail);
            
            ArrayList<Chapter> chapters = new ArrayList<>();
            for (int i = 0; i < chapterInputs.size(); i++) {
                EditText etContent = chapterInputs.get(i);
                LinearLayout parent = (LinearLayout) etContent.getParent();
                EditText etTitle = (EditText) parent.getChildAt(1);
                String chapterTitle = etTitle.getText().toString().trim();
                if (chapterTitle.isEmpty()) {
                    chapterTitle = "Chương " + (i + 1);
                }
                
                String chapterFileName = "chuong_" + (i + 1) + ".txt";
                File chapterFile = new File(new File(storyFolder, "Chapters"), chapterFileName);
                
                Chapter chapter = new Chapter(
                        id + "_" + (i + 1),
                        id,
                        chapterTitle,
                        i + 1,
                        "file:" + chapterFile.getAbsolutePath()
                );
                chapter.setContent(etContent.getText().toString());
                chapters.add(chapter);
            }
            newStory.setChapters(chapters);

            storyManager.addStory(newStory);

            Toast.makeText(this, "Đăng truyện thành công! Đã tạo " + chapterInputs.size() + " chương.", Toast.LENGTH_LONG).show();
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi lưu truyện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String saveToLocalStorage(String storyId, String folderName, String title, String author, 
                          String category, String description, String translatorEmail) throws Exception {
        
        File storiesDir = new File(getFilesDir(), "Data");
        if (!storiesDir.exists()) {
            storiesDir.mkdirs();
        }

        File storyFolder = new File(storiesDir, folderName);
        if (storyFolder.exists()) {
            folderName = folderName + "_" + System.currentTimeMillis();
            storyFolder = new File(storiesDir, folderName);
        }
        storyFolder.mkdirs();

        File coverFile = new File(storyFolder, "bia.jpg");
        if (!coverBase64.isEmpty()) {
            byte[] coverBytes = Base64.decode(coverBase64, Base64.NO_WRAP);
            FileOutputStream fos = new FileOutputStream(coverFile);
            fos.write(coverBytes);
            fos.close();
        }

        File bannerFile = new File(storyFolder, "banner.jpg");
        if (!bannerBase64.isEmpty()) {
            byte[] bannerBytes = Base64.decode(bannerBase64, Base64.NO_WRAP);
            FileOutputStream fos = new FileOutputStream(bannerFile);
            fos.write(bannerBytes);
            fos.close();
        }

        File introFile = new File(storyFolder, "gioi_thieu.txt");
        FileOutputStream fos = new FileOutputStream(introFile);
        fos.write(description.getBytes("UTF-8"));
        fos.close();

        File chaptersFolder = new File(storyFolder, "Chapters");
        chaptersFolder.mkdirs();

        for (int i = 0; i < chapterInputs.size(); i++) {
            EditText etContent = chapterInputs.get(i);
            LinearLayout parent = (LinearLayout) etContent.getParent();
            EditText etTitle = (EditText) parent.getChildAt(1);
            String chapterTitle = etTitle.getText().toString().trim();
            if (chapterTitle.isEmpty()) {
                chapterTitle = "Chương " + (i + 1);
            }
            
            String chapterFileName = "chuong_" + (i + 1) + ".txt";
            File chapterFile = new File(chaptersFolder, chapterFileName);
            FileOutputStream chFos = new FileOutputStream(chapterFile);
            
            String content = chapterTitle + "\n\n" + etContent.getText().toString();
            chFos.write(content.getBytes("UTF-8"));
            chFos.close();
        }

        JSONObject meta = new JSONObject();
        meta.put("id", storyId);
        meta.put("folder_name", folderName);
        meta.put("title", title);
        meta.put("author", author);
        meta.put("category", category);
        meta.put("rating", 0.0);
        meta.put("total_reviews", 0);
        meta.put("total_chapters", chapterInputs.size());
        meta.put("status", "Đang cập nhật");
        meta.put("views", 0);
        meta.put("translator_email", translatorEmail);

        File metaFile = new File(storyFolder, "metadata.json");
        FileOutputStream metaFos = new FileOutputStream(metaFile);
        metaFos.write(meta.toString().getBytes("UTF-8"));
        metaFos.close();
        
        return folderName;
    }
}