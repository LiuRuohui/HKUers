package hk.hku.cs.hkuers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import hk.hku.cs.hkuers.features.chat.ChatListActivity;
import hk.hku.cs.hkuers.features.courses.CourseSearchActivity;
import hk.hku.cs.hkuers.features.forum.ForumActivity;
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置顶部工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // 隐藏默认标题
        
        // 设置右上角个人资料按钮
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> openProfile());

        // 设置底部导航
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        
        // 检查是否从聊天室页面跳转过来
        boolean fromChatRoom = getIntent().getBooleanExtra("from_chat_room", false);
        
        // 设置默认选中项为Home
        bottomNavigation.setSelectedItemId(R.id.navigation_dashboard);
        
        // 如果是从聊天室跳转过来，添加日志
        if (fromChatRoom) {
            android.util.Log.d("MainActivity", "从ChatRoom页面跳转过来，强制选中Home选项");
        }
        
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_chat) {
                // 处理聊天按钮点击
                openChat();
                return true;
            } else if (itemId == R.id.navigation_forum) {
                // 处理论坛按钮点击
                openForum();
                return true;
            } else if (itemId == R.id.navigation_dashboard) {
                // 主页已经是当前页面，不需要额外操作
                return true;
            } else if (itemId == R.id.navigation_courses) {
                // 处理课程按钮点击
                openCourses();
                return true;
            } else if (itemId == R.id.navigation_marketplace) {
                // 处理市场按钮点击
                openMarketplace();
                return true;
            }
            
            return false;
        });
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

    private void openForum() {startActivity(new Intent(this, ForumActivity.class));
    }
}