package hk.hku.cs.hkuers;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import hk.hku.cs.hkuers.features.chat.ChatListActivity;
import hk.hku.cs.hkuers.features.courses.CourseSearchActivity;
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerNotifications;
    private TextView tvNoNotifications;
    private NotificationAdapter notificationAdapter;
    private List<NotificationItem> notificationItems;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 设置顶部工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // 隐藏默认标题
        
        // 设置右上角个人资料按钮
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> openProfile());

        // 初始化通知栏
        recyclerNotifications = findViewById(R.id.recycler_notifications);
        tvNoNotifications = findViewById(R.id.tv_no_notifications);
        
        // 默认显示无通知状态
        tvNoNotifications.setVisibility(View.VISIBLE);
        recyclerNotifications.setVisibility(View.GONE);
        
        initNotifications();

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
                Toast.makeText(MainActivity.this, "Forum feature coming soon", Toast.LENGTH_SHORT).show();
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

    // 初始化通知系统
    private void initNotifications() {
        notificationItems = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notificationItems);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotifications.setAdapter(notificationAdapter);
        
        // 加载通知
        loadNotifications();
    }
    
    // 加载用户通知
    private void loadNotifications() {
        if (currentUser == null) return;
        
        // 检查用户是否有未读消息
        checkUnreadMessages();
        
        // 这里可以添加其他模块的通知检查
        // 例如：课程通知、二手物品交易通知等
    }
    
    // 检查未读消息
    private void checkUnreadMessages() {
        if (currentUser == null) return;
        
        // 方法一：直接查询用户文档中的标志
        db.collection("users").document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Boolean hasUnread = documentSnapshot.getBoolean("has_unread_messages");
                    Long unreadCount = documentSnapshot.getLong("unread_messages_count");
                    
                    if (hasUnread != null && hasUnread && unreadCount != null && unreadCount > 0) {
                        // 添加未读消息通知
                        NotificationItem notification = new NotificationItem(
                            "Chat",
                            "You have unread messages",
                                R.drawable.ic_chat_active_24dp,  // 这里应替换为相应的聊天图标
                            () -> openChat()
                        );
                        addOrUpdateNotification(notification);
                    }
                }
            });
        
        // 方法二：使用ChatListActivity中的静态方法进行检查
        // 此方法不仅会检查未读消息，还会更新用户文档中的未读状态
        ChatListActivity.checkUnreadMessages(currentUser, db, hasUnread -> {
            // 已在方法一中处理UI更新，这里可以作为备用检查
            android.util.Log.d("MainActivity", "未读消息检查结果: " + hasUnread);
        });
    }
    
    // 添加或更新通知
    private void addOrUpdateNotification(NotificationItem newItem) {
        boolean updated = false;
        
        // 查找是否已存在同类型通知
        for (int i = 0; i < notificationItems.size(); i++) {
            NotificationItem item = notificationItems.get(i);
            if (item.getTitle().equals(newItem.getTitle())) {
                // 更新现有通知
                notificationItems.set(i, newItem);
                updated = true;
                break;
            }
        }
        
        // 如果不存在，添加新通知
        if (!updated) {
            notificationItems.add(newItem);
        }
        
        // 更新UI
        updateNotificationsUI();
    }
    
    // 更新通知UI
    private void updateNotificationsUI() {
        if (notificationItems.isEmpty()) {
            tvNoNotifications.setVisibility(View.VISIBLE);
            recyclerNotifications.setVisibility(View.GONE);
            android.util.Log.d("MainActivity", "显示'No unread notification'文本");
        } else {
            tvNoNotifications.setVisibility(View.GONE);
            recyclerNotifications.setVisibility(View.VISIBLE);
            notificationAdapter.notifyDataSetChanged();
            android.util.Log.d("MainActivity", "显示" + notificationItems.size() + "条通知");
        }
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
    
    // 通知项数据类
    private static class NotificationItem {
        private String title;
        private String content;
        private int iconResource;
        private Runnable action;
        
        public NotificationItem(String title, String content, int iconResource, Runnable action) {
            this.title = title;
            this.content = content;
            this.iconResource = iconResource;
            this.action = action;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getContent() {
            return content;
        }
        
        public int getIconResource() {
            return iconResource;
        }
        
        public Runnable getAction() {
            return action;
        }
    }
    
    // 通知适配器
    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        
        private List<NotificationItem> items;
        
        public NotificationAdapter(List<NotificationItem> items) {
            this.items = items;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = items.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvContent.setText(item.getContent());
            holder.ivIcon.setImageResource(item.getIconResource());
            
            holder.cardNotification.setOnClickListener(v -> {
                if (item.getAction() != null) {
                    item.getAction().run();
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            CardView cardNotification;
            TextView tvTitle;
            TextView tvContent;
            ImageView ivIcon;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardNotification = itemView.findViewById(R.id.card_notification);
                tvTitle = itemView.findViewById(R.id.tv_notification_title);
                tvContent = itemView.findViewById(R.id.tv_notification_content);
                ivIcon = itemView.findViewById(R.id.iv_notification_icon);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 清空通知列表
        notificationItems.clear();
        // 更新空通知UI
        updateNotificationsUI();
        // 重新加载通知
        loadNotifications();
    }
}