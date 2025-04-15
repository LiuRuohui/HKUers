package hk.hku.cs.hkuers.features.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import hk.hku.cs.hkuers.MainActivity;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.courses.CourseSearchActivity;
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;
import hk.hku.cs.hkuers.models.ChatGroup;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private Button btnChat, btnMap, btnProfile, btnCourses, btnMarketplace;
    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter<ChatGroup, ChatGroupViewHolder> adapter;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // 初始化Firebase和用户
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // 如果用户未登录，返回到登录页面
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化视图组件
        recyclerView = findViewById(R.id.recyclerChatGroups);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ImageButton btnCreateChat = findViewById(R.id.btnCreateChat);
        ImageButton btnBack = findViewById(R.id.btnBack);
        
        // 初始化底部导航按钮
        btnChat = findViewById(R.id.btnChat);
        btnMap = findViewById(R.id.btnMap);
        btnProfile = findViewById(R.id.btnProfile);
        btnCourses = findViewById(R.id.btnCourses);
        btnMarketplace = findViewById(R.id.btnMarketplace);

        // 设置布局管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> finish());

        // 设置创建聊天按钮点击事件
        btnCreateChat.setOnClickListener(v -> showCreateChatDialog());

        // 设置底部导航点击事件
        setupBottomNavigation();
        
        // 高亮当前选中的聊天按钮
        btnChat.setTextSize(16);
        btnChat.setTextColor(getResources().getColor(android.R.color.white));
        btnChat.setTextSize(16);
        btnChat.setTypeface(null, android.graphics.Typeface.BOLD);

        // 加载聊天列表
        loadChatGroups();
        
        // 添加长按创建聊天按钮的测试数据功能
        btnCreateChat.setOnLongClickListener(v -> {
            addTestData();
            Toast.makeText(this, "已添加测试数据", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    /**
     * 设置底部导航按钮的点击事件
     */
    private void setupBottomNavigation() {
        // 聊天按钮已经在当前页面，不需要处理
        
        // 地图按钮
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
            finish();
        });
        
        // 个人资料按钮
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("openProfile", true);
            startActivity(intent);
            finish();
        });
        
        // 课程按钮
        btnCourses.setOnClickListener(v -> {
            Intent intent = new Intent(this, CourseSearchActivity.class);
            startActivity(intent);
            finish();
        });
        
        // 市场按钮
        btnMarketplace.setOnClickListener(v -> {
            Intent intent = new Intent(this, MarketplaceActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * 加载用户参与的聊天群组
     */
    private void loadChatGroups() {
        // 查询用户加入的群组，按最后消息时间倒序排列
        Query query = db.collection("users").document(currentUser.getUid())
                .collection("joinedGroups")
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatGroup> options = new FirestoreRecyclerOptions.Builder<ChatGroup>()
                .setQuery(query, ChatGroup.class)
                .build();

        // 配置适配器
        adapter = new FirestoreRecyclerAdapter<ChatGroup, ChatGroupViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ChatGroupViewHolder holder, int position, @NonNull ChatGroup model) {
                // 绑定视图数据
                holder.bind(model);
                
                // 调试日志 - 打印群组ID和颜色索引
                String groupId = model.getGroupId();
                if (groupId != null && !groupId.isEmpty()) {
                    int colorIndex = Math.abs(groupId.hashCode()) % ChatGroupViewHolder.GROUP_COLORS.length;
                    android.util.Log.d("ChatListActivity", 
                        "Group ID: " + groupId + 
                        ", Hash: " + groupId.hashCode() + 
                        ", Color Index: " + colorIndex);
                }
                
                // 设置点击事件，进入聊天详情
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                    intent.putExtra("groupId", model.getGroupId());
                    intent.putExtra("groupName", model.getGroupName());
                    startActivity(intent);
                    
                    // 标记已读
                    markGroupAsRead(model.getGroupId());
                });
            }

            @NonNull
            @Override
            public ChatGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_chat_group, parent, false);
                return new ChatGroupViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                // 数据加载完成后，检查是否为空
                if (getItemCount() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                }
            }
        };

        recyclerView.setAdapter(adapter);
    }

    /**
     * 标记聊天组为已读
     * @param groupId 群组ID
     */
    private void markGroupAsRead(String groupId) {
        db.collection("users").document(currentUser.getUid())
                .collection("joinedGroups").document(groupId)
                .update("unreadCount", 0);
    }

    /**
     * 显示创建新聊天对话框
     */
    private void showCreateChatDialog() {
        // 创建对话框布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_chat, null);
        TextInputLayout tilGroupName = dialogView.findViewById(R.id.tilGroupName);
        TextInputEditText etGroupName = dialogView.findViewById(R.id.etGroupName);

        // 创建对话框
        new MaterialAlertDialogBuilder(this)
                .setTitle("创建新聊天群组")
                .setView(dialogView)
                .setPositiveButton("创建", (dialog, which) -> {
                    String groupName = etGroupName.getText().toString().trim();
                    if (!groupName.isEmpty()) {
                        createNewChatGroup(groupName);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 创建新的聊天群组
     * @param groupName 群组名称
     */
    private void createNewChatGroup(String groupName) {
        // 在Firestore中创建群组文档
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("name", groupName);
        groupData.put("createdBy", currentUser.getUid());
        groupData.put("createdAt", new Timestamp(new Date()));

        db.collection("groups").add(groupData)
                .addOnSuccessListener(documentReference -> {
                    String groupId = documentReference.getId();
                    addUserToGroup(groupId, groupName);
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "创建群组失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * 将当前用户添加到新创建的聊天群组
     * @param groupId 群组ID
     * @param groupName 群组名称
     */
    private void addUserToGroup(String groupId, String groupName) {
        // 将用户加入群组的记录添加到用户文档
        ChatGroup userGroupData = new ChatGroup(
                groupId,
                groupName,
                "Group created",
                new Timestamp(new Date())
        );

        DocumentReference userGroupRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("joinedGroups")
                .document(groupId);

        userGroupRef.set(userGroupData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "群组创建成功", Toast.LENGTH_SHORT).show();
                    
                    // 创建后直接进入聊天页面
                    Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                    intent.putExtra("groupId", groupId);
                    intent.putExtra("groupName", groupName);
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "加入群组失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * 添加测试数据（仅用于开发测试）
     */
    private void addTestData() {
        if (currentUser == null) return;

        // 测试群组数据
        String[][] testGroups = {
            {"test_group_1", "COMP7506 Group A", "Latest project update discussion"},
            {"test_group_2", "COMP7506 Group B", "When is the next meeting?"},
            {"test_group_3", "COMP7506 Group C", "Project submission due tomorrow"},
            {"test_group_4", "COMP7506 Group D", "Has anyone started on task 3?"},
            {"test_group_5", "COMP7506 Group E", "Meeting at 3pm tomorrow"}
        };

        // 添加测试群组
        for (String[] groupData : testGroups) {
            String groupId = groupData[0];
            String groupName = groupData[1];
            String lastMessage = groupData[2];
            
            // 创建群组数据
            ChatGroup chatGroup = new ChatGroup(
                groupId, 
                groupName,
                lastMessage,
                new Timestamp(new Date())
            );
            
            // 随机设置未读消息数量（0-100之间）
            int unreadCount = (int)(Math.random() * 100);
            chatGroup.setUnreadCount(unreadCount);
            
            // 将群组数据添加到Firestore
            db.collection("users")
                .document(currentUser.getUid())
                .collection("joinedGroups")
                .document(groupId)
                .set(chatGroup)
                .addOnSuccessListener(aVoid -> 
                    android.util.Log.d("ChatListActivity", "添加测试数据成功: " + groupName))
                .addOnFailureListener(e -> 
                    android.util.Log.e("ChatListActivity", "添加测试数据失败: " + e.getMessage()));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    /**
     * 聊天群组ViewHolder类
     */
    public static class ChatGroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGroupName, tvLastMessage, tvTimestamp, tvUnreadCount;
        private final ImageView ivReadMark;
        private final View colorIndicator;
        
        // 聊天室颜色列表
        public static final int[] GROUP_COLORS = {
            0xFF4CAF50, // 绿色
            0xFF2196F3, // 蓝色
            0xFFE91E63, // 粉色
            0xFFFF9800, // 橙色
            0xFF9C27B0, // 紫色
            0xFF00BCD4, // 青色
            0xFFFF5722, // 深橙色
            0xFF607D8B, // 蓝灰色
            0xFFFFC107, // 琥珀色
            0xFF795548, // 棕色
            0xFF673AB7, // 深紫色
            0xFF8BC34A, // 浅绿色
            0xFF3F51B5, // 靛蓝色
            0xFFFF4081, // 粉红色
            0xFF009688  // 蓝绿色
        };
        
        public ChatGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            ivReadMark = itemView.findViewById(R.id.ivReadMark);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
        }

        public void bind(ChatGroup group) {
            // 设置群组名称
            tvGroupName.setText(group.getGroupName());
            
            // 设置最后消息
            tvLastMessage.setText(group.getLastMessage());
            
            // 设置时间戳
            tvTimestamp.setText(group.getFormattedTimestamp());
            
            // 设置随机但稳定的颜色（根据群组ID生成）
            String groupId = group.getGroupId();
            if (groupId != null && !groupId.isEmpty()) {
                int colorIndex = Math.abs(groupId.hashCode()) % GROUP_COLORS.length;
                colorIndicator.setBackgroundColor(GROUP_COLORS[colorIndex]);
            } else {
                // 默认颜色
                colorIndicator.setBackgroundColor(GROUP_COLORS[0]);
            }
            
            // 设置未读消息数量和已读标记
            int unreadCount = group.getUnreadCount();
            if (unreadCount > 0) {
                // 有未读消息，显示红点
                tvUnreadCount.setVisibility(View.VISIBLE);
                ivReadMark.setVisibility(View.GONE);
                
                // 未读消息超过99时显示"99+"
                if (unreadCount > 99) {
                    tvUnreadCount.setText("99+");
                } else {
                    tvUnreadCount.setText(String.valueOf(unreadCount));
                }
            } else {
                // 没有未读消息，显示绿色对勾
                tvUnreadCount.setVisibility(View.GONE);
                ivReadMark.setVisibility(View.VISIBLE);
            }
        }
    }
}