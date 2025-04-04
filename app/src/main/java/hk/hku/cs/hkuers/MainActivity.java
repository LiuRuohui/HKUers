package hk.hku.cs.hkuers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import hk.hku.cs.hkuers.features.chat.ChatListActivity;
import hk.hku.cs.hkuers.features.courses.CourseSearchActivity;
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化按钮
        Button btnChat = findViewById(R.id.btnChat);
        Button btnMap = findViewById(R.id.btnMap);
        Button btnProfile = findViewById(R.id.btnProfile);
        Button btnCourses = findViewById(R.id.btnCourses);
        Button btnMarketplace = findViewById(R.id.btnMarketplace);

        // 设置点击事件
        btnChat.setOnClickListener(v -> openChat());
        btnMap.setOnClickListener(v -> openMap());
        btnProfile.setOnClickListener(v -> openProfile());
        btnCourses.setOnClickListener(v -> openCourses());
        btnMarketplace.setOnClickListener(v -> openMarketplace());
    }

    private void openChat() {
        startActivity(new Intent(this, ChatListActivity.class));
    }

    private void openMap() {
        startActivity(new Intent(this, MapActivity.class));
    }

    private void openProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void openCourses() {
        startActivity(new Intent(this, CourseSearchActivity.class));
    }

    private void openMarketplace() {
        startActivity(new Intent(this, MarketplaceActivity.class));
    }
}