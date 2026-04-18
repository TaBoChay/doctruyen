package com.example.doctruyen.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson; // Yêu cầu thư viện Gson
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPrefsHelper {

    private static final String PREF_NAME = "DocTruyenPrefs";
    private static final String KEY_ACCOUNTS = "registered_accounts"; // Khóa mới lưu trữ JSON Array
    private static final String KEY_LOGGED_IN_EMAIL = "loggedInEmail"; // Khóa lưu email user đang đăng nhập
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn"; // Khóa trạng thái đăng nhập

    private static SharedPrefsHelper instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    // Role constants
    public static final String ROLE_READER = "reader";
    public static final String ROLE_TRANSLATOR = "translator"; // Dịch giả/người đăng truyện

    // --- Lớp nội bộ để lưu trữ thông tin tài khoản ---
    private static class User {
        String email;
        String name;
        String password;
        String role; // reader hoặc translator

        public User(String email, String name, String password, String role) {
            this.email = email;
            this.name = name;
            this.password = password;
            this.role = role;
        }
    }

    private SharedPrefsHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized SharedPrefsHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefsHelper(context.getApplicationContext());
        }
        return instance;
    }

    // --- PHƯƠNG THỨC HỖ TRỢ ĐỌC/GHI DANH SÁCH TÀI KHOẢN ---

    private List<User> getAccountList() {
        String json = prefs.getString(KEY_ACCOUNTS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<User>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void saveAccountList(List<User> userList) {
        String json = gson.toJson(userList);
        prefs.edit().putString(KEY_ACCOUNTS, json).apply();
    }

    // 1. ĐĂNG KÝ (Lưu nhiều tài khoản)
    /**
     * Lưu thông tin đăng ký vào danh sách tài khoản.
     * @return true nếu đăng ký thành công, false nếu email đã tồn tại.
     */
    public boolean saveRegistration(String email, String name, String password, String role) {
        List<User> userList = getAccountList();

        for (User user : userList) {
            if (user.email.equals(email)) {
                return false;
            }
        }

        if (role == null || role.isEmpty()) {
            role = ROLE_READER;
        }

        User newUser = new User(email, name, password, role);
        userList.add(newUser);
        saveAccountList(userList);

        Log.d("PREFS_HELPER", "Registration saved for: " + email + " with role: " + role);
        return true;
    }

    /**
     * Overload method for backward compatibility
     */
    public boolean saveRegistration(String email, String name, String password) {
        return saveRegistration(email, name, password, ROLE_READER);
    }

    // 2. ĐĂNG NHẬP (Kiểm tra nhiều tài khoản)
    public boolean checkLogin(String email, String password) {
        List<User> userList = getAccountList();

        for (User user : userList) {
            if (user.email.equals(email) && user.password.equals(password)) {
                // Đăng nhập thành công: Lưu trạng thái session của tài khoản này
                setLoggedIn(true);
                setLoggedInEmail(email);
                return true;
            }
        }
        return false;
    }

    // 3. ĐỔI MẬT KHẨU
    public boolean changePassword(String currentPassword, String newPassword) {
        String loggedInEmail = getLoggedInEmail();
        if (loggedInEmail.isEmpty()) return false;

        List<User> userList = getAccountList();

        for (User user : userList) {
            if (user.email.equals(loggedInEmail) && user.password.equals(currentPassword)) {
                // Cập nhật mật khẩu và lưu lại danh sách
                user.password = newPassword;
                saveAccountList(userList);
                Log.d("PREFS_HELPER", "Password changed successfully for: " + loggedInEmail);
                return true;
            }
        }
        Log.w("PREFS_HELPER", "Password change failed: Current password mismatch or user not found.");
        return false;
    }

    // --- CÁC GETTER/SETTER CHO SESSION VÀ THÔNG TIN USER ---

    // Lưu email của tài khoản đang đăng nhập
    private void setLoggedInEmail(String email) {
        prefs.edit().putString(KEY_LOGGED_IN_EMAIL, email).apply();
    }

    // Lấy email của tài khoản đang đăng nhập
    public String getLoggedInEmail() {
        return prefs.getString(KEY_LOGGED_IN_EMAIL, "");
    }

    // Lấy thông tin user đang đăng nhập từ danh sách lưu trữ
    private User getCurrentUser() {
        String loggedInEmail = getLoggedInEmail();
        if (loggedInEmail.isEmpty()) return null;

        List<User> userList = getAccountList();

        for (User user : userList) {
            if (user.email.equals(loggedInEmail)) {
                return user;
            }
        }
        return null;
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean isLoggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();

        // Nếu đăng xuất, xóa email session
        if (!isLoggedIn) {
            setLoggedInEmail("");
        }
    }

    // Lấy tên người dùng đang đăng nhập
    public String getUserName() {
        User user = getCurrentUser();
        return user != null ? user.name : "Guest";
    }

    // Lấy email người dùng đang đăng nhập
    public String getUserEmail() {
        return getLoggedInEmail();
    }

    // Lấy role của user đang đăng nhập
    public String getUserRole() {
        User user = getCurrentUser();
        return user != null ? user.role : ROLE_READER;
    }

    // Kiểm tra user có phải là dịch giả/người đăng truyện không
    public boolean isTranslator() {
        return ROLE_TRANSLATOR.equals(getUserRole());
    }

    // Cập nhật role của user hiện tại
    public boolean updateUserRole(String newRole) {
        String loggedInEmail = getLoggedInEmail();
        if (loggedInEmail.isEmpty()) return false;

        List<User> userList = getAccountList();
        for (User user : userList) {
            if (user.email.equals(loggedInEmail)) {
                user.role = newRole;
                saveAccountList(userList);
                Log.d("PREFS_HELPER", "User role updated to: " + newRole);
                return true;
            }
        }
        return false;
    }

    // Xóa session hiện tại (Đăng xuất)
    public void clearUserData() {
        // Chỉ xóa session hiện tại, không xóa dữ liệu tài khoản
        setLoggedIn(false);
        // Lưu ý: Nếu bạn muốn xóa toàn bộ tài khoản, cần clear KEY_ACCOUNTS
    }
}