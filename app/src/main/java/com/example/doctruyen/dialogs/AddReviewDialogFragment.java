package com.example.doctruyen.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.doctruyen.R;
// Xóa import thừa và chuẩn hóa import
import com.example.doctruyen.activities.OnDataSubmittedListener;
import com.example.doctruyen.models.Review;
import com.example.doctruyen.utils.StoryManager;
import com.example.doctruyen.utils.SharedPrefsHelper; // <-- THÊM IMPORT

import java.util.UUID;

public class AddReviewDialogFragment extends DialogFragment {

    private static final String ARG_STORY_ID = "storyId";
    private String storyId;
    private OnDataSubmittedListener listener;

    public static AddReviewDialogFragment newInstance(String storyId) {
        AddReviewDialogFragment fragment = new AddReviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            storyId = getArguments().getString(ARG_STORY_ID);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Kiểm tra Context phải implements interface OnDataSubmittedListener
        if (context instanceof OnDataSubmittedListener) {
            listener = (OnDataSubmittedListener) context;
        } else {
            // Sửa thông báo lỗi để chỉ ra interface đúng
            throw new ClassCastException(context.toString() + " must implement OnDataSubmittedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_review, null);

        final RatingBar ratingBar = view.findViewById(R.id.rating_input_bar);
        final EditText etContent = view.findViewById(R.id.et_review_content);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Thêm Đánh giá");
        builder.setView(view);

        builder.setPositiveButton("Gửi", null);
        builder.setNegativeButton("Hủy", (dialog, id) -> dialog.dismiss());

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                float rating = ratingBar.getRating();
                String content = etContent.getText().toString().trim();

                if (rating < 0.5f || content.isEmpty()) {
                    Toast.makeText(getContext(), "Vui lòng chọn điểm và nhập nội dung.", Toast.LENGTH_SHORT).show();
                    return;
                }

                /// LẤY THÔNG TIN USER ĐANG ĐĂNG NHẬP
                SharedPrefsHelper prefs = SharedPrefsHelper.getInstance(getContext());
                String currentUserId = prefs.getLoggedInEmail(); // Sử dụng Email làm ID
                String currentUserName = prefs.getUserName();    // Lấy tên người dùng thật

                // KIỂM TRA: user này đã đánh giá truyện này chưa?
                if (StoryManager.getInstance(getContext()).hasUserReviewed(storyId, currentUserId)) {
                    Toast.makeText(getContext(), "Bạn đã đánh giá truyện này rồi.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Tạo đối tượng Review mới
                Review newReview = new Review(
                        UUID.randomUUID().toString(),
                        storyId,
                        currentUserId,
                        currentUserName,
                        rating,
                        content
                );

                StoryManager.getInstance(getContext()).addReview(newReview, storyId);


                if (listener != null) {
                    listener.onReviewSubmitted();
                }
                Toast.makeText(getContext(), "Đánh giá đã được gửi!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        return dialog;
    }
}