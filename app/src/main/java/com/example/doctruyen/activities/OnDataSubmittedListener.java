package com.example.doctruyen.activities;

/**
 * Interface Callback dùng để thông báo khi dữ liệu Review/Comment được gửi thành công.
 */
public interface OnDataSubmittedListener {
    void onReviewSubmitted();
    void onCommentSubmitted();
}