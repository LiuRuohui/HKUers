package hk.hku.cs.hkuers.features.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import hk.hku.cs.hkuers.MainActivity;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;
import hk.hku.cs.hkuers.models.Message;
import hk.hku.cs.hkuers.features.chat.MemberListActivity;

public class ChatRoomActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText etMessage;
    private TextView tvChatRoomName, tvAnnouncement;
    private Button btnSend, btnChat, btnMap, btnProfile, btnCourses, btnMarketplace;
    private ImageButton btnBack, btnGroupInfo;
    private LinearLayout announcementLayout;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    private String chatRoomId, chatRoomName, creatorId;
    private boolean isCreator = false;
    
    private FirestoreRecyclerAdapter<Message, RecyclerView.ViewHolder> adapter;
    
    // 消息类型常量
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SYSTEM = 3;
    private static final int VIEW_TYPE_ANNOUNCEMENT = 4;
    
    // 在类的顶部添加一个标志变量，表示Activity是否正在结束
    private boolean isFinishing = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 添加日志
        android.util.Log.d("ChatRoomActivity", "onCreate: 开始创建ChatRoomActivity");
        
        setContentView(R.layout.activity_chat_room);
        
        // 测试页面是否正确显示
        String displayName = getIntent().getStringExtra("chatRoomName");
        if (displayName != null) {
            Toast.makeText(this, "正在加载聊天室: " + displayName, Toast.LENGTH_SHORT).show();
        }
        
        // 如果整个页面是黑色的，可能是主题问题，尝试设置背景色
        findViewById(android.R.id.content).setBackgroundColor(getResources().getColor(android.R.color.white));
        
        // 初始化Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化视图
        initViews();
        
        // 获取传入的聊天室ID和名称
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        chatRoomName = getIntent().getStringExtra("chatRoomName");
        
        // 添加日志
        android.util.Log.d("ChatRoomActivity", "收到参数: chatRoomId=" + chatRoomId + ", chatRoomName=" + chatRoomName);
        
        if (chatRoomId == null) {
            Toast.makeText(this, "聊天室ID不能为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 设置聊天室名称
        if (chatRoomName != null) {
            tvChatRoomName.setText(chatRoomName);
        } else {
            // 如果聊天室名称为空，则使用ID的前几位字符
            tvChatRoomName.setText("聊天室-" + chatRoomId.substring(0, Math.min(6, chatRoomId.length())));
        }
        
        // 加载聊天室信息
        loadChatRoomInfo();
        
        // 设置消息适配器
        setupMessageAdapter();
        
        // 设置按钮点击事件
        setupClickListeners();
        
        // 更新用户读取状态
        updateUserReadStatus();
        
        // 添加日志
        android.util.Log.d("ChatRoomActivity", "onCreate: ChatRoomActivity创建完成");
    }
    
    private void initViews() {
        // 添加日志
        android.util.Log.d("ChatRoomActivity", "initViews: 开始初始化视图");
        
        recyclerView = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        tvChatRoomName = findViewById(R.id.tvChatRoomName);
        tvAnnouncement = findViewById(R.id.tvAnnouncement);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnGroupInfo = findViewById(R.id.btnGroupInfo);
        announcementLayout = findViewById(R.id.announcementLayout);
        
        // 底部导航栏
        btnChat = findViewById(R.id.btnChat);
        btnMap = findViewById(R.id.btnMap);
        btnProfile = findViewById(R.id.btnProfile);
        btnCourses = findViewById(R.id.btnCourses);
        btnMarketplace = findViewById(R.id.btnMarketplace);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 添加日志
        android.util.Log.d("ChatRoomActivity", "initViews: 视图初始化完成");
    }
    
    private void loadChatRoomInfo() {
        // 添加日志
        android.util.Log.d("ChatRoomActivity", "loadChatRoomInfo: 开始加载聊天室信息, chatRoomId=" + chatRoomId);
        
        db.collection("chat_rooms").document(chatRoomId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                // 检查Activity是否正在结束
                if (isFinishing || isFinishing()) return;
                
                android.util.Log.d("ChatRoomActivity", "loadChatRoomInfo: 成功获取聊天室数据");
                
                if (documentSnapshot.exists()) {
                    // 记录整个数据，用于调试
                    android.util.Log.d("ChatRoomActivity", "聊天室数据: " + documentSnapshot.getData());
                    
                    // 获取群主ID
                    creatorId = documentSnapshot.getString("creator_id");
                    isCreator = creatorId != null && creatorId.equals(currentUser.getUid());
                    
                    android.util.Log.d("ChatRoomActivity", "聊天室创建者ID: " + creatorId + ", 当前用户是创建者: " + isCreator);
                    
                    // 获取聊天室名称（兼容不同的字段名）
                    String dbChatName = null;
                    // 尝试不同的字段名
                    if (documentSnapshot.contains("chat_name")) {
                        dbChatName = documentSnapshot.getString("chat_name");
                        android.util.Log.d("ChatRoomActivity", "从chat_name字段获取聊天室名称: " + dbChatName);
                    } else if (documentSnapshot.contains("name")) {
                        dbChatName = documentSnapshot.getString("name");
                        android.util.Log.d("ChatRoomActivity", "从name字段获取聊天室名称: " + dbChatName);
                    }
                    
                    // 如果从数据库获取到了名称，且当前名称为空或者与数据库不同，则更新
                    if (dbChatName != null && !dbChatName.isEmpty() && 
                        (chatRoomName == null || !chatRoomName.equals(dbChatName))) {
                        chatRoomName = dbChatName;
                        tvChatRoomName.setText(chatRoomName);
                        android.util.Log.d("ChatRoomActivity", "更新聊天室名称为: " + chatRoomName);
                    }
                    
                    // 加载群公告
                    String announcement = documentSnapshot.getString("announcement");
                    if (announcement != null && !announcement.isEmpty()) {
                        announcementLayout.setVisibility(View.VISIBLE);
                        tvAnnouncement.setText(announcement);
                        android.util.Log.d("ChatRoomActivity", "显示群公告: " + announcement);
                    } else {
                        announcementLayout.setVisibility(View.GONE);
                        android.util.Log.d("ChatRoomActivity", "没有群公告");
                    }
                    
                    // 获取并设置顶部栏颜色
                    String colorCode = documentSnapshot.getString("color_code");
                    if (colorCode != null && !colorCode.isEmpty()) {
                        try {
                            int color = android.graphics.Color.parseColor(colorCode);
                            // 设置顶部栏颜色
                            View toolbarLayout = findViewById(R.id.toolbarLayout);
                            if (toolbarLayout != null) { // 检查视图是否存在
                                toolbarLayout.setBackgroundColor(color);
                                
                                // 设置状态栏颜色（Android 5.0+）
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setStatusBarColor(color);
                                }
                                
                                android.util.Log.d("ChatRoomActivity", "设置顶部栏颜色: " + colorCode);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ChatRoomActivity", "解析颜色码失败: " + e.getMessage());
                        }
                    } else {
                        android.util.Log.d("ChatRoomActivity", "未找到聊天室颜色，使用默认颜色");
                    }
                } else {
                    android.util.Log.e("ChatRoomActivity", "聊天室数据不存在!");
                }
            })
            .addOnFailureListener(e -> {
                // 检查Activity是否正在结束
                if (isFinishing || isFinishing()) return;
                
                android.util.Log.e("ChatRoomActivity", "加载聊天室信息失败: " + e.getMessage());
                Toast.makeText(this, "加载聊天室信息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void setupMessageAdapter() {
        // 添加日志
        android.util.Log.d("ChatRoomActivity", "setupMessageAdapter: 开始设置消息适配器");
        
        Query query = db.collection("chat_rooms").document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);
        
        android.util.Log.d("ChatRoomActivity", "消息查询路径: chat_rooms/" + chatRoomId + "/messages");
        
        FirestoreRecyclerOptions<Message> options = new FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message.class)
                .build();
        
        adapter = new FirestoreRecyclerAdapter<Message, RecyclerView.ViewHolder>(options) {
            @Override
            public int getItemViewType(int position) {
                Message message = getItem(position);
                String senderId = message.getSenderId();
                String type = message.getType();
                
                if ("system".equals(senderId)) {
                    return VIEW_TYPE_SYSTEM;
                } else if ("announcement".equals(type)) {
                    return VIEW_TYPE_ANNOUNCEMENT;
                } else if (senderId.equals(currentUser.getUid())) {
                    return VIEW_TYPE_SENT;
                } else {
                    return VIEW_TYPE_RECEIVED;
                }
            }
            
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                
                switch (viewType) {
                    case VIEW_TYPE_SENT:
                        return new SentMessageViewHolder(
                                inflater.inflate(R.layout.item_message_sent, parent, false));
                    case VIEW_TYPE_RECEIVED:
                        return new ReceivedMessageViewHolder(
                                inflater.inflate(R.layout.item_message_received, parent, false));
                    case VIEW_TYPE_SYSTEM:
                        return new SystemMessageViewHolder(
                                inflater.inflate(R.layout.item_message_system, parent, false));
                    case VIEW_TYPE_ANNOUNCEMENT:
                        return new AnnouncementViewHolder(
                                inflater.inflate(R.layout.item_message_announcement, parent, false));
                    default:
                        return new ReceivedMessageViewHolder(
                                inflater.inflate(R.layout.item_message_received, parent, false));
                }
            }
            
            @Override
            protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull Message model) {
                android.util.Log.d("ChatRoomActivity", "绑定消息: " + model.getText() + ", 类型: " + model.getType());
                
                switch (holder.getItemViewType()) {
                    case VIEW_TYPE_SENT:
                        ((SentMessageViewHolder) holder).bind(model);
                        break;
                    case VIEW_TYPE_RECEIVED:
                        ((ReceivedMessageViewHolder) holder).bind(model);
                        break;
                    case VIEW_TYPE_SYSTEM:
                        ((SystemMessageViewHolder) holder).bind(model);
                        break;
                    case VIEW_TYPE_ANNOUNCEMENT:
                        ((AnnouncementViewHolder) holder).bind(model);
                        break;
                }
            }
            
            @Override
            public void onDataChanged() {
                android.util.Log.d("ChatRoomActivity", "onDataChanged: 数据已更新，消息数量: " + getItemCount());
                if (getItemCount() == 0) {
                    android.util.Log.d("ChatRoomActivity", "没有消息");
                }
            }
        };
        
        recyclerView.setAdapter(adapter);
        android.util.Log.d("ChatRoomActivity", "setupMessageAdapter: 消息适配器设置完成");
    }
    
    private void setupClickListeners() {
        // 返回按钮 - 使用一个通用的安全退出方法
        btnBack.setOnClickListener(v -> safeFinishActivity());
        
        // 群组信息按钮
        btnGroupInfo.setOnClickListener(v -> {
            if (isFinishing || isFinishing()) return;
            showGroupInfoDialog();
        });
        
        // 发送按钮
        btnSend.setOnClickListener(v -> {
            if (isFinishing || isFinishing()) return;
            sendMessage();
        });
        
        // 点击输入框时请求焦点并显示键盘
        etMessage.setOnClickListener(v -> {
            etMessage.requestFocus();
            showKeyboard(etMessage);
        });
        
        // 底部导航栏 - 所有导航按钮使用安全的Activity转换
        btnProfile.setOnClickListener(v -> safeNavigateTo(MainActivity.class));
        btnChat.setOnClickListener(v -> safeFinishActivity());
        btnMap.setOnClickListener(v -> safeNavigateTo(MapActivity.class));
        btnCourses.setOnClickListener(v -> {
            Toast.makeText(this, "课程功能待实现", Toast.LENGTH_SHORT).show();
        });
        btnMarketplace.setOnClickListener(v -> safeNavigateTo(MarketplaceActivity.class));
    }
    
    // 修改safeFinishActivity方法，改为明确跳转到ChatListActivity
    private void safeFinishActivity() {
        // 已经在结束中，避免重复调用
        if (isFinishing || isFinishing()) return;
        
        // 添加日志
        android.util.Log.d("ChatRoomActivity", "安全结束Activity - 直接跳转到ChatListActivity");
        
        // 设置标记表示Activity正在结束
        isFinishing = true;
        
        // 停止监听器和进行中的操作
        if (adapter != null) {
            try {
                adapter.stopListening();
                adapter = null; // 彻底解除引用
                android.util.Log.d("ChatRoomActivity", "已停止适配器监听并清除引用");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "停止适配器监听失败: " + e.getMessage());
            }
        }
        
        // 不使用延迟，立即跳转到ChatListActivity
        Intent intent = new Intent(this, ChatListActivity.class);
        // 使用FLAG_ACTIVITY_NEW_TASK和FLAG_ACTIVITY_CLEAR_TOP清除任务栈
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        
        // 使用简单动画
        overridePendingTransition(0, 0);
        
        // 立即释放资源
        recyclerView.setAdapter(null);
        
        // 直接调用finish()
        finish();
        
        // 不要再使用此Activity的任何功能
        android.util.Log.d("ChatRoomActivity", "已启动ChatListActivity并finish当前Activity");
    }
    
    // 添加安全的导航方法
    private <T extends AppCompatActivity> void safeNavigateTo(Class<T> targetActivity) {
        // 已经在结束中，避免重复调用
        if (isFinishing || isFinishing()) return;
        
        // 设置标记表示Activity正在结束
        isFinishing = true;
        
        // 停止监听器和进行中的操作
        if (adapter != null) {
            try {
                adapter.stopListening();
                android.util.Log.d("ChatRoomActivity", "已停止适配器监听");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "停止适配器监听失败: " + e.getMessage());
            }
        }
        
        // 延迟一点时间，让Firebase回调有时间处理
        new Handler().postDelayed(() -> {
            try {
                Intent intent = new Intent(ChatRoomActivity.this, targetActivity);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // 设置平滑过渡
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                // 非立即结束，让过渡动画有时间执行
                new Handler().postDelayed(() -> finish(), 100);
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "导航到" + targetActivity.getSimpleName() + "失败: " + e.getMessage());
                // 恢复标记状态，允许重试
                isFinishing = false;
            }
        }, 200);
    }
    
    private void showGroupInfoDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_group_info, null);
        
        TextView tvGroupName = view.findViewById(R.id.tvGroupName);
        Button btnViewMembers = view.findViewById(R.id.btnViewMembers);
        Button btnPublishAnnouncement = view.findViewById(R.id.btnPublishAnnouncement);
        Button btnLeaveGroup = view.findViewById(R.id.btnLeaveGroup);
        
        tvGroupName.setText(chatRoomName);
        
        // 只有群主可以发布公告
        btnPublishAnnouncement.setVisibility(isCreator ? View.VISIBLE : View.GONE);
        
        // 查看成员按钮
        btnViewMembers.setOnClickListener(v -> {
            dialog.dismiss();
            showMembersDialog();
        });
        
        // 发布公告按钮
        btnPublishAnnouncement.setOnClickListener(v -> {
            dialog.dismiss();
            showAnnouncementDialog();
        });
        
        // 退出群聊按钮
        btnLeaveGroup.setOnClickListener(v -> {
            dialog.dismiss();
            showLeaveGroupConfirmation();
        });
        
        dialog.setContentView(view);
        dialog.show();
    }
    
    private void showMembersDialog() {
        // 跳转到成员列表页面
        Intent intent = new Intent(this, MemberListActivity.class);
        intent.putExtra("chat_room_id", chatRoomId);
        // 额外添加聊天室名称，确保成员列表能正确返回
        intent.putExtra("chatRoomName", chatRoomName);
        startActivity(intent);
        
        // 添加平滑过渡动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    
    private void showAnnouncementDialog() {
        if (!isCreator) {
            Toast.makeText(this, "只有群主可以发布公告", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 使用深色主题的AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_announcement, null);
        EditText etAnnouncement = view.findViewById(R.id.etAnnouncement);
        
        // 设置EditText文本颜色为黑色，确保在浅色背景上可见
        etAnnouncement.setTextColor(android.graphics.Color.BLACK);
        etAnnouncement.setHintTextColor(android.graphics.Color.GRAY);
        
        // 加载当前公告
        db.collection("chat_rooms").document(chatRoomId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String announcement = documentSnapshot.getString("announcement");
                    if (announcement != null) {
                        etAnnouncement.setText(announcement);
                    }
                }
            });
        
        builder.setView(view)
               .setTitle("群公告")
               .setPositiveButton("发布", (dialog, which) -> {
                   String announcementText = etAnnouncement.getText().toString().trim();
                   publishAnnouncement(announcementText);
               })
               .setNegativeButton("取消", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void publishAnnouncement(String announcementText) {
        if (!isCreator) {
            Toast.makeText(this, "只有群主可以发布公告", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (announcementText.isEmpty()) {
            // 清除公告
            db.collection("chat_rooms").document(chatRoomId)
                .update("announcement", "")
                .addOnSuccessListener(aVoid -> {
                    announcementLayout.setVisibility(View.GONE);
                    addAnnouncementMessage("群公告已清除");
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "清除公告失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
        } else {
            // 更新公告
            db.collection("chat_rooms").document(chatRoomId)
                .update("announcement", announcementText)
                .addOnSuccessListener(aVoid -> {
                    announcementLayout.setVisibility(View.VISIBLE);
                    tvAnnouncement.setText(announcementText);
                    addAnnouncementMessage(announcementText);
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "发布公告失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
        }
    }
    
    private void addAnnouncementMessage(String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("text", text);
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "announcement");
        
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // 更新聊天室的最后消息和时间
                updateLastMessage("【公告】" + text);
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "发送公告失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    private void showLeaveGroupConfirmation() {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("退出群聊")
            .setMessage("确定要退出该群聊吗？")
            .setPositiveButton("确定", (dialog, which) -> leaveGroup())
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void leaveGroup() {
        String uid = currentUser.getUid();
        
        // 如果是群主，不允许退出
        if (isCreator) {
            Toast.makeText(this, "群主不能退出群聊，请先转让群主身份", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DocumentReference chatRef = db.collection("chat_rooms").document(chatRoomId);
        
        // 从成员列表中移除用户
        chatRef.get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // 获取当前成员列表
                    List<String> memberIds = (List<String>) documentSnapshot.get("member_ids");
                    
                    if (memberIds != null && memberIds.contains(uid)) {
                        // 移除用户ID
                        memberIds.remove(uid);
                        
                        // 更新成员列表
                        chatRef.update("member_ids", memberIds)
                            .addOnSuccessListener(aVoid -> {
                                // 添加退出消息
                                addUserLeftMessage();
                                
                                // 返回聊天列表页面
                                Intent intent = new Intent(ChatRoomActivity.this, ChatListActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> 
                                Toast.makeText(this, "退出群聊失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                    }
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "获取群聊信息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    private void addUserLeftMessage() {
        String userName = currentUser.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = currentUser.getEmail();
        }
        
        // 创建final变量以在lambda表达式中使用
        final String finalUserName = userName;
        
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "system");
        message.put("text", finalUserName + " 退出了群聊");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // 更新聊天室的最后消息和时间
                updateLastMessage(finalUserName + " 退出了群聊");
            });
    }
    
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(this, "消息不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建消息对象
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("text", messageText);
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        // 添加消息到Firestore
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // 清空输入框
                etMessage.setText("");
                
                // 更新聊天室的最后消息和时间
                updateLastMessage(messageText);
                
                // 滚动到底部
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    private void updateLastMessage(String message) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("last_message", message);
        updates.put("last_message_time", new Timestamp(new Date()));
        
        db.collection("chat_rooms").document(chatRoomId)
            .update(updates)
            .addOnFailureListener(e -> 
                Toast.makeText(this, "更新最后消息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    private void updateUserReadStatus() {
        // 更新用户最后读取时间
        Map<String, Object> userReadStatus = new HashMap<>();
        userReadStatus.put("user_read_status." + currentUser.getUid(), new Timestamp(new Date()));
        
        db.collection("chat_rooms").document(chatRoomId)
            .update(userReadStatus)
            .addOnFailureListener(e -> 
                Toast.makeText(this, "更新读取状态失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.d("ChatRoomActivity", "onStart: Activity开始");
        if (adapter != null) {
            adapter.startListening();
            android.util.Log.d("ChatRoomActivity", "适配器开始监听");
        }
    }
    
    @Override
    protected void onStop() {
        android.util.Log.d("ChatRoomActivity", "onStop: Activity停止");
        // 提前释放适配器资源
        if (adapter != null) {
            try {
                adapter.stopListening();
                android.util.Log.d("ChatRoomActivity", "适配器停止监听");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "停止适配器监听失败: " + e.getMessage());
            }
        }
        super.onStop();
    }
    
    // 修改onBackPressed方法，覆盖系统返回键行为
    @Override
    public void onBackPressed() {
        // 使用我们的安全返回方法而不是默认行为
        safeFinishActivity();
    }
    
    // 添加显示键盘的方法
    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    // 发送消息的ViewHolder
    public class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessageText, tvTime, tvReadStatus, tvExpandCollapse;
        private CircleImageView ivUserAvatar;
        private LinearLayout messageContainer;
        private boolean isExpanded = false;
        
        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvReadStatus = itemView.findViewById(R.id.tvReadStatus);
            tvExpandCollapse = itemView.findViewById(R.id.tvExpandCollapse);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }
        
        public void bind(Message message) {
            tvMessageText.setText(message.getText());
            tvTime.setText(formatTime(message.getTimestamp()));
            
            // 动态获取已读状态
            updateReadStatus(message);
            
            // 加载用户头像
            // TODO: 替换为实际头像加载逻辑
            
            // 处理长消息折叠/展开
            if (message.getText().length() > 100) {
                tvExpandCollapse.setVisibility(View.VISIBLE);
                tvExpandCollapse.setOnClickListener(v -> {
                    // 检查Activity是否正在结束
                    if (isFinishing || isFinishing()) return;
                    
                    isExpanded = !isExpanded;
                    if (isExpanded) {
                        tvMessageText.setMaxLines(Integer.MAX_VALUE);
                        tvExpandCollapse.setText("收起");
                    } else {
                        tvMessageText.setMaxLines(5);
                        tvExpandCollapse.setText("展开更多");
                    }
                });
                tvMessageText.setMaxLines(5);
            } else {
                tvExpandCollapse.setVisibility(View.GONE);
                tvMessageText.setMaxLines(Integer.MAX_VALUE);
            }
        }
        
        // 更新已读状态
        private void updateReadStatus(Message message) {
            // 获取群组成员数量和已读人数
            db.collection("chat_rooms").document(chatRoomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing || isFinishing()) return;
                    
                    if (documentSnapshot.exists()) {
                        // 获取成员ID列表
                        List<String> memberIds = (List<String>) documentSnapshot.get("member_ids");
                        
                        if (memberIds == null || memberIds.isEmpty()) {
                            tvReadStatus.setText("0/0人已读");
                            return;
                        }
                        
                        // 创建需要阅读此消息的用户列表（排除发送者自己）
                        List<String> readersToCount = new ArrayList<>();
                        for (String memberId : memberIds) {
                            // 不将消息发送者自己计入需要读取的人数
                            if (!memberId.equals(message.getSenderId())) {
                                readersToCount.add(memberId);
                            }
                        }
                        
                        int totalReadersCount = readersToCount.size();
                        
                        // 获取已读状态映射
                        Map<String, Object> readStatusMap = (Map<String, Object>) documentSnapshot.get("user_read_status");
                        int readCount = 0;
                        
                        if (readStatusMap != null && totalReadersCount > 0) {
                            // 计算已读人数（根据时间戳比较）
                            Timestamp messageTime = message.getTimestamp();
                            
                            for (String userId : readersToCount) {
                                Object userReadTimeObj = readStatusMap.get(userId);
                                if (userReadTimeObj instanceof Timestamp) {
                                    Timestamp userReadTime = (Timestamp) userReadTimeObj;
                                    // 如果用户最后读取时间晚于消息发送时间，则计为已读
                                    if (userReadTime.compareTo(messageTime) >= 0) {
                                        readCount++;
                                    }
                                }
                            }
                            
                            // 更新UI
                            tvReadStatus.setText(readCount + "/" + totalReadersCount + "人已读");
                        } else {
                            // 默认显示
                            tvReadStatus.setText("0/" + totalReadersCount + "人已读");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing || isFinishing()) return;
                    tvReadStatus.setText("未知已读状态");
                    android.util.Log.e("ChatRoomActivity", "获取已读状态失败: " + e.getMessage());
                });
        }
    }
    
    // 接收消息的ViewHolder
    public class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessageText, tvTime, tvSenderName, tvExpandCollapse;
        private CircleImageView ivUserAvatar;
        private boolean isExpanded = false;
        
        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvExpandCollapse = itemView.findViewById(R.id.tvExpandCollapse);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }
        
        public void bind(Message message) {
            tvMessageText.setText(message.getText());
            tvTime.setText(formatTime(message.getTimestamp()));
            
            // 获取发送者信息
            db.collection("users").document(message.getSenderId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // 检查Activity是否正在结束
                    if (isFinishing || isFinishing()) return;
                    
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("display_name");
                        if (userName == null || userName.isEmpty()) {
                            userName = documentSnapshot.getString("email");
                        }
                        tvSenderName.setText(userName);
                        
                        // TODO: 加载用户头像
                    }
                });
            
            // 处理长消息折叠/展开
            if (message.getText().length() > 100) {
                tvExpandCollapse.setVisibility(View.VISIBLE);
                tvExpandCollapse.setOnClickListener(v -> {
                    // 检查Activity是否正在结束
                    if (isFinishing || isFinishing()) return;
                    
                    isExpanded = !isExpanded;
                    if (isExpanded) {
                        tvMessageText.setMaxLines(Integer.MAX_VALUE);
                        tvExpandCollapse.setText("收起");
                    } else {
                        tvMessageText.setMaxLines(5);
                        tvExpandCollapse.setText("展开更多");
                    }
                });
                tvMessageText.setMaxLines(5);
            } else {
                tvExpandCollapse.setVisibility(View.GONE);
                tvMessageText.setMaxLines(Integer.MAX_VALUE);
            }
        }
    }
    
    // 系统消息的ViewHolder
    public class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSystemMessage, tvTime;
        
        public SystemMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystemMessage = itemView.findViewById(R.id.tvSystemMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
        
        public void bind(Message message) {
            tvSystemMessage.setText(message.getText());
            tvTime.setText(formatTime(message.getTimestamp()));
        }
    }
    
    // 公告消息的ViewHolder
    public class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        private TextView tvAnnouncementText, tvTime, tvPublisherName, tvExpandCollapse;
        private boolean isExpanded = false;
        
        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAnnouncementText = itemView.findViewById(R.id.tvAnnouncementText);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPublisherName = itemView.findViewById(R.id.tvPublisherName);
            tvExpandCollapse = itemView.findViewById(R.id.tvExpandCollapse);
        }
        
        public void bind(Message message) {
            tvAnnouncementText.setText(message.getText());
            tvTime.setText(formatDate(message.getTimestamp()));
            
            // 获取发布者信息
            db.collection("users").document(message.getSenderId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // 检查Activity是否正在结束
                    if (isFinishing || isFinishing()) return;
                    
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("display_name");
                        if (userName == null || userName.isEmpty()) {
                            userName = documentSnapshot.getString("email");
                        }
                        tvPublisherName.setText(userName);
                    }
                });
            
            // 处理长消息折叠/展开
            if (message.getText().length() > 100) {
                tvExpandCollapse.setVisibility(View.VISIBLE);
                tvExpandCollapse.setOnClickListener(v -> {
                    // 检查Activity是否正在结束
                    if (isFinishing || isFinishing()) return;
                    
                    isExpanded = !isExpanded;
                    if (isExpanded) {
                        tvAnnouncementText.setMaxLines(Integer.MAX_VALUE);
                        tvExpandCollapse.setText("收起");
                    } else {
                        tvAnnouncementText.setMaxLines(5);
                        tvExpandCollapse.setText("展开更多");
                    }
                });
                tvAnnouncementText.setMaxLines(5);
            } else {
                tvExpandCollapse.setVisibility(View.GONE);
                tvAnnouncementText.setMaxLines(Integer.MAX_VALUE);
            }
        }
    }
    
    // 格式化时间为小时:分钟
    private String formatTime(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
    
    // 格式化完整日期
    private String formatDate(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
    
    // 在onPause方法中添加输入法管理器的清理
    @Override
    protected void onPause() {
        android.util.Log.d("ChatRoomActivity", "onPause: Activity暂停");
        // 隐藏键盘并清理输入管理器引用
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
        super.onPause();
    }

    // 修改onDestroy方法，确保在Activity销毁时释放资源
    @Override
    protected void onDestroy() {
        android.util.Log.d("ChatRoomActivity", "onDestroy: Activity销毁");
        
        // 确保输入法管理器资源被清理
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
        
        // 清理任何可能的引用
        if (adapter != null) {
            try {
                adapter.stopListening();
                android.util.Log.d("ChatRoomActivity", "onDestroy中停止适配器监听");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "onDestroy中停止适配器监听失败: " + e.getMessage());
            }
        }
        
        // 清空视图引用以避免内存泄漏
        recyclerView = null;
        etMessage = null;
        tvChatRoomName = null;
        tvAnnouncement = null;
        btnSend = null;
        btnBack = null;
        btnGroupInfo = null;
        announcementLayout = null;
        btnChat = null;
        btnMap = null;
        btnProfile = null;
        btnCourses = null;
        btnMarketplace = null;
        
        super.onDestroy();
    }
} 