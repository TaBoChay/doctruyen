package com.example.doctruyen.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

public class EditStoryActivity extends AppCompatActivity {

    private EditText etTitle, etAuthor, etDescription, etFolderName;
    private Spinner spinnerCategory;
    private Button btnSave, btnPickCover, btnPickBanner;
    private ImageView ivCover, ivBanner;
    private ImageView btnBack;
    private LinearLayout layoutChapters;
    private LinearLayout layoutChapterSummary;
    private TextView tvChapterSummary;
    private TextView tvAddChapter;
    private TextView tvChapterList;
    private SharedPrefsHelper prefsHelper;
    private StoryManager storyManager;
    private Story editingStory;
    private int existingChapterCount = 0;

    private String coverBase64 = "";
    private String bannerBase64 = "";
    private ArrayList<EditText> chapterInputs = new ArrayList<>();
    private String pendingChapterContent = "";
    private Uri pendingChapterUri = null;

    private static final int PICK_COVER_REQUEST = 1;
    private static final int PICK_BANNER_REQUEST = 2;
    private static final int PICK_CHAPTER_FILE_REQUEST = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_story);

        prefsHelper = SharedPrefsHelper.getInstance(this);
        storyManager = StoryManager.getInstance(this);

        if (!prefsHelper.isTranslator()) {
            Toast.makeText(this, "Bạn cần đăng nhập với tài khoản dịch giả để đăng truyện.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Check if we're editing an existing story
        String storyId = getIntent().getStringExtra("story_id");
        if (storyId != null) {
            editingStory = storyManager.getStoryById(storyId);
            if (editingStory == null) {
                Toast.makeText(this, "Không tìm thấy truyện để chỉnh sửa.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        initViews();
        setupCategorySpinner();
        setupListeners();
        
        // If editing, load existing data
        if (editingStory != null) {
            loadStoryData();
        }
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_story_title);
        etAuthor = findViewById(R.id.et_story_author);
        etDescription = findViewById(R.id.et_story_description);
        etFolderName = findViewById(R.id.et_folder_name);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSave = findViewById(R.id.btn_save_story);
        btnPickCover = findViewById(R.id.btn_pick_cover);
        btnPickBanner = findViewById(R.id.btn_pick_banner);
        ivCover = findViewById(R.id.iv_cover);
        ivBanner = findViewById(R.id.iv_banner);
        btnBack = findViewById(R.id.btn_back);
        layoutChapters = findViewById(R.id.layout_chapters);
        
        layoutChapterSummary = findViewById(R.id.layout_chapter_summary);
        tvChapterSummary = findViewById(R.id.tv_chapter_summary);
        tvAddChapter = findViewById(R.id.tv_add_chapter);
        tvChapterList = findViewById(R.id.tv_chapter_list);
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
        
        tvAddChapter.setOnClickListener(v -> showAddChapterDialog());
        
        tvChapterList.setOnClickListener(v -> {
            if (editingStory != null) {
                Intent intent = new Intent(this, ManageChaptersActivity.class);
                intent.putExtra(ManageChaptersActivity.EXTRA_STORY_ID, editingStory.getId());
                startActivity(intent);
            }
        });

        btnSave.setOnClickListener(v -> saveStory());
    }
    
    private void showAddChapterDialog() {
        pendingChapterContent = "";
        pendingChapterUri = null;
        
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_chapter_single, null);
        
        EditText etChapterTitle = dialogView.findViewById(R.id.et_chapter_title);
        Button btnPickFile = dialogView.findViewById(R.id.btn_pick_chapter_file);
        TextView tvSelectedFile = dialogView.findViewById(R.id.tv_selected_file);
        
        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/plain");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Chọn file .txt"), PICK_CHAPTER_FILE_REQUEST);
        });
        
        int newChapterNum = existingChapterCount + 1;
        etChapterTitle.setHint("Tiêu đề chương " + newChapterNum);
        
        final EditText finalEtChapterTitle = etChapterTitle;
        
        new AlertDialog.Builder(this)
                .setTitle("Thêm chương mới")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String title = finalEtChapterTitle.getText().toString().trim();
                    String content = pendingChapterContent;
                    
                    if (content.isEmpty()) {
                        Toast.makeText(this, "Vui lòng chọn file .txt", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (title.isEmpty()) {
                        title = "Chương " + newChapterNum;
                    }
                    
                    addSingleChapter(title, content);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void addSingleChapter(String title, String content) {
        if (editingStory == null) {
            Toast.makeText(this, "Không thể thêm chương: không tìm thấy truyện", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            String id = editingStory.getId();
            int newChapterNum = existingChapterCount + 1;
            
            Chapter newChapter = new Chapter(
                    id + "_" + newChapterNum,
                    id,
                    title,
                    newChapterNum,
                    ""
            );
            newChapter.setContent(content);
            
            // Add to story
            editingStory.getChapters().add(newChapter);
            
            // Save to file
            File storiesDir = new File(getFilesDir(), "Data");
            File storyFolder = new File(storiesDir, editingStory.getFolderName());
            File chaptersFolder = new File(storyFolder, "Chapters");
            if (!chaptersFolder.exists()) {
                chaptersFolder.mkdirs();
            }
            
            String chapterFileName = "chuong_" + newChapterNum + ".txt";
            File chapterFile = new File(chaptersFolder, chapterFileName);
            FileOutputStream chFos = new FileOutputStream(chapterFile);
            String fileContent = title + "\n\n" + content;
            chFos.write(fileContent.getBytes("UTF-8"));
            chFos.close();
            
            // Update metadata
            existingChapterCount++;
            tvChapterSummary.setText("Đã có " + existingChapterCount + " chương");
            
            Toast.makeText(this, "Đã thêm chương " + newChapterNum, Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi lưu chương: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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
            else if (requestCode == PICK_CHAPTER_FILE_REQUEST) {
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
                    
                    pendingChapterContent = content.toString();
                    pendingChapterUri = selectedUri;
                    
                    Toast.makeText(this, "Đã chọn file: " + getFileName(selectedUri), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Lỗi đọc file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            // Legacy: chapter file upload from add story form
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
        addChapterInput(null);
    }
    
    private void addChapterInput(Chapter existingChapter) {
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
        
        // Set existing chapter title if available
        if (existingChapter != null) {
            etChapterTitle.setText(existingChapter.getTitle());
        }

        // EditText for chapter content (will be hidden when using file upload)
        EditText etChapterContent = new EditText(this);
        etChapterContent.setHint("Nội dung chương " + chapterNum + "...");
        etChapterContent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        etChapterContent.setMinLines(5);
        etChapterContent.setBackgroundResource(R.drawable.bg_input);
        etChapterContent.setPadding(32, 24, 32, 24);
        
        // Set existing chapter content if available
        if (existingChapter != null) {
            etChapterContent.setText(existingChapter.getContent());
        }

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

    private void loadStoryData() {
        etTitle.setText(editingStory.getTitle());
        etAuthor.setText(editingStory.getAuthor());
        etDescription.setText(editingStory.getDescription());
        etFolderName.setText(editingStory.getFolderName());
        etFolderName.setEnabled(false); // Do not allow changing folder name
        
        String bannerPath = editingStory.getBannerImagePath();
        long bannerLastModified = 0;
        if (bannerPath != null && bannerPath.startsWith("file:")) {
            bannerLastModified = new java.io.File(bannerPath.substring(5)).lastModified();
        }
        Uri bannerUri = (bannerPath != null && bannerPath.startsWith("file:")) ? 
                Uri.parse(bannerPath) : Uri.parse("file:///android_asset/" + bannerPath);

        com.bumptech.glide.Glide.with(this)
                .load(bannerUri)
                .signature(new com.bumptech.glide.signature.ObjectKey(String.valueOf(bannerLastModified)))
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(ivBanner);

        String coverPath = editingStory.getCoverImagePath();
        long coverLastModified = 0;
        if (coverPath != null && coverPath.startsWith("file:")) {
            coverLastModified = new java.io.File(coverPath.substring(5)).lastModified();
        }
        Uri coverUri = (coverPath != null && coverPath.startsWith("file:")) ? 
                Uri.parse(coverPath) : Uri.parse("file:///android_asset/" + coverPath);

        com.bumptech.glide.Glide.with(this)
                .load(coverUri)
                .signature(new com.bumptech.glide.signature.ObjectKey(String.valueOf(coverLastModified)))
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(ivCover);
        
        // Set category
        String[] categories = {"Tiên Hiệp", "Ngôn Tình", "Kiếm Hiệp", "Kỳ Ảo", "Trinh Thám", "Huyền Huyễn", "Võng Du", "Đô Thị", "Khác"};
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(editingStory.getCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
        
        // Show chapter summary
        ArrayList<Chapter> chapters = editingStory.getChapters();
        existingChapterCount = chapters != null ? chapters.size() : 0;
        
        tvChapterSummary.setText("Đã có " + existingChapterCount + " chương");
        
        // layoutChapters is for adding new chapters, hide it when editing
        layoutChapters.setVisibility(View.GONE);
        
        // Update button text
        btnSave.setText("CẬP NHẬT TRUYỆN");
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

        // For new stories, require at least 1 chapter
        // For editing, chapter is optional (existing chapters are preserved)
        boolean isNewStory = (editingStory == null);
        if (chapterInputs.isEmpty() && isNewStory) {
            Toast.makeText(this, "Vui lòng thêm ít nhất 1 chương.", Toast.LENGTH_SHORT).show();
            return;
        }

        String id;
        String translatorEmail = prefsHelper.getUserEmail();
        boolean isEditing = (editingStory != null);

        try {
            if (isEditing) {
                id = editingStory.getId();
                // Use existing folder name for editing
                folderName = editingStory.getFolderName();
                
                // Update existing story
                editingStory.setTitle(title);
                editingStory.setAuthor(author);
                editingStory.setDescription(description);
                editingStory.setCategory(category);
                editingStory.setTranslatorEmail(translatorEmail);
                
                // Only update chapters if there are new chapter inputs (from the form)
                // Existing chapters are already in editingStory.getChapters()
                if (!chapterInputs.isEmpty()) {
                    ArrayList<Chapter> chapters = editingStory.getChapters();
                    if (chapters == null) {
                        chapters = new ArrayList<>();
                    }
                    
                    int startNum = chapters.size() + 1;
                    for (int i = 0; i < chapterInputs.size(); i++) {
                        EditText etContent = chapterInputs.get(i);
                        LinearLayout parent = (LinearLayout) etContent.getParent();
                        EditText etTitle = (EditText) parent.getChildAt(1);
                        String chapterTitle = etTitle.getText().toString().trim();
                        if (chapterTitle.isEmpty()) {
                            chapterTitle = "Chương " + (startNum + i);
                        }
                        
                        Chapter chapter = new Chapter(
                                id + "_" + (startNum + i),
                                id,
                                chapterTitle,
                                startNum + i,
                                ""
                        );
                        chapter.setContent(etContent.getText().toString());
                        chapters.add(chapter);
                    }
                    editingStory.setChapters(chapters);
                }
                
                // Save to local storage
                updateLocalStorage(editingStory, folderName, title, author, category, description, translatorEmail);
                
                Toast.makeText(this, "Cập nhật truyện thành công!", Toast.LENGTH_LONG).show();
            } else {
                id = UUID.randomUUID().toString();
                
                // Save new story
                saveToLocalStorage(id, folderName, title, author, category, description, translatorEmail);
                
                Story newStory = new Story(id, title, author, description, "", "", category, folderName);
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
                    Chapter chapter = new Chapter(
                            id + "_" + (i + 1),
                            id,
                            chapterTitle,
                            i + 1,
                            ""
                    );
                    chapter.setContent(etContent.getText().toString());
                    chapters.add(chapter);
                }
                newStory.setChapters(chapters);

                storyManager.addStory(newStory);

                Toast.makeText(this, "Đăng truyện thành công! Đã tạo " + chapterInputs.size() + " chương.", Toast.LENGTH_LONG).show();
            }
            
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi lưu truyện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void updateLocalStorage(Story story, String folderName, String title, String author, 
                          String category, String description, String translatorEmail) throws Exception {
        
        File storiesDir = new File(getFilesDir(), "Data");
        File storyFolder = new File(storiesDir, folderName);
        
        // Update metadata
        JSONObject meta = new JSONObject();
        meta.put("id", story.getId());
        meta.put("folder_name", folderName);
        meta.put("title", title);
        meta.put("author", author);
        meta.put("category", category);
        meta.put("rating", story.getRating());
        meta.put("total_reviews", story.getTotalReviews());
        meta.put("total_chapters", story.getChapters().size());
        meta.put("status", "Đang cập nhật");
        meta.put("views", story.getViews());
        meta.put("translator_email", translatorEmail);

        File metaFile = new File(storyFolder, "metadata.json");
        FileOutputStream metaFos = new FileOutputStream(metaFile);
        metaFos.write(meta.toString().getBytes("UTF-8"));
        metaFos.close();
        
        // Update cover if changed
        if (!coverBase64.isEmpty()) {
            File coverFile = new File(storyFolder, "bia.jpg");
            byte[] coverBytes = Base64.decode(coverBase64, Base64.NO_WRAP);
            FileOutputStream fos = new FileOutputStream(coverFile);
            fos.write(coverBytes);
            fos.close();
        }

        // Update banner if changed
        if (!bannerBase64.isEmpty()) {
            File bannerFile = new File(storyFolder, "banner.jpg");
            byte[] bannerBytes = Base64.decode(bannerBase64, Base64.NO_WRAP);
            FileOutputStream fos = new FileOutputStream(bannerFile);
            fos.write(bannerBytes);
            fos.close();
        }

        // Update description
        File introFile = new File(storyFolder, "gioi_thieu.txt");
        FileOutputStream fos = new FileOutputStream(introFile);
        fos.write(description.getBytes("UTF-8"));
        fos.close();

        // Update chapters
        File chaptersFolder = new File(storyFolder, "Chapters");
        if (!chaptersFolder.exists()) {
            chaptersFolder.mkdirs();
        }

        // Clear existing chapter files
        File[] chapterFiles = chaptersFolder.listFiles();
        if (chapterFiles != null) {
            for (File file : chapterFiles) {
                file.delete();
            }
        }

        // Write updated chapters
        ArrayList<Chapter> chapters = story.getChapters();
        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            String chapterFileName = "chuong_" + (i + 1) + ".txt";
            File chapterFile = new File(chaptersFolder, chapterFileName);
            FileOutputStream chFos = new FileOutputStream(chapterFile);
            
            String content = chapter.getTitle() + "\n\n" + chapter.getContent();
            chFos.write(content.getBytes("UTF-8"));
            chFos.close();
        }
    }

    private void saveToLocalStorage(String storyId, String folderName, String title, String author, 
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
    }
}