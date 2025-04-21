package hk.hku.cs.hkuers.features.chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hk.hku.cs.hkuers.MainActivity;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.courses.CourseSearchActivity;
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;
import hk.hku.cs.hkuers.features.profile.UserProfileActivity;

public class MemberListActivity extends AppCompatActivity {

    private static final String TAG = "MemberListActivity";
    private static final int BATCH_SIZE = 15; // 每次加载的成员数量
    private static final String SERVER_URL = "http://10.0.2.2:9000"; // 统一服务器URL

    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    
    // UI组件
    private Toolbar toolbarLayout;
    private TextView tvTitle;
    private TextView tvMemberCount;
    private RecyclerView rvMembers;
    private Button btnLoadMore;
    private FloatingActionButton fabAddMember;
    private ProgressBar progressBar;
    private TextView tvEmptyList;
    private BottomNavigationView bottomNavigation;
    
    // 数据
    private String chatRoomId;
    private String chatRoomName;
    private String chatRoomColor;
    private String ownerId;
    private List<Member> memberList;
    private MemberAdapter memberAdapter;
    private DocumentSnapshot lastVisible; // 用于分页查询
    private boolean isLoading = false;
    private boolean hasMoreMembers = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_list);
        
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
                android.util.Log.d(TAG, "设置状态栏高度: " + statusBarHeight);
            }
        }
        
        // 初始化Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "您需要登录才能查看成员列表", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 获取聊天室ID
        chatRoomId = getIntent().getStringExtra("chat_room_id");
        // 同时获取聊天室名称(可能为null)
        chatRoomName = getIntent().getStringExtra("chatRoomName");
        
        // 记录日志
        android.util.Log.d(TAG, "onCreate: 接收到参数 chatRoomId=" + chatRoomId + ", chatRoomName=" + chatRoomName);
        
        if (chatRoomId == null || chatRoomId.isEmpty()) {
            Toast.makeText(this, "无效的聊天室ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化UI组件
        initViews();
        
        // 加载聊天室信息
        loadChatRoomInfo();
    }

    private void initViews() {
        toolbarLayout = findViewById(R.id.toolbar);
        tvTitle = findViewById(R.id.tvTitle);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        rvMembers = findViewById(R.id.rv_members);
        btnLoadMore = findViewById(R.id.btn_load_more);
        fabAddMember = findViewById(R.id.fab_add_member);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyList = findViewById(R.id.tv_empty_list);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        
        // 设置返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> returnToChatRoom());
        
        // 设置成员列表
        memberList = new ArrayList<>();
        memberAdapter = new MemberAdapter(this, memberList);
        rvMembers.setAdapter(memberAdapter);
        rvMembers.setLayoutManager(new GridLayoutManager(this, 4));
        
        // 设置加载更多按钮
        btnLoadMore.setOnClickListener(v -> loadMoreMembers());
        
        // 设置底部导航栏
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        // 设置选中Chat选项
        bottomNavigation.setSelectedItemId(R.id.navigation_chat);
        
        // 设置导航点击监听
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_chat) {
                // 返回聊天列表
                safeNavigateTo(ChatListActivity.class);
                return true;
            } else if (itemId == R.id.navigation_forum) {
                Toast.makeText(this, "Forum feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.navigation_dashboard) {
                safeNavigateTo(MainActivity.class);
                return true;
            } else if (itemId == R.id.navigation_courses) {
                safeNavigateTo(CourseSearchActivity.class);
                return true;
            } else if (itemId == R.id.navigation_marketplace) {
                safeNavigateTo(MarketplaceActivity.class);
                return true;
            }
            return false;
        });
    }

    // 添加安全的导航方法
    private <T extends AppCompatActivity> void safeNavigateTo(Class<T> targetActivity) {
        // 已经在结束中，避免重复调用
        if (isFinishing()) return;
        
        // 延迟一点时间，让Firebase回调有时间处理
        new Handler().postDelayed(() -> {
            try {
                Intent intent = new Intent(MemberListActivity.this, targetActivity);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // 设置平滑过渡
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                // 非立即结束，让过渡动画有时间执行
                new Handler().postDelayed(() -> finish(), 100);
            } catch (Exception e) {
                android.util.Log.e(TAG, "导航到" + targetActivity.getSimpleName() + "失败: " + e.getMessage());
            }
        }, 200);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void loadChatRoomInfo() {
        showLoading(true);
        
        DocumentReference chatRoomRef = db.collection("chat_rooms").document(chatRoomId);
        chatRoomRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // 记录整个数据，用于调试
                    android.util.Log.d(TAG, "聊天室数据: " + document.getData());
                    
                    // 获取聊天室信息，兼容不同的字段名
                    if (document.contains("chat_name")) {
                        chatRoomName = document.getString("chat_name");
                        android.util.Log.d(TAG, "从chat_name字段获取聊天室名称: " + chatRoomName);
                    } else if (document.contains("name")) {
                        chatRoomName = document.getString("name");
                        android.util.Log.d(TAG, "从name字段获取聊天室名称: " + chatRoomName);
                    }
                    
                    // 获取颜色代码
                    chatRoomColor = document.getString("color");
                    if (chatRoomColor == null) {
                        chatRoomColor = document.getString("color_code");
                    }
                    android.util.Log.d(TAG, "获取到的颜色代码: " + chatRoomColor);
                    
                    // 设置颜色指示条
                    View colorIndicator = findViewById(R.id.colorIndicator);
                    if (colorIndicator != null) {
                        if (chatRoomColor != null && !chatRoomColor.isEmpty()) {
                            try {
                                int color = android.graphics.Color.parseColor(chatRoomColor);
                                colorIndicator.setBackgroundColor(color);
                                android.util.Log.d(TAG, "设置颜色指示条颜色: " + chatRoomColor);
                            } catch (Exception e) {
                                android.util.Log.e(TAG, "解析颜色代码失败: " + e.getMessage(), e);
                                // 使用默认颜色
                                setDefaultColorIndicator(colorIndicator);
                            }
                        } else {
                            // 如果没有设置颜色，使用基于chatRoomId的默认颜色
                            setDefaultColorIndicator(colorIndicator);
                        }
                    }
                    
                    // 获取所有者ID
                    ownerId = document.getString("ownerId");
                    if (ownerId == null) {
                        ownerId = document.getString("creator_id");
                    }
                    
                    // 设置标题
                    tvTitle.setText(chatRoomName != null ? chatRoomName : "群成员");
                    
                    // 加载成员列表
                    loadInitialMembers();
                } else {
                    Toast.makeText(MemberListActivity.this, "找不到聊天室信息", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(MemberListActivity.this, "加载聊天室信息失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            android.util.Log.e(TAG, "加载聊天室数据失败: " + e.getMessage(), e);
            Toast.makeText(MemberListActivity.this, "加载聊天室数据失败", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // 设置默认的颜色指示条，基于聊天室ID
    private void setDefaultColorIndicator(View colorIndicator) {
        if (chatRoomId != null && !chatRoomId.isEmpty() && colorIndicator != null) {
            try {
                // 使用ChatListActivity中定义的GROUP_COLORS数组
                Class<?> chatListViewHolderClass = Class.forName("hk.hku.cs.hkuers.features.chat.ChatListActivity$ChatGroupViewHolder");
                java.lang.reflect.Field colorsField = chatListViewHolderClass.getDeclaredField("GROUP_COLORS");
                colorsField.setAccessible(true);
                int[] colors = (int[]) colorsField.get(null);
                
                if (colors != null && colors.length > 0) {
                    int colorIndex = Math.abs(chatRoomId.hashCode()) % colors.length;
                    int color = colors[colorIndex];
                    colorIndicator.setBackgroundColor(color);
                    android.util.Log.d(TAG, "设置默认颜色指示条");
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "设置默认颜色指示条失败: " + e.getMessage(), e);
                // 反射失败时使用一个固定颜色
                colorIndicator.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"));
            }
        }
    }

    private void loadInitialMembers() {
        if (isLoading) return;
        isLoading = true;
        
        // 清空现有列表
        memberList.clear();
        memberAdapter.notifyDataSetChanged();
        lastVisible = null;
        
        // 获取成员列表
        loadMembers(true);
    }

    private void loadMoreMembers() {
        if (isLoading || !hasMoreMembers) return;
        loadMembers(false);
    }

    private void loadMembers(boolean isInitialLoad) {
        isLoading = true;
        showLoading(true);
        Log.d(TAG, "开始加载成员数据，chatRoomId=" + chatRoomId);
        
        // 直接查询document，检查chat_room的结构
        db.collection("chat_rooms").document(chatRoomId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // 打印所有字段，以便调试
                    Map<String, Object> data = documentSnapshot.getData();
                    if (data != null) {
                        Log.d(TAG, "聊天室数据: " + data.toString());
                        
                        // 根据聊天室数据结构，尝试不同的字段名获取成员列表
                        List<String> memberIds = null;
                        
                        // 尝试不同的字段名
                        if (data.containsKey("member_ids")) {
                            memberIds = (List<String>) data.get("member_ids");
                            Log.d(TAG, "找到member_ids字段");
                        } else if (data.containsKey("members")) {
                            memberIds = (List<String>) data.get("members");
                            Log.d(TAG, "找到members字段");
                        } else if (data.containsKey("memberIds")) {
                            memberIds = (List<String>) data.get("memberIds");
                            Log.d(TAG, "找到memberIds字段");
                        } else if (data.containsKey("users")) {
                            memberIds = (List<String>) data.get("users");
                            Log.d(TAG, "找到users字段");
                        }
                        
                        // 如果memberIds为null，尝试将users字段(Map)转换为List
                        if (memberIds == null && data.containsKey("users") && data.get("users") instanceof Map) {
                            Map<String, Object> usersMap = (Map<String, Object>) data.get("users");
                            memberIds = new ArrayList<>(usersMap.keySet());
                            Log.d(TAG, "将users Map转换为List: " + memberIds.size() + "个成员");
                        }
                        
                        // 如果找到了成员列表
                        if (memberIds != null && !memberIds.isEmpty()) {
                            final List<String> finalMemberIds = memberIds;
                            Log.d(TAG, "找到" + memberIds.size() + "个成员ID: " + memberIds.toString());
                            tvMemberCount.setText(String.format("共%d人", memberIds.size()));
                            
                            // 获取成员信息
                            int startIndex = 0;
                            if (!isInitialLoad && lastVisible != null) {
                                // 非首次加载，从上次的位置开始
                                int lastIndex = finalMemberIds.indexOf(lastVisible.getId());
                                if (lastIndex != -1 && lastIndex + 1 < finalMemberIds.size()) {
                                    startIndex = lastIndex + 1;
                                }
                            }
                            
                            // 计算结束索引
                            int endIndex = Math.min(startIndex + BATCH_SIZE, finalMemberIds.size());
                            List<String> currentBatch = new ArrayList<>(finalMemberIds.subList(startIndex, endIndex));
                            
                            // 是否还有更多成员
                            hasMoreMembers = endIndex < finalMemberIds.size();
                            btnLoadMore.setVisibility(hasMoreMembers ? View.VISIBLE : View.GONE);
                            
                            // 查询这一批用户的详细信息
                            if (!currentBatch.isEmpty()) {
                                Log.d(TAG, "加载成员批次: " + currentBatch.toString());
                                fetchUsers(currentBatch, isInitialLoad);
                                
                                // 记住最后一个ID
                                String lastId = currentBatch.get(currentBatch.size() - 1);
                                db.collection("users").document(lastId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        lastVisible = userDoc;
                                        Log.d(TAG, "设置lastVisible为: " + lastId);
                                    });
                            } else {
                                // 没有更多成员
                                showLoading(false);
                                updateEmptyView();
                                isLoading = false;
                            }
                        } else {
                            // 成员列表为空或未找到
                            Log.d(TAG, "未找到成员列表或成员列表为空");
                            tvMemberCount.setText("共0人");
                            showLoading(false);
                            updateEmptyView();
                            isLoading = false;
                        }
                    } else {
                        Log.e(TAG, "聊天室数据为null");
                        showLoading(false);
                        updateEmptyView();
                        isLoading = false;
                    }
                } else {
                    // 聊天室不存在
                    Log.e(TAG, "聊天室不存在");
                    Toast.makeText(MemberListActivity.this, "找不到聊天室", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    updateEmptyView();
                    isLoading = false;
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "加载聊天室失败: " + e.getMessage());
                Toast.makeText(MemberListActivity.this, "加载成员列表失败", Toast.LENGTH_SHORT).show();
                showLoading(false);
                updateEmptyView();
                isLoading = false;
            });
    }

    private void fetchUsers(List<String> userIds, boolean isFirstBatch) {
        // 如果是第一批并且需要清空列表
        if (isFirstBatch) {
            memberList.clear();
            memberAdapter.notifyDataSetChanged();
        }
        
        // 批量获取用户信息
        if (userIds.size() <= 10) {
            // 如果ID数量少于等于10个，可以一次性查询
            fetchUsersBatch(userIds);
        } else {
            // 如果ID数量过多，分批次查询
            List<String> batch = new ArrayList<>();
            for (int i = 0; i < userIds.size(); i++) {
                batch.add(userIds.get(i));
                if (batch.size() == 10 || i == userIds.size() - 1) {
                    fetchUsersBatch(new ArrayList<>(batch));
                    batch.clear();
                }
            }
        }
    }

    private void fetchUsersBatch(List<String> userIds) {
        Log.d(TAG, "查询用户数据，数量: " + userIds.size());
        
        // 尝试收集所有可能匹配用户ID的字段
        List<String> possibleIdFields = new ArrayList<>();
        possibleIdFields.add("uid");
        possibleIdFields.add("id");
        possibleIdFields.add("userId");
        possibleIdFields.add("user_id");
        
        // 使用in查询查找可能包含这些用户ID的文档
        db.collection("users")
            .whereIn(possibleIdFields.get(0), userIds)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    int resultSize = task.getResult().size();
                    Log.d(TAG, "使用" + possibleIdFields.get(0) + "字段获取到 " + resultSize + " 个用户数据");
                    
                    if (resultSize > 0) {
                        // 处理获取到的用户数据
                        processUserData(task.getResult().getDocuments());
                    } else if (possibleIdFields.size() > 1) {
                        // 尝试使用下一个可能的ID字段
                        String nextField = possibleIdFields.get(1);
                        possibleIdFields.remove(0);
                        Log.d(TAG, "尝试使用另一个字段: " + nextField);
                        
                        // 直接使用用户ID作为文档ID查询
                        List<DocumentReference> userRefs = new ArrayList<>();
                        for (String userId : userIds) {
                            userRefs.add(db.collection("users").document(userId));
                        }
                        
                        // 批量获取
                        if (!userRefs.isEmpty()) {
                            db.runTransaction(transaction -> {
                                List<DocumentSnapshot> userDocs = new ArrayList<>();
                                for (DocumentReference ref : userRefs) {
                                    DocumentSnapshot doc = transaction.get(ref);
                                    if (doc.exists()) {
                                        userDocs.add(doc);
                                    }
                                }
                                return userDocs;
                            }).addOnSuccessListener(userDocs -> {
                                Log.d(TAG, "通过文档ID直接获取到 " + userDocs.size() + " 个用户数据");
                                if (!userDocs.isEmpty()) {
                                    processUserData(userDocs);
                                } else {
                                    showNoDataMessage();
                                }
                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "批量获取用户失败: " + e.getMessage());
                                showNoDataMessage();
                            });
                        } else {
                            showNoDataMessage();
                        }
                    } else {
                        showNoDataMessage();
                    }
                } else {
                    Log.e(TAG, "获取用户详情失败: " + task.getException());
                    showNoDataMessage();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "批量获取用户失败: " + e.getMessage());
                showNoDataMessage();
            });
    }
    
    private void processUserData(List<DocumentSnapshot> userDocs) {
        // 查找聊天室所有者ID
        db.collection("chat_rooms").document(chatRoomId)
            .get()
            .addOnSuccessListener(chatRoomDoc -> {
                // 尝试多个可能的所有者ID字段名
                String ownerId = null;
                if (chatRoomDoc.contains("creator_id")) {
                    ownerId = chatRoomDoc.getString("creator_id");
                } else if (chatRoomDoc.contains("ownerId")) {
                    ownerId = chatRoomDoc.getString("ownerId");
                } else if (chatRoomDoc.contains("owner_id")) {
                    ownerId = chatRoomDoc.getString("owner_id");
                }
                
                final String finalOwnerId = ownerId;
                Log.d(TAG, "聊天室所有者ID: " + finalOwnerId);
                
                // 处理用户数据
                for (DocumentSnapshot userDoc : userDocs) {
                    Member member = new Member();
                    
                    // 获取用户ID (可能在不同字段中)
                    String userId = userDoc.getId(); // 默认使用文档ID
                    
                    // 尝试查找可能包含userId的字段
                    if (userDoc.contains("uid")) {
                        userId = userDoc.getString("uid");
                    } else if (userDoc.contains("id")) {
                        userId = userDoc.getString("id");
                    } else if (userDoc.contains("userId")) {
                        userId = userDoc.getString("userId");
                    }
                    
                    // 设置基本属性
                    member.setUserId(userId);
                    
                    // 尝试获取用户名 (可能在不同字段中)
                    String username = null;
                    if (userDoc.contains("uname")) {
                        username = userDoc.getString("uname");
                    } else if (userDoc.contains("username")) {
                        username = userDoc.getString("username");
                    } else if (userDoc.contains("name")) {
                        username = userDoc.getString("name");
                    } else if (userDoc.contains("displayName")) {
                        username = userDoc.getString("displayName");
                    }
                    
                    // 如果没有用户名，生成默认用户名（使用邮箱前缀或ID的前6位）
                    if (username == null || username.isEmpty()) {
                        String email = userDoc.getString("email");
                        if (email != null && email.contains("@")) {
                            username = email.substring(0, email.indexOf('@'));
                        } else {
                            // 使用用户ID的前6位作为默认名
                            username = "用户" + userId.substring(0, Math.min(6, userId.length()));
                        }
                    }
                    member.setUsername(username);
                    
                    // 尝试获取头像URL
                    String avatarUrl = userDoc.getString("avatar_url");
                    member.setAvatarUrl(avatarUrl);
                    
                    // 设置是否为群主
                    member.setIsOwner(finalOwnerId != null && finalOwnerId.equals(userId));
                    
                    // 设置额外属性
                    if (userDoc.contains("email")) {
                        member.setEmail(userDoc.getString("email"));
                    }
                    if (userDoc.contains("department")) {
                        member.setDepartment(userDoc.getString("department"));
                    }
                    // 新增加的字段
                    if (userDoc.contains("programme")) {
                        member.setProgramme(userDoc.getString("programme"));
                    }
                    if (userDoc.contains("year_of_entry")) {
                        member.setYearOfEntry(userDoc.getString("year_of_entry"));
                    }
                    if (userDoc.contains("signature")) {
                        member.setSignature(userDoc.getString("signature"));
                    }
                    
                    // 默认加入时间
                    member.setJoinedAt(System.currentTimeMillis());
                    
                    // 添加到列表
                    memberList.add(member);
                    Log.d(TAG, "添加成员: " + member.getUsername() + ", ID: " + userId + ", 头像: " + avatarUrl);
                }
                
                // 按群主和加入时间排序
                if (!memberList.isEmpty()) {
                    memberList.sort((m1, m2) -> {
                        // 群主总是排在最前面
                        if (m1.isOwner()) return -1;
                        if (m2.isOwner()) return 1;
                        // 按加入时间排序
                        return Long.compare(m1.getJoinedAt(), m2.getJoinedAt());
                    });
                }
                
                // 更新适配器
                memberAdapter.notifyDataSetChanged();
                updateEmptyView();
                showLoading(false);
                isLoading = false;
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "获取聊天室所有者失败: " + e.getMessage());
                
                // 即使失败也要更新UI
                memberAdapter.notifyDataSetChanged();
                updateEmptyView();
                showLoading(false);
                isLoading = false;
            });
    }
    
    private void showNoDataMessage() {
        Toast.makeText(MemberListActivity.this, "无法加载成员信息", Toast.LENGTH_SHORT).show();
        updateEmptyView();
        showLoading(false);
        isLoading = false;
    }

    // 在onPause方法中添加输入法管理器的清理
    @Override
    protected void onPause() {
        try {
            android.util.Log.d(TAG, "onPause: Activity暂停");
            // 隐藏键盘并清理输入管理器引用
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "onPause处理异常: " + e.getMessage());
        }
        super.onPause();
    }

    // 在onDestroy方法中添加输入法管理器的清理
    @Override
    protected void onDestroy() {
        try {
            android.util.Log.d(TAG, "onDestroy: Activity销毁");
            // 释放资源，避免内存泄漏
            memberList = null;
            memberAdapter = null;
            
            // 确保输入法管理器资源被清理
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "onDestroy处理异常: " + e.getMessage());
        }
        super.onDestroy();
    }

    // 修复返回按钮处理
    @Override
    public void onBackPressed() {
        returnToChatRoom();
    }
    
    private void returnToChatRoom() {
        // 如果有聊天室ID，返回到对应的聊天室
        if (chatRoomId != null && !chatRoomId.isEmpty()) {
            Intent intent = new Intent(this, ChatRoomActivity.class);
            intent.putExtra("chatRoomId", chatRoomId);
            if (chatRoomName != null && !chatRoomName.isEmpty()) {
                intent.putExtra("chatRoomName", chatRoomName);
            }
            startActivity(intent);
            finish();
        } else {
            // 否则返回到聊天列表
            Intent intent = new Intent(this, ChatListActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void updateEmptyView() {
        if (memberList.isEmpty()) {
            tvEmptyList.setVisibility(View.VISIBLE);
            rvMembers.setVisibility(View.GONE);
        } else {
            tvEmptyList.setVisibility(View.GONE);
            rvMembers.setVisibility(View.VISIBLE);
        }
    }

    // 成员数据模型
    public static class Member {
        private String userId;
        private String username;
        private String avatarUrl;
        private boolean isOwner;
        private String email;
        private String department;
        private String programme;
        private String yearOfEntry;
        private String signature;
        private long joinedAt;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public boolean isOwner() {
            return isOwner;
        }

        public void setIsOwner(boolean owner) {
            isOwner = owner;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getDepartment() {
            return department;
        }
        
        public void setDepartment(String department) {
            this.department = department;
        }
        
        public String getProgramme() {
            return programme;
        }
        
        public void setProgramme(String programme) {
            this.programme = programme;
        }
        
        public String getYearOfEntry() {
            return yearOfEntry;
        }
        
        public void setYearOfEntry(String yearOfEntry) {
            this.yearOfEntry = yearOfEntry;
        }
        
        public String getSignature() {
            return signature;
        }
        
        public void setSignature(String signature) {
            this.signature = signature;
        }
        
        public long getJoinedAt() {
            return joinedAt;
        }
        
        public void setJoinedAt(long joinedAt) {
            this.joinedAt = joinedAt;
        }
        
        // 辅助方法
        public String getDisplayName() {
            return username != null ? username : "未知用户";
        }
    }

    // 成员适配器
    public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {
        private final List<Member> members;
        private final MemberListActivity context;

        public MemberAdapter(MemberListActivity context, List<Member> members) {
            this.context = context;
            this.members = members;
        }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_member, parent, false);
            return new MemberViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            Member member = members.get(position);
            
            // 设置用户名
            holder.tvUsername.setText(member.getDisplayName());
            
            // 设置头像
            String avatarUrl = member.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
                Log.d(TAG, "加载头像URL: " + avatarUrl);
                
                // 确保头像URL正确（添加avatar/前缀如果没有）
                if (!avatarUrl.startsWith("avatar/")) {
                    avatarUrl = "avatar/" + avatarUrl;
                }
                
                // 构建完整URL，使用与ProfileActivity相同的逻辑
                String imageUrl = SERVER_URL + "/image/" + avatarUrl;
                // 添加随机参数避免缓存问题
                String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
                imageUrl = imageUrl + "?nocache=" + uniqueParam;
                
                Log.d(TAG, "完整头像URL: " + imageUrl);
                
                // 加载网络图片
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .centerCrop()
                        .into(holder.ivAvatar);
            } else {
                // 使用默认头像
                holder.ivAvatar.setImageResource(R.drawable.default_avatar);
            }
            
            // 设置群主标识
            boolean isOwner = member.isOwner();
            holder.ivOwnerBadge.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            
            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                // 跳转到用户资料页面
                Intent intent = new Intent(context, UserProfileActivity.class);
                
                // 传递用户基本信息
                intent.putExtra("user_id", member.getUserId());
                
                // 传递聊天室返回信息，用于正确处理返回逻辑
                intent.putExtra("from_chat_room", "true");
                intent.putExtra("chat_room_id", chatRoomId);
                intent.putExtra("chat_room_name", chatRoomName);
                
                // 传递已有的用户数据，避免重复查询
                if (member.getUsername() != null) {
                    intent.putExtra("user_name", member.getUsername());
                }
                if (member.getEmail() != null) {
                    intent.putExtra("user_email", member.getEmail());
                }
                if (member.getDepartment() != null) {
                    intent.putExtra("user_department", member.getDepartment());
                }
                if (member.getProgramme() != null) {
                    intent.putExtra("user_programme", member.getProgramme());
                }
                if (member.getYearOfEntry() != null) {
                    intent.putExtra("user_year_of_entry", member.getYearOfEntry());
                }
                if (member.getSignature() != null) {
                    intent.putExtra("user_signature", member.getSignature());
                }
                if (member.getAvatarUrl() != null) {
                    intent.putExtra("user_avatar_url", member.getAvatarUrl());
                }
                
                // 启动Activity
                context.startActivity(intent);
                
                // 设置过渡动画
                context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        class MemberViewHolder extends RecyclerView.ViewHolder {
            TextView tvUsername;
            de.hdodenhof.circleimageview.CircleImageView ivAvatar;
            android.widget.ImageView ivOwnerBadge;

            MemberViewHolder(View itemView) {
                super(itemView);
                tvUsername = itemView.findViewById(R.id.tv_username);
                ivAvatar = itemView.findViewById(R.id.iv_avatar);
                ivOwnerBadge = itemView.findViewById(R.id.iv_owner_badge);
            }
        }
    }
} 