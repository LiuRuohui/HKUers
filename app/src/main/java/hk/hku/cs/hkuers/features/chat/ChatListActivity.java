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
import com.google.firebase.firestore.DocumentSnapshot;
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
        // 查询用户参与的聊天室
        Query query = db.collection("chat_rooms")
                .whereArrayContains("member_ids", currentUser.getUid())
                .orderBy("last_message_time", Query.Direction.DESCENDING);
        
        FirestoreRecyclerOptions<ChatGroup> options = new FirestoreRecyclerOptions.Builder<ChatGroup>()
                .setQuery(query, ChatGroup.class)
                .build();
        
        adapter = new FirestoreRecyclerAdapter<ChatGroup, ChatGroupViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ChatGroupViewHolder holder, int position, @NonNull ChatGroup model) {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
                
                // 获取聊天室ID和名称
                String chatId = snapshot.getId();
                model.setGroupId(chatId);
                
                // 获取聊天室名称
                final String chatName;  // 声明为final
                String nameFromDb = snapshot.getString("chat_name");
                if (nameFromDb == null || nameFromDb.isEmpty()) {
                    // 尝试其他字段名
                    nameFromDb = snapshot.getString("name");
                    if (nameFromDb == null || nameFromDb.isEmpty()) {
                        // 使用ID的前几个字符作为名称
                        chatName = "聊天室-" + chatId.substring(0, Math.min(6, chatId.length()));
                    } else {
                        chatName = nameFromDb;
                    }
                } else {
                    chatName = nameFromDb;
                }
                model.setGroupName(chatName);
                
                // 获取最后消息和时间
                model.setLastMessage(snapshot.getString("last_message"));
                model.setLastMessageTimestamp(snapshot.getTimestamp("last_message_time"));
                
                // 计算未读消息数
                Map<String, Object> readCounts = (Map<String, Object>) snapshot.get("read_counts");
                Map<String, Object> userReadStatus = (Map<String, Object>) snapshot.get("user_read_status");
                
                if (readCounts != null && userReadStatus != null) {
                    Long messageCount = snapshot.getLong("message_count");
                    if (messageCount == null) messageCount = 0L;
                    
                    // 获取当前用户的已读消息数
                    Long userReadCount = 0L;
                    Object userReadCountObj = readCounts.get(currentUser.getUid());
                    if (userReadCountObj instanceof Long) {
                        userReadCount = (Long) userReadCountObj;
                    }
                    
                    // 计算未读消息数
                    long unreadCount = messageCount - userReadCount;
                    model.setUnreadCount((int) unreadCount);
                }
                
                // 绑定数据到视图
                holder.bind(model);
                
                // 设置点击事件
                holder.itemView.setOnClickListener(v -> {
                    // 标记消息为已读
                    markGroupAsRead(chatId);
                    
                    // 打开聊天室
                    openChatActivity(chatId, chatName);
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
        
        // 首先查询是否已存在相同的聊天室
        db.collection("chat_rooms")
            .whereEqualTo("course", course)
            .whereEqualTo("section", section)
            .whereEqualTo("semester", semester)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    // 聊天室已存在，提示用户并询问是否加入
                    DocumentReference existingChatRef = queryDocumentSnapshots.getDocuments().get(0).getReference();
                    String existingChatId = existingChatRef.getId();
                    String existingChatName = queryDocumentSnapshots.getDocuments().get(0).getString("chat_name");
                    
                    new AlertDialog.Builder(ChatListActivity.this)
                        .setTitle("聊天室已存在")
                        .setMessage("相同课程的聊天室已存在，您想加入该聊天室吗？")
                        .setPositiveButton("加入", (dialog, which) -> {
                            // 用户选择加入现有聊天室
                            joinExistingChatGroup(existingChatId, existingChatName);
                        })
                        .setNegativeButton("取消", null)
                        .show();
                } else {
                    // 聊天室不存在，创建新聊天室
                    createNewChatGroupInternal(chatName, course, section, semester);
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "查询聊天室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }

    /**
     * 内部方法，实际创建新的聊天群组
     */
    private void createNewChatGroupInternal(String chatName, String course, String section, String semester) {
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
                    Intent intent = new Intent(ChatListActivity.this, ChatRoomActivity.class);
                    intent.putExtra("chatRoomId", chatId);
                    intent.putExtra("chatRoomName", chatName);
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
        message.put("senderId", "system");  // 使用与Message类一致的字段名
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
     * 加入现有聊天群组
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     */
    private void joinExistingChatGroup(String chatId, String chatName) {
        // 检查用户是否已经是群组成员
        db.collection("chat_rooms").document(chatId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> memberIds = (List<String>) documentSnapshot.get("member_ids");
                    
                    if (memberIds != null && memberIds.contains(currentUser.getUid())) {
                        // 用户已经是成员，直接进入聊天页面
                        Toast.makeText(this, "您已经是该群组成员", Toast.LENGTH_SHORT).show();
                        openChatActivity(chatId, chatName);
                        return;
                    }
                    
                    // 用户不是成员，添加用户到成员列表
                    memberIds.add(currentUser.getUid());
                    
                    // 更新聊天室文档
                    documentSnapshot.getReference().update("member_ids", memberIds)
                        .addOnSuccessListener(aVoid -> {
                            // 添加用户详细信息到成员子集合
                            addUserToGroup(chatId, chatName);
                            
                            // 更新用户读取状态
                            Timestamp now = new Timestamp(new Date());
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("user_read_status." + currentUser.getUid(), now);
                            
                            // 获取当前消息计数
                            Long messageCount = documentSnapshot.getLong("message_count");
                            if (messageCount == null) messageCount = 0L;
                            
                            // 更新用户读取计数
                            updates.put("read_counts." + currentUser.getUid(), messageCount);
                            
                            documentSnapshot.getReference().update(updates)
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "成功加入聊天室", Toast.LENGTH_SHORT).show();
                                    
                                    // 添加用户加入的系统消息
                                    addUserJoinMessage(chatId);
                                    
                                    // 进入聊天页面
                                    openChatActivity(chatId, chatName);
                                });
                        });
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "加入聊天室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }

    /**
     * 添加用户加入的系统消息
     * @param chatId 聊天室ID
     */
    private void addUserJoinMessage(String chatId) {
        String userName = currentUser.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = currentUser.getEmail();
        }
        
        // 创建final变量以便在lambda表达式中使用
        final String finalUserName = userName;
        
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "system");  // 修正字段名称，与Message类一致
        message.put("text", finalUserName + " 加入了聊天室");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        db.collection("chat_rooms").document(chatId)
          .collection("messages")
          .add(message)
          .addOnSuccessListener(documentReference -> {
              // 更新聊天室的消息计数和最后消息时间
              db.collection("chat_rooms").document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentCount = documentSnapshot.getLong("message_count");
                        if (currentCount == null) currentCount = 0L;
                        
                        documentSnapshot.getReference().update(
                            "message_count", currentCount + 1,
                            "last_message", finalUserName + " 加入了聊天室",
                            "last_message_time", new Timestamp(new Date())
                        );
                    }
                });
          });
    }

    /**
     * 打开聊天页面
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     */
    private void openChatActivity(String chatId, String chatName) {
        Intent intent = new Intent(ChatListActivity.this, ChatRoomActivity.class);
        intent.putExtra("chatRoomId", chatId);
        intent.putExtra("chatRoomName", chatName);
        startActivity(intent);
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