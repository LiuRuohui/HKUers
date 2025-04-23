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
import android.widget.RadioGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;
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
import java.util.Calendar;

import hk.hku.cs.hkuers.MainActivity;
import hk.hku.cs.hkuers.ProfileActivity;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.courses.CourseSearchActivity;
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;
import hk.hku.cs.hkuers.models.ChatGroup;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private BottomNavigationView bottomNavigation;
    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter<ChatGroup, ChatGroupViewHolder> adapter;
    private FirebaseUser currentUser;
    private ImageButton btnFilter;
    
    // 筛选选项
    private String currentChatTypeFilter = null; // 当前聊天类型筛选，null表示全部
    private int currentTimeFilter = 0; // 0: 全部时间, 1: 今天, 2: 最近3天, 3: 最近一周
    private boolean showUnreadOnly = false; // 是否只显示未读消息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // 设置系统状态栏颜色为固定的深色（与顶部栏一致）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#1E1E1E"));
        }

        // 设置状态栏空间高度
        View statusBarSpace = findViewById(R.id.statusBarSpace);
        if (statusBarSpace != null) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                int statusBarHeight = getResources().getDimensionPixelSize(resourceId);
                ViewGroup.LayoutParams params = statusBarSpace.getLayoutParams();
                params.height = statusBarHeight;
                statusBarSpace.setLayoutParams(params);
            }
        }

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
        btnFilter = findViewById(R.id.btnFilter);
        
        // 初始化底部导航
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // 设置布局管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> {
            bottomNavigation.setSelectedItemId(R.id.navigation_dashboard);
            finish();
        });

        // 设置创建聊天按钮点击事件
        btnCreateChat.setOnClickListener(v -> showCreateChatDialog());
        
        // 设置筛选按钮点击事件
        btnFilter.setOnClickListener(v -> showFilterDialog());

        // 设置底部导航
        setupBottomNavigation();

        // 加载聊天列表
        loadChatGroups();
        
        // 添加长按创建聊天按钮的测试数据功能
        btnCreateChat.setOnLongClickListener(v -> {
            addTestData();
            Toast.makeText(this, "Test data added", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    /**
     * 设置底部导航
     */
    private void setupBottomNavigation() {
        // 设置选中Chat选项
        bottomNavigation.setSelectedItemId(R.id.navigation_chat);
        
        // 设置导航点击监听
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_chat) {
                // 已经在聊天页面，不需要操作
                return true;
            } else if (itemId == R.id.navigation_forum) {
                Toast.makeText(this, "Forum feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.navigation_dashboard) {
                // 跳转到主页
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_courses) {
                // 跳转到课程页面
                startActivity(new Intent(this, CourseSearchActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_marketplace) {
                // 跳转到市场页面
                startActivity(new Intent(this, MarketplaceActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    /**
     * 加载用户参与的聊天群组
     */
    private void loadChatGroups() {
        // 如果有正在监听的adapter，先停止监听
        if (adapter != null) {
            adapter.stopListening();
        }
        
        // 查询用户参与的聊天室
        Query query = db.collection("chat_rooms")
                .whereArrayContains("member_ids", currentUser.getUid());
        
        // 应用聊天类型筛选
        if (currentChatTypeFilter != null) {
            query = query.whereEqualTo("group_type", currentChatTypeFilter);
        }
        
        // 应用时间筛选
        if (currentTimeFilter > 0) {
            // 计算时间起点
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            switch (currentTimeFilter) {
                case 1: // 今天
                    // 使用今天凌晨的时间戳
                    break;
                case 2: // 最近3天
                    calendar.add(Calendar.DAY_OF_MONTH, -2); // -2因为今天已经包含了一天
                    break;
                case 3: // 最近一周
                    calendar.add(Calendar.DAY_OF_MONTH, -6); // -6因为今天已经包含了一天
                    break;
            }
            
            Timestamp startTime = new Timestamp(calendar.getTime());
            query = query.whereGreaterThanOrEqualTo("last_message_time", startTime);
        }
        
        // 按时间降序排序（无论是否有时间筛选）
        query = query.orderBy("last_message_time", Query.Direction.DESCENDING);
        
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
                        chatName = "Chat Room-" + chatId.substring(0, Math.min(6, chatId.length()));
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
                
                // 获取群聊类型
                String groupType = snapshot.getString("group_type");
                if (groupType == null) {
                    // 兼容旧数据，未设置类型的默认为普通群聊
                    groupType = ChatGroup.TYPE_NORMAL;
                }
                model.setGroupType(groupType);
                
                // 获取创建者ID，用于判断是否为群主
                String creatorId = snapshot.getString("creator_id");
                boolean isCreator = currentUser.getUid().equals(creatorId);
                
                // 计算未读消息数
                int unreadCount = 0;
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
                    unreadCount = (int)(messageCount - userReadCount);
                    model.setUnreadCount(unreadCount);
                }
                
                // 如果筛选条件为仅显示未读且当前项没有未读消息，则跳过这个项
                if (showUnreadOnly && unreadCount <= 0) {
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                } else {
                    holder.itemView.setVisibility(View.VISIBLE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                
                // 绑定数据到视图
                holder.bind(model, isCreator);
                
                // 设置点击事件
                holder.itemView.setOnClickListener(v -> {
                    // 标记消息为已读
                    markGroupAsRead(chatId);
                    
                    // 打开聊天室
                    openChatActivity(chatId, chatName);
                });
                
                // 设置长按事件
                holder.itemView.setOnLongClickListener(v -> {
                    // 根据群聊类型处理长按菜单
                    showChatOptions(model, isCreator);
                    return true;
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
                int visibleItemCount = getVisibleItemCount();
                android.util.Log.d("ChatListActivity", "Data loaded successfully, found " + itemCount + " chat rooms, " + visibleItemCount + " visible");
                
                if (visibleItemCount == 0) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    
                    // 根据当前筛选类型设置不同的空状态提示
                    if (currentChatTypeFilter == null) {
                        tvEmptyState.setText(showUnreadOnly ? 
                                "No unread chats. All caught up!" : 
                                "No chats yet. Create your first chat!");
                    } else if (ChatGroup.TYPE_COURSE.equals(currentChatTypeFilter)) {
                        tvEmptyState.setText(showUnreadOnly ? 
                                "No unread course chats!" : 
                                "No course chats. Add a course to join a course chat!");
                    } else if (ChatGroup.TYPE_TRADE.equals(currentChatTypeFilter)) {
                        tvEmptyState.setText(showUnreadOnly ? 
                                "No unread trade chats!" : 
                                "No trade chats. Check the marketplace for trades!");
                    } else if (ChatGroup.TYPE_NORMAL.equals(currentChatTypeFilter)) {
                        tvEmptyState.setText(showUnreadOnly ? 
                                "No unread group chats!" : 
                                "No group chats. Create a new group to start chatting!");
                    }
                    
                    android.util.Log.d("ChatListActivity", "Chat rooms empty, showing empty state");
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                    android.util.Log.d("ChatListActivity", "Chat rooms available, showing list");
                }
            }
            
            // 计算实际可见的项目数（用于筛选后的空状态判断）
            private int getVisibleItemCount() {
                int visibleCount = 0;
                for (int i = 0; i < getItemCount(); i++) {
                    ChatGroup model = getItem(i);
                    if (!showUnreadOnly || model.getUnreadCount() > 0) {
                        visibleCount++;
                    }
                }
                return visibleCount;
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
        android.util.Log.d("ChatListActivity", "Adapter set and listening started");
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
        // 创建对话框选项
        final String[] options = {"Create Normal Group", "Join Existing Group"};
        
        new AlertDialog.Builder(this)
                .setTitle("Chat Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 创建普通群聊
                            showCreateNormalGroupDialog();
                            break;
                        case 1:
                            // 加入现有群聊
                            showJoinExistingGroupDialog();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * 显示创建普通群聊对话框
     */
    private void showCreateNormalGroupDialog() {
        // 创建对话框布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_normal_chat, null);
        TextInputLayout tilGroupName = dialogView.findViewById(R.id.tilGroupName);
        TextInputEditText etGroupName = dialogView.findViewById(R.id.etGroupName);

        // 创建对话框
        new AlertDialog.Builder(this)
                .setTitle("Create Group Chat")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    // 验证输入
                    String groupName = etGroupName.getText().toString().trim();
                    
                    if (groupName.isEmpty()) {
                        Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 创建普通群聊
                    createNormalChatGroup(groupName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * 显示加入现有群聊对话框
     */
    private void showJoinExistingGroupDialog() {
        // 创建对话框布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_join_chat, null);
        TextInputLayout tilGroupId = dialogView.findViewById(R.id.tilGroupId);
        TextInputEditText etGroupId = dialogView.findViewById(R.id.etGroupId);
        
        // 创建对话框
        new AlertDialog.Builder(this)
                .setTitle("Join Existing Group")
                .setView(dialogView)
                .setPositiveButton("Join", (dialog, which) -> {
                    // 验证输入
                    String groupId = etGroupId.getText().toString().trim();
                    
                    if (groupId.isEmpty()) {
                        Toast.makeText(this, "Please enter a group ID or name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 查找并加入群聊
                    findAndJoinChatGroup(groupId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * 查找并加入群聊
     * @param searchText 群聊ID或名称
     */
    private void findAndJoinChatGroup(String searchText) {
        // 首先尝试通过ID查找
        db.collection("chat_rooms").document(searchText)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // 找到了群聊，检查类型
                    String groupType = documentSnapshot.getString("group_type");
                    if (ChatGroup.TYPE_TRADE.equals(groupType)) {
                        Toast.makeText(this, "Cannot join trade chats", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 获取群聊名称
                    String chatName = documentSnapshot.getString("chat_name");
                    if (chatName == null || chatName.isEmpty()) {
                        chatName = "Chat Room-" + searchText;
                    }
                    
                    // 加入群聊
                    joinExistingChatGroup(searchText, chatName);
                    return;
                }
                
                // 通过ID没找到，尝试通过名称查找
        db.collection("chat_rooms")
                    .whereEqualTo("chat_name", searchText)
                    .whereNotEqualTo("group_type", ChatGroup.TYPE_TRADE) // 排除购物聊天
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                            // 找到了群聊
                            DocumentReference chatRef = queryDocumentSnapshots.getDocuments().get(0).getReference();
                            String chatId = chatRef.getId();
                            String chatName = queryDocumentSnapshots.getDocuments().get(0).getString("chat_name");
                            
                            // 加入群聊
                            joinExistingChatGroup(chatId, chatName);
                        } else {
                            Toast.makeText(this, "Group not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Query failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Query failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    /**
     * 创建普通群聊
     * @param groupName 群聊名称
     */
    private void createNormalChatGroup(String groupName) {
        // 首先检查是否已存在同名群聊
        db.collection("chat_rooms")
            .whereEqualTo("chat_name", groupName)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    // 群聊已存在，提示用户
                    new AlertDialog.Builder(ChatListActivity.this)
                        .setTitle("Group Already Exists")
                        .setMessage("A group with this name already exists. Would you like to join it?")
                        .setPositiveButton("Join", (dialog, which) -> {
                            // 用户选择加入现有群聊
                            DocumentReference existingChatRef = queryDocumentSnapshots.getDocuments().get(0).getReference();
                            String existingChatId = existingChatRef.getId();
                            joinExistingChatGroup(existingChatId, groupName);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else {
                    // 群聊不存在，创建新群聊
                    createNewNormalChatGroupInternal(groupName);
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Query failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }

    /**
     * 内部方法，实际创建新的普通群聊
     * @param chatName 群聊名称
     */
    private void createNewNormalChatGroupInternal(String chatName) {
        // 获取当前时间
        Timestamp now = new Timestamp(new Date());
        
        // 生成唯一ID
        String chatId = db.collection("chat_rooms").document().getId();
        
        // 在Firestore中创建聊天室文档
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chat_id", chatId);
        chatData.put("chat_name", chatName);
        chatData.put("creator_id", currentUser.getUid());
        chatData.put("created_at", now);
        chatData.put("is_active", true);
        chatData.put("group_type", ChatGroup.TYPE_NORMAL); // 设置为普通群聊类型
        
        // 设置最后消息时间为创建时间，确保聊天室能在列表查询中显示
        chatData.put("last_message_time", now);
        chatData.put("last_message", "Group created. Welcome!");
        
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
                    
                    Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show();
                    
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
                    Toast.makeText(this, "Failed to create group: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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
                        Toast.makeText(this, "You are already a member of this group", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(this, "Successfully joined the chat room", Toast.LENGTH_SHORT).show();
                                    
                                    // 添加用户加入的系统消息
                                    addUserJoinMessage(chatId);
                                    
                                    // 进入聊天页面
                                    openChatActivity(chatId, chatName);
                                });
                        });
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to join chat room: " + e.getMessage(), Toast.LENGTH_SHORT).show()
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
        message.put("text", finalUserName + " joined the chat room");
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
                            "last_message", finalUserName + " joined the chat room",
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
                Toast.makeText(this, "Failed to add member: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }

    /**
     * 为新创建的群聊添加初始系统消息
     * @param chatId 聊天室ID
     */
    private void addInitialMessage(String chatId) {
        // 组装消息数据
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "system");
        message.put("text", "Group created. Welcome!");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        // 保存消息到Firestore
        db.collection("chat_rooms").document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // 消息添加成功，更新聊天室消息计数
                updateChatRoomMessageCount(chatId);
            });
    }
    
    /**
     * 为课程群聊添加初始系统消息
     * @param chatId 聊天室ID
     * @param courseName 课程名称
     */
    private void addCourseInitialMessage(String chatId, String courseName) {
        // 组装消息数据
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "system");
        message.put("text", "Course chat for " + courseName + " created. Welcome!");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        // 保存消息到Firestore
        db.collection("chat_rooms").document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // 消息添加成功，更新聊天室消息计数
                updateChatRoomMessageCount(chatId);
            });
    }
    
    /**
     * 为测试群聊添加初始消息
     * @param chatId 聊天室ID
     */
    private void addTestInitialMessage(String chatId) {
        // 组装消息数据
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "system");
        message.put("text", "Test Group created. Welcome!");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        // 保存消息到Firestore
        db.collection("chat_rooms").document(chatId)
            .collection("messages")
            .add(message);
        
        // 添加一条测试消息
        Map<String, Object> testMessage = new HashMap<>();
        testMessage.put("senderId", currentUser.getUid());
        testMessage.put("text", "Hello everyone, this is a test message.");
        testMessage.put("timestamp", new Timestamp(new Date()));
        testMessage.put("type", "text");
        
        db.collection("chat_rooms").document(chatId)
            .collection("messages")
            .add(testMessage)
            .addOnSuccessListener(documentReference -> {
                // 消息添加成功，更新聊天室消息计数和最后消息
                db.collection("chat_rooms").document(chatId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long currentCount = documentSnapshot.getLong("message_count");
                            if (currentCount == null) currentCount = 0L;
                            
                            documentSnapshot.getReference().update(
                                "message_count", currentCount + 2,  // 两条消息
                                "last_message", "Hello everyone, this is a test message.",
                                "last_message_time", new Timestamp(new Date())
                            );
                        }
                    });
            });
    }
    
    /**
     * 更新聊天室消息计数
     * @param chatId 聊天室ID
     */
    private void updateChatRoomMessageCount(String chatId) {
        db.collection("chat_rooms").document(chatId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long currentCount = documentSnapshot.getLong("message_count");
                    if (currentCount == null) currentCount = 0L;
                    
                    documentSnapshot.getReference().update(
                        "message_count", currentCount + 1
                    );
                }
            });
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
            
            // 创建课程群聊
            createCourseChatGroup(course, section, semester);
        }
    }
    
    /**
     * 创建课程群聊（测试用）
     * @param courseId 课程ID
     * @param courseClass 课程班级
     * @param courseSemester 课程学期
     */
    private void createCourseChatGroup(String courseId, String courseClass, String courseSemester) {
        // 生成聊天室名称 格式: 课程-班级-学期
        String chatName = courseId + "-" + courseClass + "-" + courseSemester;
        
        // 获取当前时间
        Timestamp now = new Timestamp(new Date());
        
        // 生成唯一ID
        String chatId = db.collection("chat_rooms").document().getId();
        
        // 在Firestore中创建聊天室文档
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chat_id", chatId);
        chatData.put("chat_name", chatName);
        chatData.put("courseId", courseId);
        chatData.put("courseClass", courseClass);
        chatData.put("courseSemester", courseSemester);
        chatData.put("creator_id", "system"); // 系统创建，没有群主
        chatData.put("created_at", now);
        chatData.put("is_active", true);
        chatData.put("group_type", ChatGroup.TYPE_COURSE); // 设置为课程群聊类型
        
        // 设置最后消息时间为创建时间，确保聊天室能在列表查询中显示
        chatData.put("last_message_time", now);
        chatData.put("last_message", "Course chat for " + chatName + " created. Welcome!");
        
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
                    
                    Toast.makeText(this, "测试课程群聊创建成功", Toast.LENGTH_SHORT).show();
                    
                    // 添加一条初始消息
                    addTestInitialMessage(chatId);
                    
                    // 刷新适配器
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "创建测试群聊失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * 显示聊天选项菜单
     * @param group 聊天群组
     * @param isCreator 是否为创建者
     */
    private void showChatOptions(ChatGroup group, boolean isCreator) {
        ArrayList<String> options = new ArrayList<>();
        
        // 根据群聊类型和用户权限添加选项
        if (group.isNormalGroup() && isCreator) {
            // 普通群聊且是群主
            options.add("Manage Group");
            options.add("Disband Group");
        } else if (group.isCourseGroup()) {
            // 课程群聊
            options.add("Leave Group");
        } else if (group.isTradeGroup()) {
            // 购物聊天
            options.add("Delete Chat");
        } else {
            // 普通群聊非群主
            options.add("Leave Group");
        }
        
        // 显示菜单
        new AlertDialog.Builder(this)
            .setTitle(group.getGroupName())
            .setItems(options.toArray(new String[0]), (dialog, which) -> {
                String option = options.get(which);
                switch (option) {
                    case "Manage Group":
                        // TODO: 群组管理功能
                        Toast.makeText(this, "Group management feature coming soon", Toast.LENGTH_SHORT).show();
                        break;
                    case "Disband Group":
                        confirmDismissGroup(group.getGroupId(), group.getGroupName());
                        break;
                    case "Leave Group":
                        confirmLeaveGroup(group.getGroupId(), group.getGroupName(), group.getGroupType());
                        break;
                    case "Delete Chat":
                        confirmDeleteChat(group.getGroupId(), group.getGroupName());
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * 确认解散群聊
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     */
    private void confirmDismissGroup(String chatId, String chatName) {
        new AlertDialog.Builder(this)
            .setTitle("Disband Group")
            .setMessage("Are you sure you want to disband the group \"" + chatName + "\"? This action cannot be undone.")
            .setPositiveButton("Disband", (dialog, which) -> {
                dismissGroup(chatId);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * 解散群聊
     * @param chatId 聊天室ID
     */
    private void dismissGroup(String chatId) {
        db.collection("chat_rooms").document(chatId)
            .update("is_active", false)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Group disbanded", Toast.LENGTH_SHORT).show();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to disband group: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    /**
     * 确认退出群聊
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     * @param groupType 群聊类型
     */
    private void confirmLeaveGroup(String chatId, String chatName, String groupType) {
        new AlertDialog.Builder(this)
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave the group \"" + chatName + "\"?")
            .setPositiveButton("Leave", (dialog, which) -> {
                leaveGroup(chatId, groupType);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * 退出群聊
     * @param chatId 聊天室ID
     * @param groupType 群聊类型
     */
    private void leaveGroup(String chatId, String groupType) {
        db.collection("chat_rooms").document(chatId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> memberIds = (List<String>) documentSnapshot.get("member_ids");
                    
                    if (memberIds != null && memberIds.contains(currentUser.getUid())) {
                        // 从成员列表中移除用户
                        memberIds.remove(currentUser.getUid());
                        
                        // 更新聊天室文档
                        documentSnapshot.getReference().update("member_ids", memberIds)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Left the group", Toast.LENGTH_SHORT).show();
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                                
                                // 添加用户退出的系统消息
                                addUserLeaveMessage(chatId);
                            })
                            .addOnFailureListener(e -> 
                                Toast.makeText(this, "Failed to leave group: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                    }
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Operation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    /**
     * 添加用户退出的系统消息
     * @param chatId 聊天室ID
     */
    private void addUserLeaveMessage(String chatId) {
        String userName = currentUser.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = currentUser.getEmail();
        }
        
        // 创建final变量以便在lambda表达式中使用
        final String finalUserName = userName;
        
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "system");
        message.put("text", finalUserName + " left the group");
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
                            "last_message", finalUserName + " left the group",
                            "last_message_time", new Timestamp(new Date())
                        );
                    }
                });
          });
    }
    
    /**
     * 确认删除聊天
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     */
    private void confirmDeleteChat(String chatId, String chatName) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete the chat with \"" + chatName + "\"? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                leaveGroup(chatId, ChatGroup.TYPE_TRADE);
            })
            .setNegativeButton("Cancel", null)
            .show();
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
        private final TextView tvGroupName, tvLastMessage, tvTimestamp, tvUnreadCount, tvGroupType;
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
            0xFF009688  // 青绿色
        };
        
        public ChatGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            tvGroupType = itemView.findViewById(R.id.tvGroupType);
            ivReadMark = itemView.findViewById(R.id.ivReadMark);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
        }

        public void bind(ChatGroup group, boolean isCreator) {
            tvGroupName.setText(group.getGroupName());
            
            String lastMessage = group.getLastMessage();
            if (lastMessage == null || lastMessage.isEmpty()) {
                tvLastMessage.setText("暂无消息");
            } else {
                tvLastMessage.setText(lastMessage);
            }
            
            if (group.getLastMessageTimestamp() != null) {
            tvTimestamp.setText(group.getFormattedTimestamp());
                tvTimestamp.setVisibility(View.VISIBLE);
            } else {
                tvTimestamp.setVisibility(View.GONE);
            }
            
            // 设置群聊类型标识
            if (group.isNormalGroup()) {
                if (isCreator) {
                    tvGroupType.setText("群主");
                    tvGroupType.setVisibility(View.VISIBLE);
            } else {
                    tvGroupType.setVisibility(View.GONE);
                }
            } else if (group.isCourseGroup()) {
                tvGroupType.setText("课程");
                tvGroupType.setVisibility(View.VISIBLE);
            } else if (group.isTradeGroup()) {
                tvGroupType.setText("商品");
                tvGroupType.setVisibility(View.VISIBLE);
            } else {
                tvGroupType.setVisibility(View.GONE);
            }
            
            // 设置未读消息数
            int unreadCount = group.getUnreadCount();
            if (unreadCount > 0) {
                tvUnreadCount.setText(String.valueOf(unreadCount));
                tvUnreadCount.setVisibility(View.VISIBLE);
                ivReadMark.setVisibility(View.GONE);
                } else {
                tvUnreadCount.setVisibility(View.GONE);
                ivReadMark.setVisibility(View.VISIBLE);
            }
            
            // 设置颜色标识
            String groupId = group.getGroupId();
            if (groupId != null && !groupId.isEmpty()) {
                int colorIndex = Math.abs(groupId.hashCode()) % GROUP_COLORS.length;
                colorIndicator.setBackgroundColor(GROUP_COLORS[colorIndex]);
            } else {
                colorIndicator.setBackgroundColor(GROUP_COLORS[0]);
            }
        }
    }

    /**
     * 显示筛选对话框
     */
    private void showFilterDialog() {
        // 创建对话框视图
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_chat, null);
        
        // 初始化聊天类型筛选选项
        RadioGroup rgChatType = dialogView.findViewById(R.id.rgChatType);
        RadioButton rbAllChats = dialogView.findViewById(R.id.rbAllChats);
        RadioButton rbCourseChats = dialogView.findViewById(R.id.rbCourseChats);
        RadioButton rbTradeChats = dialogView.findViewById(R.id.rbTradeChats);
        RadioButton rbNormalChats = dialogView.findViewById(R.id.rbNormalChats);
        
        // 初始化时间筛选选项
        RadioGroup rgTimeFilter = dialogView.findViewById(R.id.rgTimeFilter);
        RadioButton rbAllTime = dialogView.findViewById(R.id.rbAllTime);
        RadioButton rbToday = dialogView.findViewById(R.id.rbToday);
        RadioButton rbThreeDays = dialogView.findViewById(R.id.rbThreeDays);
        RadioButton rbOneWeek = dialogView.findViewById(R.id.rbOneWeek);
        
        // 初始化消息状态筛选选项
        RadioGroup rgStatusFilter = dialogView.findViewById(R.id.rgStatusFilter);
        RadioButton rbAllStatus = dialogView.findViewById(R.id.rbAllStatus);
        RadioButton rbUnreadOnly = dialogView.findViewById(R.id.rbUnreadOnly);
        
        // 设置当前筛选状态
        // 聊天类型
        if (currentChatTypeFilter == null) {
            rbAllChats.setChecked(true);
        } else if (ChatGroup.TYPE_COURSE.equals(currentChatTypeFilter)) {
            rbCourseChats.setChecked(true);
        } else if (ChatGroup.TYPE_TRADE.equals(currentChatTypeFilter)) {
            rbTradeChats.setChecked(true);
        } else if (ChatGroup.TYPE_NORMAL.equals(currentChatTypeFilter)) {
            rbNormalChats.setChecked(true);
        }
        
        // 时间筛选
        switch (currentTimeFilter) {
            case 0:
                rbAllTime.setChecked(true);
                break;
            case 1:
                rbToday.setChecked(true);
                break;
            case 2:
                rbThreeDays.setChecked(true);
                break;
            case 3:
                rbOneWeek.setChecked(true);
                break;
        }
        
        // 消息状态筛选
        if (showUnreadOnly) {
            rbUnreadOnly.setChecked(true);
        } else {
            rbAllStatus.setChecked(true);
        }
        
        // 初始化按钮
        Button btnReset = dialogView.findViewById(R.id.btnReset);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnApply = dialogView.findViewById(R.id.btnApply);
        
        // 创建并显示对话框（使用自定义布局，不使用AlertDialog的按钮）
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // 设置按钮点击事件
        btnReset.setOnClickListener(v -> {
            // 重置所有筛选条件
            currentChatTypeFilter = null;
            currentTimeFilter = 0;
            showUnreadOnly = false;
            loadChatGroups();
            dialog.dismiss();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnApply.setOnClickListener(v -> {
            // 获取聊天类型筛选
            int chatTypeId = rgChatType.getCheckedRadioButtonId();
            if (chatTypeId == R.id.rbAllChats) {
                currentChatTypeFilter = null;
            } else if (chatTypeId == R.id.rbCourseChats) {
                currentChatTypeFilter = ChatGroup.TYPE_COURSE;
            } else if (chatTypeId == R.id.rbTradeChats) {
                currentChatTypeFilter = ChatGroup.TYPE_TRADE;
            } else if (chatTypeId == R.id.rbNormalChats) {
                currentChatTypeFilter = ChatGroup.TYPE_NORMAL;
            }
            
            // 获取时间筛选
            int timeId = rgTimeFilter.getCheckedRadioButtonId();
            if (timeId == R.id.rbAllTime) {
                currentTimeFilter = 0;
            } else if (timeId == R.id.rbToday) {
                currentTimeFilter = 1;
            } else if (timeId == R.id.rbThreeDays) {
                currentTimeFilter = 2;
            } else if (timeId == R.id.rbOneWeek) {
                currentTimeFilter = 3;
            }
            
            // 获取消息状态筛选
            int statusId = rgStatusFilter.getCheckedRadioButtonId();
            showUnreadOnly = (statusId == R.id.rbUnreadOnly);
            
            // 应用筛选并刷新列表
            loadChatGroups();
            dialog.dismiss();
        });
        
        dialog.show();
    }
}