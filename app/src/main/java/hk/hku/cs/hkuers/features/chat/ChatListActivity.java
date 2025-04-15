package hk.hku.cs.hkuers.features.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.models.ChatGroup;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
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

        // 初始化视图
        recyclerView = findViewById(R.id.recyclerChatGroups);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ImageButton btnCreateChat = findViewById(R.id.btnCreateChat);

        // 设置布局管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 设置创建聊天按钮点击事件
        btnCreateChat.setOnClickListener(v -> showCreateChatDialog());

        // 加载聊天列表
        loadChatGroups();
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
        
        public ChatGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }

        public void bind(ChatGroup group) {
            // 设置群组名称
            tvGroupName.setText(group.getGroupName());
            
            // 设置最后消息
            tvLastMessage.setText(group.getLastMessage());
            
            // 设置时间戳
            tvTimestamp.setText(group.getFormattedTimestamp());
            
            // 设置未读消息数量
            int unreadCount = group.getUnreadCount();
            if (unreadCount > 0) {
                tvUnreadCount.setVisibility(View.VISIBLE);
                tvUnreadCount.setText(String.valueOf(unreadCount));
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }
        }
    }
}