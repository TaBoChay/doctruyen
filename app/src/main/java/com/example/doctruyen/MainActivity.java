package com.example.doctruyen;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.doctruyen.fragments.TuTruyenFragment;
import com.example.doctruyen.fragments.TrangChuFragment;
import com.example.doctruyen.fragments.XepHangFragment;
import com.example.doctruyen.fragments.TaiKhoanFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 1. Set listener trước
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_tu_truyen) {
                    selectedFragment = new TuTruyenFragment();
                } else if (itemId == R.id.nav_trang_chu) {
                    selectedFragment = new TrangChuFragment();
                } else if (itemId == R.id.nav_xep_hang) {
                    selectedFragment = new XepHangFragment();
                } else if (itemId == R.id.nav_tai_khoan) {
                    selectedFragment = new TaiKhoanFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });

        // 2. Lần đầu mở app → load TrangChuFragment trực tiếp
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TrangChuFragment())
                    .commit();
        }
    }
}
