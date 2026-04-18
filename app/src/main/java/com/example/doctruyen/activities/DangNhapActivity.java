package com.example.doctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.doctruyen.utils.StoryManager;
import com.example.doctruyen.MainActivity;
import com.example.doctruyen.R;
import com.example.doctruyen.utils.SharedPrefsHelper;

public class DangNhapActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword, etName;
    private Button btnSubmit;
    private TextView tvToggle, tvTitle;
    private Spinner spinnerRole;
    private boolean isRegisterMode = false;
    private SharedPrefsHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dang_nhap);

        prefsHelper = SharedPrefsHelper.getInstance(this);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etName = findViewById(R.id.et_name);
        btnSubmit = findViewById(R.id.btn_submit);
        tvToggle = findViewById(R.id.tv_toggle);
        tvTitle = findViewById(R.id.tv_title);
        spinnerRole = findViewById(R.id.spinner_role);

        setupRoleSpinner();

        if (getIntent().getBooleanExtra("showRegister", false)) {
            toggleMode();
        }

        btnSubmit.setOnClickListener(v -> handleSubmit());
        tvToggle.setOnClickListener(v -> toggleMode());
    }

    private void setupRoleSpinner() {
        String[] roles = new String[]{getString(R.string.role_reader), getString(R.string.role_translator)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;

        if (isRegisterMode) {
            tvTitle.setText("Đăng Ký");
            btnSubmit.setText("Đăng Ký");
            tvToggle.setText("Đã có tài khoản? Đăng nhập");
            etConfirmPassword.setVisibility(View.VISIBLE);
            etName.setVisibility(View.VISIBLE);
            spinnerRole.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setText("Đăng Nhập");
            btnSubmit.setText("Đăng Nhập");
            tvToggle.setText("Chưa có tài khoản? Đăng ký");
            etConfirmPassword.setVisibility(View.GONE);
            etName.setVisibility(View.GONE);
            spinnerRole.setVisibility(View.GONE);
        }
    }

    private void handleSubmit() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ Email và Mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRegisterMode) {
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedPosition = spinnerRole.getSelectedItemPosition();
            String role = selectedPosition == 1 ? SharedPrefsHelper.ROLE_TRANSLATOR : SharedPrefsHelper.ROLE_READER;

            boolean success = prefsHelper.saveRegistration(email, name, password, role);

            if (success) {
                Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                toggleMode();
            } else {
                Toast.makeText(this, "Email này đã được đăng ký. Vui lòng sử dụng email khác.", Toast.LENGTH_LONG).show();
            }

        } else {
            if (prefsHelper.checkLogin(email, password)) {

                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                StoryManager.getInstance(this).refreshLibraryForCurrentUser();

                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            } else {
                Toast.makeText(this, "Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại.", Toast.LENGTH_LONG).show();
            }
        }
    }
}