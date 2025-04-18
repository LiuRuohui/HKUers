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
import com.bumptech.glide.Glide;

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
    
    // 服务器URL常量
    private static final String SERVER_URL = "http://10.0.2.2:9000";
    
    // 在类的顶部添加一个标志变量，表示Activity是否正在结束
    private boolean isFinishing = false;
    
    private static final int REQUEST_CODE_VIEW_MEMBERS = 1001;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 添加日志
        android.util.Log.d("ChatRoomActivity", "onCreate: 开始创建ChatRoomActivity");
        
        setContentView(R.layout.activity_chat_room);
        
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
                android.util.Log.d("ChatRoomActivity", "设置状态栏高度: " + statusBarHeight);
            }
        }
        
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
                    
                    // 获取并设置颜色指示条的颜色
                    String colorCode = documentSnapshot.getString("color_code");
                    if (colorCode != null && !colorCode.isEmpty()) {
                        try {
                            int color = android.graphics.Color.parseColor(colorCode);
                            // 设置颜色指示条的颜色
                            View colorIndicator = findViewById(R.id.colorIndicator);
                            if (colorIndicator != null) {
                                colorIndicator.setBackgroundColor(color);
                                android.util.Log.d("ChatRoomActivity", "设置颜色指示条颜色: " + colorCode);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ChatRoomActivity", "解析颜色码失败: " + e.getMessage());
                        }
                    } else {
                        android.util.Log.d("ChatRoomActivity", "未找到聊天室颜色，使用默认颜色");
                        // 如果没有设置颜色，根据聊天室ID生成一个固定的颜色
                        if (chatRoomId != null && !chatRoomId.isEmpty()) {
                            View colorIndicator = findViewById(R.id.colorIndicator);
                            if (colorIndicator != null) {
                                int colorIndex = Math.abs(chatRoomId.hashCode()) % ChatListActivity.ChatGroupViewHolder.GROUP_COLORS.length;
                                int color = ChatListActivity.ChatGroupViewHolder.GROUP_COLORS[colorIndex];
                                colorIndicator.setBackgroundColor(color);
                            }
                        }
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
                
                // 添加空值检查，避免NullPointerException
                if ("system".equals(senderId)) {
                    return VIEW_TYPE_SYSTEM;
                } else if (type != null && "announcement".equals(type)) {
                    return VIEW_TYPE_ANNOUNCEMENT;
                } else if (senderId != null && senderId.equals(currentUser.getUid())) {
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
        
        // 根据是否为群主更改按钮文字
        if (isCreator) {
            btnLeaveGroup.setText("解散群聊");
        } else {
            btnLeaveGroup.setText("退出群聊");
        }
        
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
        
        // 退出/解散群聊按钮
        btnLeaveGroup.setOnClickListener(v -> {
            dialog.dismiss();
            if (isCreator) {
                showDisbandGroupConfirmation();
            } else {
                showLeaveGroupConfirmation();
            }
        });
        
        dialog.setContentView(view);
        dialog.show();
    }
    
    private void showMembersDialog() {
        try {
            // 简单直接的跳转实现
            Intent intent = new Intent(this, MemberListActivity.class);
            intent.putExtra("chat_room_id", chatRoomId);
            if (chatRoomName != null && !chatRoomName.isEmpty()) {
                intent.putExtra("chatRoomName", chatRoomName);
            }
            // 使用startActivityForResult而不是startActivity
            startActivityForResult(intent, REQUEST_CODE_VIEW_MEMBERS);
            // 简单的过渡动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            android.util.Log.e("ChatRoomActivity", "跳转到成员列表失败: " + e.getMessage(), e);
            Toast.makeText(this, "无法打开成员列表: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // 处理从成员列表页面返回的结果
        if (requestCode == REQUEST_CODE_VIEW_MEMBERS) {
            android.util.Log.d("ChatRoomActivity", "从成员列表返回");
            
            // 确保适配器开始监听
            if (adapter != null && !isFinishing && !isFinishing()) {
                try {
                    adapter.startListening();
                    android.util.Log.d("ChatRoomActivity", "从成员列表返回后恢复适配器监听");
                } catch (Exception e) {
                    android.util.Log.e("ChatRoomActivity", "恢复适配器监听失败: " + e.getMessage());
                }
            }
        }
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
    
    private void showDisbandGroupConfirmation() {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("解散群聊")
            .setMessage("确定要解散该群聊吗？此操作不可逆，所有聊天记录将被删除！")
            .setPositiveButton("确定解散", (dialog, which) -> disbandGroup())
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void leaveGroup() {
        String uid = currentUser.getUid();
        
        // 验证不是群主才能退出
        if (isCreator) {
            Toast.makeText(this, "群主不能退出群聊，只能解散群聊", Toast.LENGTH_SHORT).show();
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
    
    private void disbandGroup() {
        // 验证是群主才能解散
        if (!isCreator) {
            Toast.makeText(this, "只有群主可以解散群聊", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示进度对话框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setTitle("正在解散群聊")
            .setMessage("请稍候...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        final DocumentReference chatRef = db.collection("chat_rooms").document(chatRoomId);
        
        // 首先从聊天室集合中删除所有消息
        chatRef.collection("messages")
            .get()
            .addOnSuccessListener(messagesSnapshot -> {
                // 创建批量写入操作
                android.util.Log.d("ChatRoomActivity", "正在删除聊天室消息, 共 " + messagesSnapshot.size() + " 条");
                
                // 使用递归方式批量删除，避免单次批量操作过多
                deleteMessagesRecursively(chatRef, messagesSnapshot.getDocuments(), 0, progressDialog);
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "解散群聊失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    // 递归删除消息，每批次最多删除500条，避免超过Firebase批量操作限制
    private void deleteMessagesRecursively(DocumentReference chatRef, 
                                          List<DocumentSnapshot> messages,
                                          int startIndex,
                                          AlertDialog progressDialog) {
        final int batchSize = 500; // Firestore每批次最多500个操作
        
        // 创建一个新的批量写入
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        
        int endIndex = Math.min(startIndex + batchSize, messages.size());
        
        // 将消息添加到批量删除中
        for (int i = startIndex; i < endIndex; i++) {
            batch.delete(messages.get(i).getReference());
        }
        
        // 提交批量删除
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                android.util.Log.d("ChatRoomActivity", "已删除 " + (endIndex - startIndex) + " 条消息");
                
                // 检查是否还有更多消息需要删除
                if (endIndex < messages.size()) {
                    // 还有更多消息，继续递归删除
                    deleteMessagesRecursively(chatRef, messages, endIndex, progressDialog);
                } else {
                    // 所有消息已删除，接下来删除成员集合
                    android.util.Log.d("ChatRoomActivity", "所有消息已删除，正在删除成员集合");
                    deleteMembers(chatRef, progressDialog);
                }
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "删除消息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    // 删除成员集合
    private void deleteMembers(DocumentReference chatRef, AlertDialog progressDialog) {
        chatRef.collection("members")
            .get()
            .addOnSuccessListener(membersSnapshot -> {
                android.util.Log.d("ChatRoomActivity", "正在删除群成员, 共 " + membersSnapshot.size() + " 个");
                
                // 创建批量写入操作
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                
                // 将所有成员添加到批量删除中
                for (DocumentSnapshot memberDoc : membersSnapshot.getDocuments()) {
                    batch.delete(memberDoc.getReference());
                }
                
                // 提交批量删除
                batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("ChatRoomActivity", "所有成员已删除，正在删除聊天室文档");
                        
                        // 最后删除聊天室文档本身
                        deleteChat(chatRef, progressDialog);
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "删除成员失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "删除成员集合失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    // 删除聊天室文档
    private void deleteChat(DocumentReference chatRef, AlertDialog progressDialog) {
        // 获取聊天室信息用于记录
        chatRef.get()
            .addOnSuccessListener(chatSnapshot -> {
                if (chatSnapshot.exists()) {
                    // 记录一下要删除的聊天室信息
                    String chatRoomNameToDelete = chatSnapshot.getString("chat_name");
                    android.util.Log.d("ChatRoomActivity", "准备删除聊天室: " + chatRoomNameToDelete);
                    
                    // 删除聊天室文档
                    chatRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            // 关闭进度对话框
                            progressDialog.dismiss();
                            
                            Toast.makeText(ChatRoomActivity.this, "群聊已解散并删除", Toast.LENGTH_SHORT).show();
                            android.util.Log.d("ChatRoomActivity", "聊天室已完全删除: " + chatRoomNameToDelete);
                            
                            // 返回聊天列表页面
                            Intent intent = new Intent(ChatRoomActivity.this, ChatListActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "删除聊天室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(this, "聊天室不存在", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "获取聊天室信息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
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
            
            // 加载当前用户头像
            String currentUserId = currentUser.getUid();
            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing || isFinishing()) return;
                    
                    if (documentSnapshot.exists()) {
                        // 加载头像
                        String avatarUrl = documentSnapshot.getString("avatar_url");
                        if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
                            // 确保头像URL正确（添加avatar/前缀如果没有）
                            if (!avatarUrl.startsWith("avatar/")) {
                                avatarUrl = "avatar/" + avatarUrl;
                            }
                            
                            // 使用统一的服务器URL格式
                            String imageUrl = SERVER_URL + "/image/" + avatarUrl;
                            // 添加随机参数避免缓存问题
                            String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
                            imageUrl = imageUrl + "?nocache=" + uniqueParam;
                            
                            // 使用Glide加载头像
                            Glide.with(ChatRoomActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(ivUserAvatar);
                            
                            // 添加日志记录
                            android.util.Log.d("ChatRoomActivity", "加载头像URL: " + imageUrl);
                        } else {
                            // 使用默认头像
                            ivUserAvatar.setImageResource(R.drawable.default_avatar);
                        }
                        
                        // 添加点击头像查看用户资料的功能
                        final DocumentSnapshot userDoc = documentSnapshot;
                        ivUserAvatar.setOnClickListener(v -> {
                            android.util.Log.d("ChatRoomActivity", "用户点击自己的头像，打开资料页面");
                            openUserProfile(currentUserId, userDoc);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing || isFinishing()) return;
                    // 使用默认头像
                    ivUserAvatar.setImageResource(R.drawable.default_avatar);
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
                        String senderId = message.getSenderId();
                        if (senderId != null) {
                            for (String memberId : memberIds) {
                                // 不将消息发送者自己计入需要读取的人数
                                if (!memberId.equals(senderId)) {
                                    readersToCount.add(memberId);
                                }
                            }
                        } else {
                            // 如果发送者ID为空，则所有成员都需要阅读
                            readersToCount.addAll(memberIds);
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
            
            // 获取发送者信息 - 添加空值检查
            String senderId = message.getSenderId();
            if (senderId != null && !senderId.isEmpty() && !"system".equals(senderId)) {
                db.collection("users").document(senderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // 检查Activity是否正在结束
                        if (isFinishing || isFinishing()) return;
                        
                        if (documentSnapshot.exists()) {
                            // 设置用户名 - 优先使用uname字段
                            String userName = documentSnapshot.getString("uname");
                            if (userName == null || userName.isEmpty()) {
                                // 备选显示字段
                                userName = documentSnapshot.getString("email");
                                if (userName != null && userName.contains("@")) {
                                    // 显示邮箱前缀作为名称
                                    userName = userName.substring(0, userName.indexOf('@'));
                                }
                            }
                            tvSenderName.setText(userName);
                            
                            // 加载头像
                            String avatarUrl = documentSnapshot.getString("avatar_url");
                            if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
                                // 确保头像URL正确（添加avatar/前缀如果没有）
                                if (!avatarUrl.startsWith("avatar/")) {
                                    avatarUrl = "avatar/" + avatarUrl;
                                }
                                
                                // 使用统一的服务器URL格式
                                String imageUrl = SERVER_URL + "/image/" + avatarUrl;
                                // 添加随机参数避免缓存问题
                                String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
                                imageUrl = imageUrl + "?nocache=" + uniqueParam;
                                
                                // 添加日志记录
                                android.util.Log.d("ChatRoomActivity", "加载其他用户头像URL: " + imageUrl);
                                
                                // 使用Glide加载头像
                                Glide.with(ChatRoomActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .error(R.drawable.default_avatar)
                                    .into(ivUserAvatar);
                            } else {
                                // 使用默认头像
                                ivUserAvatar.setImageResource(R.drawable.default_avatar);
                            }
                            
                            // 添加点击头像查看用户资料的功能
                            final String finalSenderId = senderId;
                            final DocumentSnapshot userDoc = documentSnapshot;
                            ivUserAvatar.setOnClickListener(v -> {
                                android.util.Log.d("ChatRoomActivity", "用户点击他人的头像，用户ID: " + finalSenderId);
                                openUserProfile(finalSenderId, userDoc);
                            });
                        }
                    });
            } else {
                // 如果是系统消息或发送者ID为空
                tvSenderName.setText("系统");
                ivUserAvatar.setImageResource(R.drawable.default_avatar);
            }
            
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
        private CircleImageView ivUserAvatar;
        private boolean isExpanded = false;
        
        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAnnouncementText = itemView.findViewById(R.id.tvAnnouncementText);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPublisherName = itemView.findViewById(R.id.tvPublisherName);
            tvExpandCollapse = itemView.findViewById(R.id.tvExpandCollapse);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }
        
        public void bind(Message message) {
            tvAnnouncementText.setText(message.getText());
            tvTime.setText(formatDate(message.getTimestamp()));
            
            // 获取发布者信息 - 添加空值检查
            String senderId = message.getSenderId();
            if (senderId != null && !senderId.isEmpty() && !"system".equals(senderId)) {
                db.collection("users").document(senderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // 检查Activity是否正在结束
                        if (isFinishing || isFinishing()) return;
                        
                        if (documentSnapshot.exists()) {
                            // 设置用户名 - 优先使用uname字段
                            String userName = documentSnapshot.getString("uname");
                            if (userName == null || userName.isEmpty()) {
                                // 备选显示字段
                                userName = documentSnapshot.getString("email");
                                if (userName != null && userName.contains("@")) {
                                    // 显示邮箱前缀作为名称
                                    userName = userName.substring(0, userName.indexOf('@'));
                                }
                            }
                            tvPublisherName.setText(userName);
                            
                            // 加载头像
                            String avatarUrl = documentSnapshot.getString("avatar_url");
                            if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
                                // 确保头像URL正确（添加avatar/前缀如果没有）
                                if (!avatarUrl.startsWith("avatar/")) {
                                    avatarUrl = "avatar/" + avatarUrl;
                                }
                                
                                // 使用统一的服务器URL格式
                                String imageUrl = SERVER_URL + "/image/" + avatarUrl;
                                // 添加随机参数避免缓存问题
                                String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
                                imageUrl = imageUrl + "?nocache=" + uniqueParam;
                                
                                // 添加日志记录
                                android.util.Log.d("ChatRoomActivity", "加载公告发布者头像URL: " + imageUrl);
                                
                                // 使用Glide加载头像
                                if (ivUserAvatar != null) {
                                    Glide.with(ChatRoomActivity.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar)
                                        .into(ivUserAvatar);
                                }
                            } else if (ivUserAvatar != null) {
                                // 使用默认头像
                                ivUserAvatar.setImageResource(R.drawable.default_avatar);
                            }
                            
                            // 添加点击头像查看用户资料的功能
                            if (ivUserAvatar != null) {
                                final String finalSenderId = senderId;
                                final DocumentSnapshot userDoc = documentSnapshot;
                                ivUserAvatar.setOnClickListener(v -> {
                                    android.util.Log.d("ChatRoomActivity", "用户点击他人的头像，用户ID: " + finalSenderId);
                                    openUserProfile(finalSenderId, userDoc);
                                });
                            }
                        }
                    });
            } else {
                // 如果是系统消息或发送者ID为空
                tvPublisherName.setText("系统");
                if (ivUserAvatar != null) {
                    ivUserAvatar.setImageResource(R.drawable.default_avatar);
                }
            }
            
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

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("ChatRoomActivity", "onResume: Activity恢复");
        
        try {
            // 确保适配器开始监听
            if (adapter != null && !isFinishing && !isFinishing()) {
                adapter.startListening();
                android.util.Log.d("ChatRoomActivity", "onResume中启动适配器监听");
                
                // 触发滚动到最新消息
                new Handler().postDelayed(() -> {
                    if (adapter.getItemCount() > 0 && !isFinishing && !isFinishing()) {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 300);
            }
            
            // 更新用户读取状态
            updateUserReadStatus();
        } catch (Exception e) {
            android.util.Log.e("ChatRoomActivity", "onResume处理异常: " + e.getMessage());
        }
    }

    // 打开用户资料页面
    private void openUserProfile(String userId, DocumentSnapshot userDoc) {
        try {
            // 创建Intent
            Intent intent = new Intent(this, hk.hku.cs.hkuers.features.profile.UserProfileActivity.class);
            
            // 传递必要的用户ID
            intent.putExtra("user_id", userId);
            
            // 如果有文档，传递更多用户信息，减少下一个页面的查询
            if (userDoc != null) {
                // 传递用户基本信息
                if (userDoc.contains("uname") || userDoc.contains("name")) {
                    String username = userDoc.getString("uname");
                    if (username == null || username.isEmpty()) {
                        username = userDoc.getString("name");
                    }
                    if (username != null && !username.isEmpty()) {
                        intent.putExtra("user_name", username);
                    }
                }
                
                if (userDoc.contains("email")) {
                    intent.putExtra("user_email", userDoc.getString("email"));
                }
                
                // 添加调试信息，验证头像URL是否存在和正确
                android.util.Log.d("ChatRoomActivity", "用户文档是否包含avatar_url: " + userDoc.contains("avatar_url"));
                if (userDoc.contains("avatar_url")) {
                    String avatarUrl = userDoc.getString("avatar_url");
                    intent.putExtra("user_avatar_url", avatarUrl);
                    android.util.Log.d("ChatRoomActivity", "传递给UserProfileActivity的avatarUrl: " + avatarUrl);
                }
                
                if (userDoc.contains("department")) {
                    intent.putExtra("user_department", userDoc.getString("department"));
                }
                
                if (userDoc.contains("programme")) {
                    intent.putExtra("user_programme", userDoc.getString("programme"));
                }
                
                if (userDoc.contains("year_of_entry")) {
                    intent.putExtra("user_year_of_entry", userDoc.getString("year_of_entry"));
                }
                
                if (userDoc.contains("signature")) {
                    intent.putExtra("user_signature", userDoc.getString("signature"));
                }
            } else {
                android.util.Log.d("ChatRoomActivity", "userDoc为空，无用户数据传递");
            }
            
            // 启动活动
            startActivity(intent);
            
            // 添加过渡动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            
            android.util.Log.d("ChatRoomActivity", "已打开用户资料页面，用户ID: " + userId);
        } catch (Exception e) {
            android.util.Log.e("ChatRoomActivity", "打开用户资料失败: " + e.getMessage(), e);
            Toast.makeText(this, "无法打开用户资料: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 