package com.example.doctruyen.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.doctruyen.R;
import com.example.doctruyen.activities.OnDataSubmittedListener;
import com.example.doctruyen.models.Comment;
import com.example.doctruyen.utils.StoryManager;
import com.example.doctruyen.utils.SharedPrefsHelper;

import java.util.UUID;

public class AddCommentDialogFragment extends DialogFragment {

    private static final String ARG_STORY_ID = "storyId";
    private String storyId;
    private OnDataSubmittedListener listener;

    public static AddCommentDialogFragment newInstance(String storyId) {
        AddCommentDialogFragment fragment = new AddCommentDialogFragment();
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
        if (context instanceof OnDataSubmittedListener) {
            listener = (OnDataSubmittedListener) context;
        } else {
            // Thông báo lỗi cần implement interface nào
            throw new ClassCastException(context.toString() + " must implement OnDataSubmittedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_comment, null);

        final EditText etContent = view.findViewById(R.id.et_comment_content);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Thêm Bình luận");
        builder.setView(view);

        builder.setPositiveButton("Gửi", null);
        builder.setNegativeButton("Hủy", (dialog, id) -> dialog.dismiss());

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String content = etContent.getText().toString().trim();

                if (content.isEmpty()) {
                    Toast.makeText(getContext(), "Bình luận không được để trống.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // LẤY THÔNG TIN USER ĐANG ĐĂNG NHẬP
                SharedPrefsHelper prefs = SharedPrefsHelper.getInstance(getContext());
                // FIX LỖI: Sử dụng getLoggedInEmail() làm ID (thay vì getUserId())
                String currentUserId = prefs.getLoggedInEmail();
                String currentUserName = prefs.getUserName();

                // Tạo đối tượng Comment mới
                Comment newComment = new Comment(
                        UUID.randomUUID().toString(),
                        storyId,
                        currentUserId,     // Gán ID người dùng thật (email)
                        currentUserName,   // GÁN TÊN USER THẬT
                        content
                );

                StoryManager.getInstance(getContext()).addComment(newComment, storyId);

                if (listener != null) {
                    listener.onCommentSubmitted();
                }
                Toast.makeText(getContext(), "Bình luận đã được gửi!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        return dialog;
    }
}