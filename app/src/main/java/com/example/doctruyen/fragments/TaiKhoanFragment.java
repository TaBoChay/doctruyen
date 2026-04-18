package com.example.doctruyen.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.doctruyen.R;
import com.example.doctruyen.activities.AddStoryActivity;
import com.example.doctruyen.activities.DangNhapActivity;
import com.example.doctruyen.activities.ManageStoriesActivity;
import com.example.doctruyen.utils.SharedPrefsHelper;

public class TaiKhoanFragment extends Fragment {

    private LinearLayout layoutLoggedIn, layoutNotLoggedIn, layoutTranslatorMenu;
    private TextView tvUserName, tvUserEmail;
    private Button btnLogin, btnRegister, btnLogout, btnChangePassword, btnAddStory, btnManageStories;
    private SharedPrefsHelper prefsHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tai_khoan, container, false);

        prefsHelper = SharedPrefsHelper.getInstance(getContext());

        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        layoutNotLoggedIn = view.findViewById(R.id.layout_not_logged_in);
        layoutTranslatorMenu = view.findViewById(R.id.layout_translator_menu);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        btnLogin = view.findViewById(R.id.btn_login);
        btnRegister = view.findViewById(R.id.btn_register);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        btnAddStory = view.findViewById(R.id.btn_add_story);
        btnManageStories = view.findViewById(R.id.btn_manage_stories);

        setupButtons();
        updateUI();

        return view;
    }

    private void setupButtons() {
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), DangNhapActivity.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), DangNhapActivity.class);
            intent.putExtra("showRegister", true);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            prefsHelper.setLoggedIn(false);
            updateUI();
            Toast.makeText(getContext(), "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
        });

        btnChangePassword.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        btnAddStory.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddStoryActivity.class);
            startActivity(intent);
        });

        btnManageStories.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ManageStoriesActivity.class);
            startActivity(intent);
        });
    }

    // --- PHƯƠNG THỨC HIỂN THỊ DIALOG ĐỔI MẬT KHẨU ---
    private void showChangePasswordDialog() {
        Context context = getContext();
        if (context == null) return;

        // LƯU Ý: Bạn cần tạo file Layout XML này: res/layout/dialog_change_password.xml
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_password, null);

        // Giả định ID từ dialog_change_password.xml
        EditText etCurrentPass = dialogView.findViewById(R.id.et_current_password);
        EditText etNewPass = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirmNewPass = dialogView.findViewById(R.id.et_confirm_new_password);

        new AlertDialog.Builder(context)
                .setTitle("Đổi Mật Khẩu")
                .setView(dialogView)
                .setPositiveButton("Xác Nhận", (dialog, which) -> {
                    String currentPass = etCurrentPass.getText().toString();
                    String newPass = etNewPass.getText().toString();
                    String confirmPass = etConfirmNewPass.getText().toString();

                    // 1. Kiểm tra Mật khẩu mới khớp
                    if (!newPass.equals(confirmPass)) {
                        Toast.makeText(context, "Mật khẩu mới không khớp.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 2. Kiểm tra độ dài
                    if (newPass.length() < 6) {
                        Toast.makeText(context, "Mật khẩu mới phải có ít nhất 6 ký tự.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 3. Gọi hàm đổi mật khẩu
                    if (prefsHelper.changePassword(currentPass, newPass)) {
                        Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Mật khẩu hiện tại không đúng.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    // ----------------------------------------------------------------------


    private void updateUI() {
        if (prefsHelper.isLoggedIn()) {
            layoutLoggedIn.setVisibility(View.VISIBLE);
            layoutNotLoggedIn.setVisibility(View.GONE);
            if (getContext() != null) {
                tvUserName.setText(prefsHelper.getUserName());
                tvUserEmail.setText(prefsHelper.getUserEmail());
            }

            if (prefsHelper.isTranslator()) {
                layoutTranslatorMenu.setVisibility(View.VISIBLE);
            } else {
                layoutTranslatorMenu.setVisibility(View.GONE);
            }
        } else {
            layoutLoggedIn.setVisibility(View.GONE);
            layoutNotLoggedIn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }
}