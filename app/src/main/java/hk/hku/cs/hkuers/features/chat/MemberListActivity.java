package hk.hku.cs.hkuers.features.chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.profile.UserProfileActivity;
import hk.hku.cs.hkuers.features.profile.UserProfileManager;

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
        
        // ===== 强制输出日志，用于调试 =====
        // 设置日志过滤器为显示所有日志
        try {
            String logcatCmd = "logcat -c"; // 清空日志
            Runtime.getRuntime().exec(logcatCmd);
            
            // 强制输出一些日志消息
            android.util.Log.e(TAG, "===== 群成员列表页面启动 - ERROR级别 =====");
            android.util.Log.w(TAG, "===== 群成员列表页面启动 - WARN级别 =====");
            android.util.Log.i(TAG, "===== 群成员列表页面启动 - INFO级别 =====");
            android.util.Log.d(TAG, "===== 群成员列表页面启动 - DEBUG级别 =====");
            android.util.Log.v(TAG, "===== 群成员列表页面启动 - VERBOSE级别 =====");
            
            // 打印在控制台
            System.out.println("===== 群成员列表页面启动 - System.out =====");
            System.err.println("===== 群成员列表页面启动 - System.err =====");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
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
            Toast.makeText(this, "You need to login to view the member list", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Invalid chat room ID", Toast.LENGTH_SHORT).show();
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
        
        // 设置调试日志模式（长按标题可以触发）
        tvTitle.setOnLongClickListener(v -> {
            testLogcat();
            return true;
        });
        
        // 设置返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> returnToChatRoom());
        
        // 设置成员列表
        memberList = new ArrayList<>();
        memberAdapter = new MemberAdapter(this, memberList);
        rvMembers.setAdapter(memberAdapter);
        rvMembers.setLayoutManager(new GridLayoutManager(this, 4));
        
        // 设置加载更多按钮
        btnLoadMore.setOnClickListener(v -> loadMoreMembers());
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
                    Toast.makeText(MemberListActivity.this, "Chat room information not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                android.util.Log.e(TAG, "加载聊天室信息失败: " + task.getException());
                Toast.makeText(MemberListActivity.this, "Failed to load chat room information", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            android.util.Log.e(TAG, "加载聊天室数据失败: " + e.getMessage(), e);
            Toast.makeText(MemberListActivity.this, "Failed to load chat room data", Toast.LENGTH_SHORT).show();
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
        Log.i(TAG, "开始加载成员数据，chatRoomId=" + chatRoomId);
        
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
                        
                        // ===== 调试：打印所有顶级字段 =====
                        StringBuilder keys = new StringBuilder("聊天室包含的字段: ");
                        for (String key : data.keySet()) {
                            keys.append(key).append("(").append(data.get(key).getClass().getSimpleName()).append("), ");
                        }
                        Log.i(TAG, keys.toString());
                        
                        // 尝试不同的字段名
                        if (data.containsKey("member_ids") && data.get("member_ids") instanceof List) {
                            memberIds = (List<String>) data.get("member_ids");
                            Log.i(TAG, "找到member_ids字段, 成员数量: " + memberIds.size());
                        } else if (data.containsKey("members") && data.get("members") instanceof List) {
                            memberIds = (List<String>) data.get("members");
                            Log.i(TAG, "找到members字段, 成员数量: " + memberIds.size());
                        } else if (data.containsKey("memberIds") && data.get("memberIds") instanceof List) {
                            memberIds = (List<String>) data.get("memberIds");
                            Log.i(TAG, "找到memberIds字段, 成员数量: " + memberIds.size());
                        } else if (data.containsKey("users") && data.get("users") instanceof List) {
                            memberIds = (List<String>) data.get("users");
                            Log.i(TAG, "找到users字段(List), 成员数量: " + memberIds.size());
                        }
                        
                        // 如果memberIds为null，尝试将users字段(Map)转换为List
                        if (memberIds == null && data.containsKey("users") && data.get("users") instanceof Map) {
                            Map<String, Object> usersMap = (Map<String, Object>) data.get("users");
                            memberIds = new ArrayList<>(usersMap.keySet());
                            Log.i(TAG, "将users Map转换为List: " + memberIds.size() + "个成员");
                            
                            // 调试输出所有成员ID
                            StringBuilder userIds = new StringBuilder("成员ID列表: ");
                            for (String userId : memberIds) {
                                userIds.append(userId).append(", ");
                            }
                            Log.d(TAG, userIds.toString());
                        }
                        
                        // 尝试检查在另一个嵌套的map结构中
                        if (memberIds == null) {
                            for (String key : data.keySet()) {
                                Object value = data.get(key);
                                if (value instanceof Map) {
                                    Map<String, Object> nestedMap = (Map<String, Object>) value;
                                    if (nestedMap.containsKey("members") && nestedMap.get("members") instanceof List) {
                                        memberIds = (List<String>) nestedMap.get("members");
                                        Log.i(TAG, "从嵌套Map中的" + key + ".members找到成员列表, 数量: " + memberIds.size());
                                        break;
                                    } else if (nestedMap.containsKey("users") && nestedMap.get("users") instanceof List) {
                                        memberIds = (List<String>) nestedMap.get("users");
                                        Log.i(TAG, "从嵌套Map中的" + key + ".users找到成员列表, 数量: " + memberIds.size());
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // 如果仍未找到成员列表，添加当前用户和聊天室创建者作为临时解决方案
                        if (memberIds == null || memberIds.isEmpty()) {
                            Log.w(TAG, "未找到有效的成员列表，创建临时列表");
                            memberIds = new ArrayList<>();
                            
                            // 添加当前用户
                            if (currentUser != null) {
                                memberIds.add(currentUser.getUid());
                                Log.d(TAG, "添加当前用户ID: " + currentUser.getUid());
                            }
                            
                            // 添加聊天室创建者（如果有）
                            String creatorId = null;
                            if (data.containsKey("creator_id")) {
                                creatorId = (String) data.get("creator_id");
                            } else if (data.containsKey("ownerId")) {
                                creatorId = (String) data.get("ownerId");
                            } else if (data.containsKey("owner_id")) {
                                creatorId = (String) data.get("owner_id");
                            }
                            
                            if (creatorId != null && !memberIds.contains(creatorId)) {
                                memberIds.add(creatorId);
                                Log.d(TAG, "添加聊天室创建者ID: " + creatorId);
                            }
                        }
                        
                        // 如果找到了成员列表
                        if (memberIds != null && !memberIds.isEmpty()) {
                            final List<String> finalMemberIds = memberIds;
                            Log.i(TAG, "找到" + memberIds.size() + "个成员ID: " + memberIds.toString());
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
                                Log.i(TAG, "加载成员批次: " + currentBatch.toString());
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
                            Log.w(TAG, "未找到成员列表或成员列表为空");
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
                    Toast.makeText(MemberListActivity.this, "Chat room not found", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    updateEmptyView();
                    isLoading = false;
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "加载聊天室失败: " + e.getMessage(), e);
                Toast.makeText(MemberListActivity.this, "Failed to load member list", Toast.LENGTH_SHORT).show();
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
        
        // 打印所有用户ID，用于调试
        StringBuilder ids = new StringBuilder();
        for (String id : userIds) {
            ids.append(id).append(", ");
        }
        Log.i(TAG, "尝试获取以下用户ID: " + ids.toString());
        
        // 直接使用ID作为文档ID查询（优先使用此方法）
        List<DocumentReference> userRefs = new ArrayList<>();
        for (String userId : userIds) {
            userRefs.add(db.collection("users").document(userId));
            Log.d(TAG, "添加用户引用: " + userId);
        }
        
        // 批量获取用户数据
        if (!userRefs.isEmpty()) {
            Log.i(TAG, "使用文档ID获取用户数据...");
            db.runTransaction(transaction -> {
                List<DocumentSnapshot> userDocs = new ArrayList<>();
                for (DocumentReference ref : userRefs) {
                    DocumentSnapshot doc = transaction.get(ref);
                    if (doc.exists()) {
                        userDocs.add(doc);
                        Log.d(TAG, "获取到用户文档: " + doc.getId());
                    } else {
                        Log.w(TAG, "用户文档不存在: " + ref.getId());
                    }
                }
                return userDocs;
            }).addOnSuccessListener(userDocs -> {
                Log.i(TAG, "通过文档ID直接获取到 " + userDocs.size() + " 个用户数据");
                if (!userDocs.isEmpty()) {
                    processUserData(userDocs);
                } else {
                    // 如果直接通过ID获取失败，尝试whereIn查询
                    tryWhereInQueries(userIds);
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "批量获取用户失败: " + e.getMessage(), e);
                // 尝试whereIn查询
                tryWhereInQueries(userIds);
            });
        } else {
            Log.e(TAG, "没有用户引用可以查询");
            showNoDataMessage();
        }
    }
    
    // 尝试使用whereIn查询获取用户数据（作为备选方法）
    private void tryWhereInQueries(List<String> userIds) {
        Log.i(TAG, "尝试使用whereIn查询获取用户数据");
        
        // 尝试收集所有可能匹配用户ID的字段
        List<String> possibleIdFields = new ArrayList<>();
        possibleIdFields.add("uid");
        possibleIdFields.add("id");
        possibleIdFields.add("userId");
        possibleIdFields.add("user_id");
        
        // 限制查询数量，避免超过Firestore限制
        final List<String> limitedIds;
        if (userIds.size() > 10) {
            limitedIds = new ArrayList<>(userIds.subList(0, 10));
            Log.w(TAG, "查询ID数量过多，限制为前10个");
        } else {
            limitedIds = new ArrayList<>(userIds); // 创建新的副本以确保不变性
        }
        
        // 使用in查询查找可能包含这些用户ID的文档
        db.collection("users")
            .whereIn(possibleIdFields.get(0), limitedIds)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    int resultSize = task.getResult().size();
                    Log.i(TAG, "使用" + possibleIdFields.get(0) + "字段获取到 " + resultSize + " 个用户数据");
                    
                    if (resultSize > 0) {
                        // 处理获取到的用户数据
                        processUserData(task.getResult().getDocuments());
                    } else if (possibleIdFields.size() > 1) {
                        // 尝试使用下一个可能的ID字段
                        String nextField = possibleIdFields.get(1);
                        Log.i(TAG, "尝试使用另一个字段: " + nextField);
                        possibleIdFields.remove(0);
                        
                        // 使用final变量保存要在Lambda中使用的nextField
                        final String finalNextField = nextField;
                        
                        // 再次尝试查询
                        db.collection("users")
                            .whereIn(finalNextField, limitedIds)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    Log.i(TAG, "使用" + finalNextField + "字段成功获取到 " + 
                                          queryDocumentSnapshots.size() + " 个用户数据");
                                    processUserData(queryDocumentSnapshots.getDocuments());
                                } else {
                                    Log.w(TAG, "尝试所有字段后仍无法获取用户数据");
                                    showNoDataMessage();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "使用" + finalNextField + "字段查询失败: " + e.getMessage(), e);
                                showNoDataMessage();
                            });
                    } else {
                        Log.w(TAG, "尝试所有可能的字段后仍无法获取用户数据");
                        showNoDataMessage();
                    }
                } else {
                    Log.e(TAG, "获取用户详情失败: " + task.getException());
                    showNoDataMessage();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "批量获取用户失败: " + e.getMessage(), e);
                showNoDataMessage();
            });
    }
    
    private void processUserData(List<DocumentSnapshot> userDocs) {
        Log.i(TAG, "开始处理用户数据，数量: " + userDocs.size());
        
        if (userDocs.isEmpty()) {
            Log.w(TAG, "没有用户数据可处理");
            showNoDataMessage();
            return;
        }
        
        // 查找聊天室所有者ID
        db.collection("chat_rooms").document(chatRoomId)
            .get()
            .addOnSuccessListener(chatRoomDoc -> {
                if (!chatRoomDoc.exists()) {
                    Log.e(TAG, "聊天室文档不存在: " + chatRoomId);
                    processUsersWithoutOwner(userDocs);
                    return;
                }
                
                // 输出整个聊天室数据用于调试
                Map<String, Object> data = chatRoomDoc.getData();
                if (data != null) {
                    Log.d(TAG, "聊天室数据: " + data);
                } else {
                    Log.w(TAG, "聊天室数据为空");
                }
                
                // 尝试多个可能的所有者ID字段名
                String ownerId = null;
                if (chatRoomDoc.contains("creator_id")) {
                    ownerId = chatRoomDoc.getString("creator_id");
                    Log.d(TAG, "从creator_id字段获取所有者: " + ownerId);
                } else if (chatRoomDoc.contains("ownerId")) {
                    ownerId = chatRoomDoc.getString("ownerId");
                    Log.d(TAG, "从ownerId字段获取所有者: " + ownerId);
                } else if (chatRoomDoc.contains("owner_id")) {
                    ownerId = chatRoomDoc.getString("owner_id");
                    Log.d(TAG, "从owner_id字段获取所有者: " + ownerId);
                }
                
                final String finalOwnerId = ownerId;
                Log.i(TAG, "聊天室所有者ID: " + (finalOwnerId != null ? finalOwnerId : "未找到"));
                
                // 处理用户数据
                processUsersWithOwner(userDocs, finalOwnerId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "获取聊天室所有者失败: " + e.getMessage(), e);
                // 即使失败也要处理用户数据
                processUsersWithoutOwner(userDocs);
            });
    }
    
    // 处理用户数据（带所有者信息）
    private void processUsersWithOwner(List<DocumentSnapshot> userDocs, String ownerId) {
        processUsersInternal(userDocs, ownerId);
    }
    
    // 处理用户数据（无所有者信息）
    private void processUsersWithoutOwner(List<DocumentSnapshot> userDocs) {
        processUsersInternal(userDocs, null);
    }
    
    // 内部处理用户数据的通用方法
    private void processUsersInternal(List<DocumentSnapshot> userDocs, String ownerId) {
        // 循环处理每个用户文档
        for (DocumentSnapshot userDoc : userDocs) {
            Log.d(TAG, "处理用户文档: " + userDoc.getId() + ", 数据: " + userDoc.getData());
            
            Member member = new Member();
            
            // 获取用户ID (可能在不同字段中)
            String userId = userDoc.getId(); // 默认使用文档ID
            
            // 尝试查找可能包含userId的字段
            if (userDoc.contains("uid")) {
                userId = userDoc.getString("uid");
                Log.d(TAG, "从uid字段获取用户ID: " + userId);
            } else if (userDoc.contains("id")) {
                userId = userDoc.getString("id");
                Log.d(TAG, "从id字段获取用户ID: " + userId);
            } else if (userDoc.contains("userId")) {
                userId = userDoc.getString("userId");
                Log.d(TAG, "从userId字段获取用户ID: " + userId);
            }
            
            // 设置基本属性
            member.setUserId(userId);
            
            // 尝试获取用户名 (可能在不同字段中)
            String username = null;
            if (userDoc.contains("uname")) {
                username = userDoc.getString("uname");
                Log.d(TAG, "从uname字段获取用户名: " + username);
            } else if (userDoc.contains("username")) {
                username = userDoc.getString("username");
                Log.d(TAG, "从username字段获取用户名: " + username);
            } else if (userDoc.contains("name")) {
                username = userDoc.getString("name");
                Log.d(TAG, "从name字段获取用户名: " + username);
            } else if (userDoc.contains("displayName")) {
                username = userDoc.getString("displayName");
                Log.d(TAG, "从displayName字段获取用户名: " + username);
            }
            
            // 如果没有用户名，生成默认用户名（使用邮箱前缀或ID的前6位）
            if (username == null || username.isEmpty()) {
                String email = userDoc.getString("email");
                if (email != null && email.contains("@")) {
                    username = email.substring(0, email.indexOf('@'));
                    Log.d(TAG, "使用邮箱生成用户名: " + username);
                } else {
                    // 使用用户ID的前6位作为默认名
                    username = "用户" + userId.substring(0, Math.min(6, userId.length()));
                    Log.d(TAG, "生成默认用户名: " + username);
                }
            }
            member.setUsername(username);
            
            // 尝试获取头像URL
            String avatarUrl = null;
            if (userDoc.contains("avatar_url")) {
                avatarUrl = userDoc.getString("avatar_url");
                Log.d(TAG, "从avatar_url字段获取头像: " + avatarUrl);
            } else if (userDoc.contains("avatarUrl")) {
                avatarUrl = userDoc.getString("avatarUrl");
                Log.d(TAG, "从avatarUrl字段获取头像: " + avatarUrl);
            } else if (userDoc.contains("photoUrl")) {
                avatarUrl = userDoc.getString("photoUrl");
                Log.d(TAG, "从photoUrl字段获取头像: " + avatarUrl);
            }
            member.setAvatarUrl(avatarUrl);
            
            // 设置是否为群主
            boolean isOwner = ownerId != null && ownerId.equals(userId);
            member.setIsOwner(isOwner);
            if (isOwner) {
                Log.i(TAG, "标记用户为群主: " + username + ", ID: " + userId);
            }
            
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
            Log.i(TAG, "添加成员: " + member.getUsername() + ", ID: " + userId + ", 头像: " + 
                  (avatarUrl != null ? avatarUrl : "无"));
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
            Log.i(TAG, "成员列表排序完成，共 " + memberList.size() + " 个成员");
        } else {
            Log.w(TAG, "处理后的成员列表为空");
        }
        
        // 在UI线程上更新适配器
        runOnUiThread(() -> {
            memberAdapter.notifyDataSetChanged();
            updateEmptyView();
            showLoading(false);
            isLoading = false;
            
            // 显示成员数量
            tvMemberCount.setText(String.format("共%d人", memberList.size()));
            Log.i(TAG, "UI更新完成，显示 " + memberList.size() + " 个成员");
        });
    }
    
    private void showNoDataMessage() {
        Toast.makeText(MemberListActivity.this, "Failed to load member information", Toast.LENGTH_SHORT).show();
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
        // 记录日志
        android.util.Log.d(TAG, "returnToChatRoom: 返回聊天室 chatRoomId=" + chatRoomId);
        
        // 通过创建新的ChatRoomActivity Intent来返回聊天室，确保完全重建
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("chatRoomId", chatRoomId);
        if (chatRoomName != null && !chatRoomName.isEmpty()) {
            intent.putExtra("chatRoomName", chatRoomName);
        }
        
        // 设置标志确保聊天室Activity完全重建
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        
        // 添加平滑过渡动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        
        // 结束当前Activity
        finish();
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

    // 测试日志输出
    private void testLogcat() {
        // 输出各种级别的日志以测试Logcat配置
        android.util.Log.e(TAG, "测试ERROR级别日志 - " + System.currentTimeMillis());
        android.util.Log.w(TAG, "测试WARN级别日志 - " + System.currentTimeMillis());
        android.util.Log.i(TAG, "测试INFO级别日志 - " + System.currentTimeMillis());
        android.util.Log.d(TAG, "测试DEBUG级别日志 - " + System.currentTimeMillis());
        android.util.Log.v(TAG, "测试VERBOSE级别日志 - " + System.currentTimeMillis());
        
        // 显示提示
        Toast.makeText(this, "已发送测试日志，请检查Logcat", Toast.LENGTH_SHORT).show();
        
        // 尝试清除成员列表并重新加载，帮助刷新界面
        memberList.clear();
        memberAdapter.notifyDataSetChanged();
        loadInitialMembers();
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
                // 获取用户数据
                context.db.collection("users").document(member.getUserId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // 使用UserProfileManager显示资料浮窗
                        UserProfileManager.getInstance().showUserProfile(
                                context, 
                                holder.itemView,
                                member.getUserId(),
                                documentSnapshot,
                                context.chatRoomId,
                                context.chatRoomName);
                        
                        Log.d(TAG, "已打开用户资料浮窗，用户ID: " + member.getUserId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "获取用户数据失败: " + e.getMessage(), e);
                        Toast.makeText(context, "无法加载用户资料: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
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