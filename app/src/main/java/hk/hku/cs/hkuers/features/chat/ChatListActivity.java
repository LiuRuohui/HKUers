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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
        // 记录当前用户ID，用于日志
        String uid = currentUser.getUid();
        android.util.Log.d("ChatListActivity", "开始加载聊天列表，当前用户ID: " + uid);
        
        // 查询用户参与的聊天室 - 直接从chat_rooms表查询
        Query query = db.collection("chat_rooms")
                .whereArrayContains("member_ids", uid);  // 使用数组字段存储成员ID
        
        // 添加可选的排序，如果存在排序字段则使用
        Query queryWithOrder = query.orderBy("last_message_time", Query.Direction.DESCENDING);  // 按最后消息时间排序

        // 记录查询条件
        android.util.Log.d("ChatListActivity", "查询条件: whereArrayContains('member_ids', '" + uid + 
                "') orderBy('last_message_time', DESCENDING)");

        FirestoreRecyclerOptions<ChatGroup> options = new FirestoreRecyclerOptions.Builder<ChatGroup>()
                .setQuery(queryWithOrder, snapshot -> {
                    // 转换器：从Firestore文档到ChatGroup对象
                    String chatId = snapshot.getId();
                    String chatName = snapshot.getString("chat_name");
                    String lastMessage = snapshot.getString("last_message") != null ? 
                                        snapshot.getString("last_message") : "No messages yet";
                    Timestamp lastMessageTime = snapshot.getTimestamp("last_message_time");
                    
                    // 记录查询到的每个文档
                    android.util.Log.d("ChatListActivity", "加载到聊天室: ID=" + chatId + ", 名称=" + chatName);
                    
                    // 如果没有最后消息时间，使用创建时间
                    if (lastMessageTime == null) {
                        lastMessageTime = snapshot.getTimestamp("created_at");
                        android.util.Log.d("ChatListActivity", "聊天室 " + chatId + " 没有最后消息时间，使用创建时间");
                    }
                    
                    // 计算未读消息数
                    Long unreadCount = 0L;
                    Map<String, Object> userReadStatus = snapshot.contains("user_read_status") ? 
                            (Map<String, Object>) snapshot.get("user_read_status") : new HashMap<>();
                            
                    if (userReadStatus != null && userReadStatus.containsKey(uid)) {
                        // 获取用户已读时间
                        Timestamp userReadTime = (Timestamp) userReadStatus.get(uid);
                        // 获取当前总消息数
                        Long totalMessages = snapshot.getLong("message_count");
                        
                        if (userReadTime != null && totalMessages != null) {
                            // 查询该时间后的消息数量作为未读数
                            Map<String, Object> readCounts = snapshot.contains("read_counts") ? 
                                    (Map<String, Object>) snapshot.get("read_counts") : new HashMap<>();
                            
                            // 获取用户已读消息数
                            Long userReadCount = 0L;
                            if (readCounts != null && readCounts.containsKey(uid)) {
                                Object value = readCounts.get(uid);
                                if (value instanceof Long) {
                                    userReadCount = (Long) value;
                                } else if (value instanceof Number) {
                                    userReadCount = ((Number) value).longValue();
                                }
                            }
                            
                            // 计算未读消息数
                            unreadCount = totalMessages - userReadCount;
                            android.util.Log.d("ChatListActivity", "聊天室 " + chatId + " 总消息: " + totalMessages 
                                    + ", 已读: " + userReadCount + ", 未读: " + unreadCount);
                        }
                    }
                    
                    // 创建并返回ChatGroup对象
                    ChatGroup chatGroup = new ChatGroup(chatId, chatName, lastMessage, lastMessageTime);
                    chatGroup.setUnreadCount(unreadCount.intValue());
                    return chatGroup;
                })
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
                int itemCount = getItemCount();
                android.util.Log.d("ChatListActivity", "数据加载完成，共有 " + itemCount + " 个聊天室");
                
                if (itemCount == 0) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    android.util.Log.d("ChatListActivity", "没有聊天室，显示空状态");
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                    android.util.Log.d("ChatListActivity", "有聊天室，显示列表");
                }
            }
        };

        recyclerView.setAdapter(adapter);
        android.util.Log.d("ChatListActivity", "已设置适配器");
    }

    /**
     * 标记聊天组为已读
     * @param chatId 聊天室ID
     */
    private void markGroupAsRead(String chatId) {
        // 获取当前时间作为读取时间戳
        Timestamp now = new Timestamp(new Date());
        
        // 更新聊天室中的用户读取状态
        db.collection("chat_rooms").document(chatId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // 获取当前消息计数
                    Long messageCount = documentSnapshot.getLong("message_count");
                    if (messageCount == null) messageCount = 0L;
                    
                    // 更新操作
                    Map<String, Object> updates = new HashMap<>();
                    
                    // 更新用户读取时间
                    updates.put("user_read_status." + currentUser.getUid(), now);
                    
                    // 更新用户读取计数
                    updates.put("read_counts." + currentUser.getUid(), messageCount);
                    
                    // 应用更新
                    db.collection("chat_rooms").document(chatId)
                        .update(updates)
                        .addOnFailureListener(e -> 
                            android.util.Log.e("ChatListActivity", "标记已读失败: " + e.getMessage()));
                }
            });
    }

    /**
     * 显示创建新聊天对话框
     */
    private void showCreateChatDialog() {
        // 创建对话框布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_chat, null);
        TextInputLayout tilCourse = dialogView.findViewById(R.id.tilCourse);
        TextInputLayout tilSection = dialogView.findViewById(R.id.tilSection);
        TextInputLayout tilSemester = dialogView.findViewById(R.id.tilSemester);
        TextInputEditText etCourse = dialogView.findViewById(R.id.etCourse);
        TextInputEditText etSection = dialogView.findViewById(R.id.etSection);
        TextInputEditText etSemester = dialogView.findViewById(R.id.etSemester);

        // 创建对话框
        new MaterialAlertDialogBuilder(this)
                .setTitle("创建课程聊天群组")
                .setView(dialogView)
                .setPositiveButton("创建", (dialog, which) -> {
                    // 验证输入
                    String course = etCourse.getText().toString().trim();
                    String section = etSection.getText().toString().trim();
                    String semester = etSemester.getText().toString().trim();
                    
                    if (course.isEmpty()) {
                        Toast.makeText(this, "请输入课程编号", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (section.isEmpty()) {
                        Toast.makeText(this, "请输入班级", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (semester.isEmpty()) {
                        Toast.makeText(this, "请输入学期", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 创建聊天群组
                    createNewChatGroup(course, section, semester);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 创建新的聊天群组
     * @param course 课程编号
     * @param section 班级
     * @param semester 学期
     */
    private void createNewChatGroup(String course, String section, String semester) {
        // 生成聊天室名称 格式: 课程-班级-学期
        String chatName = course + "-" + section + "-" + semester;
        
        // 获取当前时间
        Timestamp now = new Timestamp(new Date());
        
        // 生成唯一ID (可选用chatName作为ID，但这里仍使用自动生成的ID)
        String chatId = db.collection("chat_rooms").document().getId();
        
        // 在Firestore中创建聊天室文档
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chat_id", chatId);
        chatData.put("chat_name", chatName);
        chatData.put("course", course);
        chatData.put("section", section);
        chatData.put("semester", semester);
        chatData.put("creator_id", currentUser.getUid());
        chatData.put("created_at", now);
        chatData.put("is_active", true);
        
        // 设置最后消息时间为创建时间，确保聊天室能在列表查询中显示
        chatData.put("last_message_time", now);
        chatData.put("last_message", "聊天室已创建，开始交流吧！");
        
        // 为聊天室生成一个随机颜色代码
        int colorIndex = Math.abs(chatId.hashCode()) % ChatGroupViewHolder.GROUP_COLORS.length;
        String colorCode = String.format("#%06X", (0xFFFFFF & ChatGroupViewHolder.GROUP_COLORS[colorIndex]));
        chatData.put("color_code", colorCode);
        
        // 初始化成员列表
        List<String> memberIds = new ArrayList<>();
        memberIds.add(currentUser.getUid());
        chatData.put("member_ids", memberIds);
        
        // 初始化消息计数
        chatData.put("message_count", 0L);
        
        // 初始化用户读取状态
        Map<String, Object> userReadStatus = new HashMap<>();
        userReadStatus.put(currentUser.getUid(), now);
        chatData.put("user_read_status", userReadStatus);
        
        // 初始化读取计数
        Map<String, Object> readCounts = new HashMap<>();
        readCounts.put(currentUser.getUid(), 0L);
        chatData.put("read_counts", readCounts);

        // 保存聊天室数据
        db.collection("chat_rooms").document(chatId)
                .set(chatData)
                .addOnSuccessListener(documentReference -> {
                    // 将用户详细信息添加到聊天室成员子集合
                    addUserToGroup(chatId, chatName);
                    
                    Toast.makeText(this, "课程群组创建成功", Toast.LENGTH_SHORT).show();
                    
                    // 添加一条初始消息
                    addInitialMessage(chatId);
                    
                    // 刷新适配器
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    
                    // 创建后直接进入聊天页面
                    Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                    intent.putExtra("groupId", chatId);
                    intent.putExtra("groupName", chatName);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "创建群组失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * 添加初始消息到聊天室
     * @param chatId 聊天室ID
     */
    private void addInitialMessage(String chatId) {
        Map<String, Object> message = new HashMap<>();
        message.put("sender_id", "system");
        message.put("sender_name", "系统");
        message.put("text", "聊天室已创建，开始交流吧！");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        db.collection("chat_rooms").document(chatId)
          .collection("messages")
          .add(message)
          .addOnSuccessListener(documentReference -> {
              // 更新聊天室的消息计数和最后消息时间
              db.collection("chat_rooms").document(chatId)
                .update(
                    "message_count", 1L,
                    "last_message", "聊天室已创建，开始交流吧！",
                    "last_message_time", new Timestamp(new Date())
                );
          });
    }

    /**
     * 将当前用户添加到聊天群组成员列表中
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     */
    private void addUserToGroup(String chatId, String chatName) {
        // 添加用户详细信息到聊天室成员子集合
        Map<String, Object> memberData = new HashMap<>();
        memberData.put("user_id", currentUser.getUid());
        memberData.put("email", currentUser.getEmail());
        memberData.put("display_name", currentUser.getDisplayName());
        memberData.put("joined_at", new Timestamp(new Date()));
        memberData.put("is_admin", true);  // 创建者为管理员
        memberData.put("is_active", true);
        
        // 保存到聊天室的成员子集合
        db.collection("chat_rooms")
            .document(chatId)
            .collection("members")
            .document(currentUser.getUid())
            .set(memberData)
            .addOnFailureListener(e ->
                Toast.makeText(this, "添加成员失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }

    /**
     * 添加测试数据（仅用于开发测试）
     */
    private void addTestData() {
        if (currentUser == null) return;

        // 测试课程数据 [课程, 班级, 学期]
        String[][] testCourses = {
            {"COMP7506", "1A", "2024"},
            {"COMP7506", "2B", "2024"},
            {"COMP7507", "1", "2024"},
            {"COMP7504", "A", "2024"},
            {"COMP7508", "C", "2024"}
        };

        // 添加测试群组
        for (String[] courseData : testCourses) {
            String course = courseData[0];
            String section = courseData[1];
            String semester = courseData[2];
            
            // 创建聊天群组
            createNewChatGroup(course, section, semester);
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