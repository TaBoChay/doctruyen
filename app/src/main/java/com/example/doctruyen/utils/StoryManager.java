package com.example.doctruyen.utils;
import com.example.doctruyen.utils.SharedPrefsHelper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.doctruyen.models.Chapter;
import com.example.doctruyen.models.Story;
import com.example.doctruyen.models.Review;
import com.example.doctruyen.models.Comment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.doctruyen.data.AppDatabase;
import com.example.doctruyen.data.ReviewEntity;
import com.example.doctruyen.data.CommentEntity;
import com.example.doctruyen.data.LibraryEntry;
import com.example.doctruyen.data.ReadChapterEntity;

public class StoryManager {

    private static final String TAG = "StoryManager";
    private static StoryManager instance;
    private ArrayList<Story> allStories;
    private final Context context;
    private boolean isLoaded = false;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final AppDatabase db;

    // Interface để thông báo khi dữ liệu tải xong
    public interface StoryLoadListener {
        void onStoriesLoaded();
        void onStoriesLoadFailed(String error);
    }
    
    public interface CommentCallback {
        void onCommentsLoaded(List<Comment> comments);
    }

    private StoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.allStories = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.db = AppDatabase.getInstance(this.context);
    }
    private String getCurrentUserId() {
        SharedPrefsHelper prefs = SharedPrefsHelper.getInstance(context);
        String email = prefs.getLoggedInEmail();
        return email != null ? email : "";
    }

    public static synchronized StoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new StoryManager(context);
        }
        return instance;
    }
    public void refreshLibraryForCurrentUser() {
        executor.execute(() -> {
            String userId = getCurrentUserId();

            for (Story story : allStories) {
                boolean inLib = false;
                if (userId != null && !userId.isEmpty()) {
                    inLib = db.libraryDao().isInLibrary(story.getId(), userId);
                }

                boolean finalInLib = inLib;
                mainHandler.post(() -> {
                    story.setInLibrary(finalInLib);
                });
            }

            Log.d(TAG, "Refreshed library state for user: " + userId);
        });
    }

    public void loadStoriesAsync(StoryLoadListener listener) {
        if (isLoaded) {
            listener.onStoriesLoaded();
            refreshLibraryForCurrentUser();
            return;
        }

        executor.execute(() -> {
            try {
                loadStoriesFromAssets();
                loadStoriesFromLocalStorage();

                mainHandler.post(() -> {
                    isLoaded = true;
                    listener.onStoriesLoaded();
                    refreshLibraryForCurrentUser();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading stories: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    listener.onStoriesLoadFailed(e.getMessage());
                });
            }
        });
    }

    private void loadStoriesFromLocalStorage() {
        File dataDir = new File(context.getFilesDir(), "Data");
        if (!dataDir.exists()) {
            Log.d(TAG, "Local Data folder does not exist");
            return;
        }

        File[] storyFolders = dataDir.listFiles(File::isDirectory);
        if (storyFolders == null || storyFolders.length == 0) {
            Log.d(TAG, "No local stories found");
            return;
        }

        Log.d(TAG, "Found " + storyFolders.length + " local story folders");

        for (File folder : storyFolders) {
            try {
                File metaFile = new File(folder, "metadata.json");
                if (!metaFile.exists()) {
                    Log.w(TAG, "No metadata.json in folder: " + folder.getName());
                    continue;
                }

                String jsonContent = readFile(metaFile);
                JSONObject metaObj = new JSONObject(jsonContent);

                String id = metaObj.getString("id");
                String title = metaObj.getString("title");
                String author = metaObj.getString("author");
                String category = metaObj.optString("category", "Khác");
                String folderName = metaObj.getString("folder_name");
                float rating = (float) metaObj.optDouble("rating", 0.0);
                int totalReviews = metaObj.optInt("total_reviews", 0);
                int views = metaObj.optInt("views", 0);
                String translatorEmail = metaObj.optString("translator_email", "");

                String description = "";
                File introFile = new File(folder, "gioi_thieu.txt");
                if (introFile.exists()) {
                    description = readFile(introFile);
                }

                String coverPath = "file:" + folder.getAbsolutePath() + "/bia.jpg";
                String bannerPath = "file:" + folder.getAbsolutePath() + "/banner.jpg";

                Story story = new Story(id, title, author, description, coverPath, bannerPath, category, folderName);
                story.setRating(rating);
                story.setTotalReviews(totalReviews);
                story.setViews(views);
                story.setTranslatorEmail(translatorEmail);

                ArrayList<Chapter> chapters = new ArrayList<>();
                File chaptersFolder = new File(folder, "Chapters");
                if (chaptersFolder.exists() && chaptersFolder.isDirectory()) {
                    File[] chapterFiles = chaptersFolder.listFiles((dir, name) -> name.endsWith(".txt"));
                    if (chapterFiles != null) {
                        for (File chapterFile : chapterFiles) {
                            String chapterContent = readFile(chapterFile);
                            String chapterTitle = "";
                            String actualContent = chapterContent;
                            
                            int newlineIndex = chapterContent.indexOf("\n");
                            if (newlineIndex > 0) {
                                chapterTitle = chapterContent.substring(0, newlineIndex).trim();
                                actualContent = chapterContent.substring(newlineIndex + 1).trim();
                            } else if (newlineIndex == 0) {
                                actualContent = chapterContent.substring(1).trim();
                            }

                            String fileName = chapterFile.getName();
                            int chapterNum = 1;
                            try {
                                String numStr = fileName.replace("chuong_", "").replace(".txt", "");
                                chapterNum = Integer.parseInt(numStr);
                            } catch (Exception e) {
                                chapterNum = chapters.size() + 1;
                            }

                            Chapter chapter = new Chapter(
                                    id + "_" + chapterNum,
                                    id,
                                    chapterTitle.isEmpty() ? "Chương " + chapterNum : chapterTitle,
                                    chapterNum,
                                    "file:" + chapterFile.getAbsolutePath()
                            );
                            chapter.setContent(actualContent);
                            chapters.add(chapter);
                        }
                        Collections.sort(chapters, (c1, c2) -> Integer.compare(c1.getChapterNumber(), c2.getChapterNumber()));
                    }
                }
                story.setChapters(chapters);

                boolean alreadyExists = false;
                for (Story s : allStories) {
                    if (s.getId().equals(id)) {
                        alreadyExists = true;
                        break;
                    }
                }

                if (!alreadyExists) {
                    allStories.add(story);
                    Log.d(TAG, "Loaded local story: " + title + " with " + chapters.size() + " chapters");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading local story from " + folder.getName() + ": " + e.getMessage());
            }
        }
    }

    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString().trim();
    }

    // PHƯƠNG THỨC HỖ TRỢ CHỨC NĂNG TỦ TRUYỆN
    public void removeStoriesFromLibrary(List<Story> storiesToRemove) {
        if (storiesToRemove == null || storiesToRemove.isEmpty()) {
            return;
        }

        String userId = getCurrentUserId();
        if (userId.isEmpty()) {
            Log.w(TAG, "removeStoriesFromLibrary: Không có user đang đăng nhập.");
            return;
        }

        List<String> ids = new ArrayList<>();

        for (Story storyToRemove : storiesToRemove) {
            for (Story story : allStories) {
                if (story.getId().equals(storyToRemove.getId())) {
                    story.setInLibrary(false);
                    ids.add(story.getId());
                    break;
                }
            }
        }

        executor.execute(() -> {
            db.libraryDao().deleteStories(ids, userId);
        });

        Log.d(TAG, "Removed " + storiesToRemove.size() + " stories from library for user: " + userId);
    }



    // PHƯƠNG THỨC MỚI: THÊM VÀO TỦ TRUYỆN
    public void addToLibrary(Story storyToSave) {
        String userId = getCurrentUserId();
        if (userId.isEmpty()) {
            Log.w(TAG, "addToLibrary: Không có user đang đăng nhập.");
            return;
        }

        for (Story story : allStories) {
            if (story.getId().equals(storyToSave.getId())) {
                story.setInLibrary(true);

                executor.execute(() -> {
                    LibraryEntry entry = new LibraryEntry();
                    entry.id = story.getId() + "_" + userId; // KEY duy nhất
                    entry.storyId = story.getId();
                    entry.userId = userId;
                    entry.inLibrary = true;
                    db.libraryDao().insert(entry);
                });
                break;
            }
        }
    }



    // ----------------------------------------------------
    // --- CHỨC NĂNG BÌNH LUẬN VÀ ĐÁNH GIÁ MỚI ---
    // ----------------------------------------------------

    /** Thêm một đánh giá mới vào truyện và lưu trữ dữ liệu. */
    public void addReview(Review newReview, String storyId) {
        Story story = getStoryById(storyId);
        if (story != null) {
            story.getReviews().add(0, newReview);
            updateStoryRating(storyId);
            executor.execute(() -> {
                ReviewEntity entity = new ReviewEntity();
                entity.id = newReview.getId();
                entity.storyId = newReview.getStoryId();
                entity.userId = newReview.getUserId();
                entity.userName = newReview.getUserName();
                entity.rating = newReview.getRating();
                entity.comment = newReview.getComment();
                entity.timestamp = newReview.getTimestamp();
                db.reviewDao().insertReview(entity);
            });
        }
    }

    /** Thêm một bình luận mới vào truyện và lưu trữ dữ liệu. */
    public void addComment(Comment newComment, String storyId) {
        Story story = getStoryById(storyId);
        if (story != null) {
            story.getComments().add(0, newComment);
            executor.execute(() -> {
                CommentEntity entity = new CommentEntity();
                entity.id = newComment.getId();
                entity.storyId = newComment.getStoryId();
                entity.userId = newComment.getUserId();
                entity.userName = newComment.getUserName();
                entity.content = newComment.getContent();
                entity.timestamp = newComment.getTimestamp();
                db.commentDao().insertComment(entity);
            });
        }
    }

    public void addChapterComment(Comment newComment) {
        executor.execute(() -> {
            CommentEntity entity = new CommentEntity();
            entity.id = newComment.getId();
            entity.storyId = newComment.getStoryId(); // this stores chapterId
            entity.userId = newComment.getUserId();
            entity.userName = newComment.getUserName();
            entity.content = newComment.getContent();
            entity.timestamp = newComment.getTimestamp();
            db.commentDao().insertComment(entity);
        });
    }

    public void getChapterComments(String chapterId, CommentCallback callback) {
        executor.execute(() -> {
            List<CommentEntity> localComments = db.commentDao().getCommentsForStory(chapterId);
            List<Comment> chapterComments = new ArrayList<>();
            for (CommentEntity ce : localComments) {
                Comment c = new Comment(ce.id, ce.storyId, ce.userId, ce.userName, ce.content, ce.timestamp);
                chapterComments.add(c);
            }
            mainHandler.post(() -> callback.onCommentsLoaded(chapterComments));
        });
    }

    /** Tính toán lại Rating trung bình của truyện sau khi có Review mới. */
    public void updateStoryRating(String storyId) {
        Story story = getStoryById(storyId);
        if (story != null) {
            List<Review> reviews = story.getReviews();
            if (reviews == null || reviews.isEmpty()) {
                story.setRating(0.0f);
                story.setTotalReviews(0);
                return;
            }

            float totalRating = 0.0f;
            for (Review review : reviews) {
                totalRating += review.getRating();
            }

            float newRating = totalRating / reviews.size();

            story.setRating(newRating);
            story.setTotalReviews(reviews.size());

            // TODO: Cần lưu trạng thái Rating mới này vào metadata (ví dụ: story_metadata.json)
            // saveMetadataToStorage(story);
        }
    }

    /** Phương thức giả định để lưu trữ Review mới (Cần triển khai thực tế). */
    private void saveReviewToStorage(Review review) {
        // TODO: Triển khai logic ghi Review vào file reviews.json (sau khi chuyển đổi Review sang JSON)
        Log.d(TAG, "Review saved locally: " + review.getId());
    }

    /** Phương thức giả định để lưu trữ Comment mới (Cần triển khai thực tế). */
    private void saveCommentToStorage(Comment comment) {
        // TODO: Triển khai logic ghi Comment vào file comments.json (sau khi chuyển đổi Comment sang JSON)
        Log.d(TAG, "Comment saved locally: " + comment.getId());
    }
    // ----------------------------------------------------


    // ----------------------------------------------------
    // --- CÁC PHƯƠNG THỨC KHÁC ---
    // ----------------------------------------------------
    public boolean hasUserReviewed(String storyId, String userId) {
        Story story = getStoryById(storyId);
        if (story == null) return false;

        List<Review> reviews = story.getReviews();
        if (reviews == null) return false;

        for (Review r : reviews) {
            if (userId != null && userId.equals(r.getUserId())) {
                return true;
            }
        }
        return false;
    }

    private void saveLibraryState(String storyId, boolean isInLibrary) {
        // TODO: Triển khai lưu trạng thái (SharedPreferences/Database) tại đây
        Log.d(TAG, "Saving library state for " + storyId + ": " + isInLibrary);
    }

    private boolean loadLibraryState(String storyId) {
        String userId = getCurrentUserId();
        if (userId.isEmpty()) return false;
        return db.libraryDao().isInLibrary(storyId, userId);
    }



    private void loadStoriesFromAssets() throws IOException, JSONException {
        JSONObject metadataRoot = new JSONObject(readAssetFile("Data/story_metadata.json"));
        JSONArray metadataArray = metadataRoot.optJSONArray("stories");
        if (metadataArray == null) {
            Log.e(TAG, "Metadata JSON file is missing 'stories' array.");
            throw new JSONException("Metadata JSON missing 'stories' array.");
        }

        JSONObject reviewsRoot = new JSONObject(readAssetFile("Data/reviews.json"));
        JSONArray reviewsArray = reviewsRoot.optJSONArray("reviews");
        if (reviewsArray == null) reviewsArray = new JSONArray();

        JSONObject commentsRoot = new JSONObject(readAssetFile("Data/comments.json"));
        JSONArray commentsArray = commentsRoot.optJSONArray("comments");
        if (commentsArray == null) commentsArray = new JSONArray();

        Log.d(TAG, "Loaded " + metadataArray.length() + " metadata entries.");

        String[] storyFolders = context.getAssets().list("Data");
        if (storyFolders == null || storyFolders.length == 0) {
            Log.e(TAG, "The 'assets/Data' folder is empty or not found.");
            return;
        }

        allStories.clear();

        for (String folderName : storyFolders) {
            if (folderName.endsWith(".json")) continue;
            String basePath = "Data/" + folderName;

            JSONObject metaObj = null;
            for (int i = 0; i < metadataArray.length(); i++) {
                JSONObject obj = metadataArray.getJSONObject(i);
                if (obj.has("folder_name") && obj.getString("folder_name").equals(folderName)) {
                    metaObj = obj;
                    break;
                }
            }
            if (metaObj == null) {
                Log.w(TAG, "Metadata not found for folder: " + folderName);
                continue;
            }

            // --- 3. Tạo đối tượng Story ---
            String id = metaObj.getString("id");
            String title = metaObj.getString("title");
            String author = metaObj.getString("author");
            String category = metaObj.getString("category");
            float rating = (float) metaObj.optDouble("rating", 0.0);
            int totalReviews = metaObj.optInt("total_reviews", 0);
            int views = metaObj.optInt("views", 0);

            String description = readOptionalAssetFile(basePath + "/gioi_thieu.txt", "Đang cập nhật nội dung giới thiệu...");
            String coverImage = basePath + "/bia.jpg";
            String bannerImage = basePath + "/banner.jpg";

            Story story = new Story(id, title, author, description, coverImage, bannerImage, category, folderName);
            story.setRating(rating);
            story.setTotalReviews(totalReviews);
            story.setViews(views);

            allStories.add(story);


            // --- 4. Chapters ---
            ArrayList<Chapter> chapters = new ArrayList<>();
            try {
                String[] chapterFiles = context.getAssets().list(basePath + "/Chapters");
                if (chapterFiles != null) {
                    for (int i = 0; i < chapterFiles.length; i++) {
                        String chapterFile = chapterFiles[i];
                        Chapter chapter = new Chapter(
                                id + "_" + (i + 1),
                                story.getId(),
                                chapterFile.replace(".txt", ""),
                                i + 1,
                                basePath + "/Chapters/" + chapterFile
                        );
                        chapters.add(chapter);
                    }
                    Collections.sort(chapters, (c1, c2) -> Integer.compare(c1.getChapterNumber(), c2.getChapterNumber()));
                }
            } catch (IOException e) {
                Log.w(TAG, "Chapters folder not found for story: " + folderName);
            }
            story.setChapters(chapters);

            // --- 5. Reviews ---
            List<Review> storyReviews = new ArrayList<>();
            for (int i = 0; i < reviewsArray.length(); i++) {
                JSONObject obj = reviewsArray.getJSONObject(i);
                if (obj.has("storyId") && obj.getString("storyId").equals(id)) {
                    Review review = new Review(
                            obj.getString("id"),
                            obj.getString("storyId"),
                            obj.getString("userId"),
                            obj.getString("userName"),
                            (float)obj.getDouble("rating"),
                            obj.getString("comment")
                    );
                    storyReviews.add(review);
                }
            }
            story.setReviews(storyReviews);
            List<ReviewEntity> localReviews = db.reviewDao().getReviewsForStory(id);
            for (ReviewEntity re : localReviews) {
                Review r = new Review(
                        re.id,
                        re.storyId,
                        re.userId,
                        re.userName,
                        re.rating,
                        re.comment,
                        re.timestamp
                );
                storyReviews.add(0, r); // add lên đầu
            }
            story.setReviews(storyReviews);
            // --- 6. Comments ---
            List<Comment> storyComments = new ArrayList<>();
            for (int i = 0; i < commentsArray.length(); i++) {
                JSONObject obj = commentsArray.getJSONObject(i);
                if (obj.has("storyId") && obj.getString("storyId").equals(id)) {
                    Comment comment = new Comment(
                            obj.getString("id"),
                            obj.getString("storyId"),
                            obj.getString("userId"),
                            obj.getString("userName"),
                            obj.getString("content")
                    );
                    storyComments.add(comment);
                }
            }
            story.setComments(storyComments);
            List<CommentEntity> localComments = db.commentDao().getCommentsForStory(id);
            for (CommentEntity ce : localComments) {
                Comment c = new Comment(
                        ce.id,
                        ce.storyId,
                        ce.userId,
                        ce.userName,
                        ce.content,
                        ce.timestamp
                );
                storyComments.add(0, c);
            }
            story.setComments(storyComments);

        }
        Log.d(TAG, "Successfully loaded " + allStories.size() + " stories.");
    }
    public void markChapterRead(String chapterId, String storyId) {
        // Cập nhật DB trong background
        executor.execute(() -> {
            ReadChapterEntity entity = new ReadChapterEntity();
            entity.chapterId = chapterId;
            entity.storyId = storyId;
            entity.isRead = true;
            db.readChapterDao().insert(entity);
        });
    }

    public boolean isChapterRead(String chapterId) {
        // LƯU Ý: chỉ gọi hàm này từ background (vd: trong loadStoriesFromAssets)
        return db.readChapterDao().isChapterRead(chapterId);
    }

    private String readAssetFile(String filePath) throws IOException {
        try (InputStream is = context.getAssets().open(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString().trim();
        } catch (IOException e) {
            Log.e(TAG, "Could not read asset file: " + filePath, e);
            throw e;
        }
    }

    private String readOptionalAssetFile(String filePath, String defaultContent) {
        try {
            return readAssetFile(filePath);
        } catch (IOException e) {
            return defaultContent;
        }
    }

    public ArrayList<Story> getAllStories() {
        return new ArrayList<>(allStories);
    }

    public void addStory(Story story) {
        allStories.add(0, story);
        Log.d(TAG, "Story added: " + story.getTitle());
    }

    public ArrayList<Story> getStoriesInLibrary() {
        ArrayList<Story> libraryStories = new ArrayList<>();
        for (Story story : allStories) {
            if (story.isInLibrary()) {
                libraryStories.add(story);
            }
        }
        return libraryStories;
    }

    public ArrayList<Story> getStoriesByTranslator(String translatorEmail) {
        ArrayList<Story> translatorStories = new ArrayList<>();
        for (Story story : allStories) {
            if (translatorEmail.equals(story.getTranslatorEmail())) {
                translatorStories.add(story);
            }
        }
        return translatorStories;
    }

    public Story getStoryById(String id) {
        for (Story story : allStories) {
            if (story.getId().equals(id)) {
                return story;
            }
        }
        return null;
    }

    public ArrayList<Story> getTopRatedStories() {
        ArrayList<Story> sortedStories = new ArrayList<>(allStories);
        sortedStories.sort((s1, s2) -> Float.compare(s2.getRating(), s1.getRating()));
        return sortedStories;
    }

    public List<Review> getStoryReviews(String storyId) {
        Story story = getStoryById(storyId);
        if (story != null) return new ArrayList<>(story.getReviews());
        return new ArrayList<>();
    }

    public List<Comment> getStoryComments(String storyId) {
        Story story = getStoryById(storyId);
        if (story != null) return new ArrayList<>(story.getComments());
        return new ArrayList<>();
    }

    public String readChapterContent(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        if (filePath.startsWith("file:") || filePath.startsWith("/")) {
            return readLocalFile(filePath.replace("file:", ""));
        }

        try {
            return readAssetFile(filePath);
        } catch (IOException e) {
            Log.e(TAG, "Error reading chapter content: " + filePath, e);
            return "Không thể tải nội dung chương. Lỗi: " + e.getMessage();
        }
    }

    private String readLocalFile(String path) {
        try {
            StringBuilder content = new StringBuilder();
            java.io.File file = new java.io.File(path);
            if (!file.exists()) {
                return "File không tồn tại: " + path;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "Error reading local file: " + path, e);
            return "Lỗi đọc file: " + e.getMessage();
        }
    }

    public void addChapter(Chapter chapter) {
        File storiesDir = new File(context.getFilesDir(), "Data");
        for (Story story : allStories) {
            if (story.getId().equals(chapter.getStoryId())) {
                File storyFolder = new File(storiesDir, story.getFolderName());
                File chaptersFolder = new File(storyFolder, "Chapters");
                if (!chaptersFolder.exists()) {
                    chaptersFolder.mkdirs();
                }

                String chapterFileName = "chuong_" + chapter.getChapterNumber() + ".txt";
                File chapterFile = new File(chaptersFolder, chapterFileName);
                try {
                    FileOutputStream chFos = new FileOutputStream(chapterFile);
                    String content = chapter.getTitle() + "\n\n" + chapter.getContent();
                    chFos.write(content.getBytes("UTF-8"));
                    chFos.close();

                    chapter.setFilePath("file:" + chapterFile.getAbsolutePath());

                    if (story.getChapters() == null) {
                        story.setChapters(new ArrayList<>());
                    }
                    story.getChapters().add(chapter);

                    Log.d(TAG, "Added chapter: " + chapter.getTitle());
                } catch (Exception e) {
                    Log.e(TAG, "Error adding chapter: " + e.getMessage());
                }
                break;
            }
        }
    }

    public void updateChapter(Chapter chapter) {
        File storiesDir = new File(context.getFilesDir(), "Data");
        for (Story story : allStories) {
            if (story.getId().equals(chapter.getStoryId())) {
                File storyFolder = new File(storiesDir, story.getFolderName());
                File chaptersFolder = new File(storyFolder, "Chapters");

                String chapterFileName = "chuong_" + chapter.getChapterNumber() + ".txt";
                File chapterFile = new File(chaptersFolder, chapterFileName);
                try {
                    FileOutputStream chFos = new FileOutputStream(chapterFile);
                    String content = chapter.getTitle() + "\n\n" + chapter.getContent();
                    chFos.write(content.getBytes("UTF-8"));
                    chFos.close();

                    chapter.setFilePath("file:" + chapterFile.getAbsolutePath());

                    if (story.getChapters() != null) {
                        for (int i = 0; i < story.getChapters().size(); i++) {
                            if (story.getChapters().get(i).getId().equals(chapter.getId())) {
                                story.getChapters().set(i, chapter);
                                break;
                            }
                        }
                    }

                    Log.d(TAG, "Updated chapter: " + chapter.getTitle());
                } catch (Exception e) {
                    Log.e(TAG, "Error updating chapter: " + e.getMessage());
                }
                break;
            }
        }
    }

    public ArrayList<Chapter> getChaptersByStoryId(String storyId) {
        for (Story story : allStories) {
            if (story.getId().equals(storyId)) {
                return story.getChapters();
            }
        }
        return new ArrayList<>();
    }

    public boolean deleteLocalStory(String storyId) {
        Story storyToDelete = null;
        for (Story story : allStories) {
            if (story.getId().equals(storyId)) {
                storyToDelete = story;
                break;
            }
        }

        if (storyToDelete != null && storyToDelete.getFolderName() != null) {
            try {
                File storiesDir = new File(context.getFilesDir(), "Data");
                File storyFolder = new File(storiesDir, storyToDelete.getFolderName());

                if (storyFolder.exists()) {
                    deleteRecursive(storyFolder);
                }

                allStories.remove(storyToDelete);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error deleting local story: " + e.getMessage());
            }
        }
        return false;
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}