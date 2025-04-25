package hk.hku.cs.hkuers.features.forum;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.adapters.CommentAdapter;
import hk.hku.cs.hkuers.features.forum.models.Comment;

public class CommentActivity extends AppCompatActivity implements CommentAdapter.OnCommentActionListener {
    private RecyclerView recyclerView;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;
    private EditText commentEditText;
    private FloatingActionButton fabSend;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String postId;
    private String postTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        // 初始化Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 获取传递的帖子信息
        postId = getIntent().getStringExtra("post_id");
        postTitle = getIntent().getStringExtra("post_title");

        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(postTitle);

        // 初始化视图
        recyclerView = findViewById(R.id.rvComments);
        commentEditText = findViewById(R.id.etComment);
        fabSend = findViewById(R.id.fabSend);

        // 初始化评论列表
        commentList = new ArrayList<>();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : null;
        commentAdapter = new CommentAdapter(this, commentList, currentUserId, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(commentAdapter);

        // 设置发送按钮点击事件
        fabSend.setOnClickListener(v -> sendComment());
        // 加载评论前，先修复数据库
        updateExistingComments();
        // 加载评论
        loadComments();
    }

    private void loadComments() {
        // 清空评论列表
        commentList.clear();

        // 从Firestore加载评论数据
        db.collection("comments")
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Comment comment = document.toObject(Comment.class);
                        if (comment != null) {
                            comment.setId(document.getId());
                            commentList.add(comment);
                        }
                    }
                    commentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "加载评论失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendComment() {
        String commentText = commentEditText.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查用户是否已登录
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "请先登录再发表评论", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建评论ID
        String commentId = UUID.randomUUID().toString();

        // 创建评论对象
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(currentUser.getUid());
        comment.setAuthorName(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "匿名用户");
        comment.setContent(commentText);
        comment.setTimestamp(new Date().toString());
        comment.setLikesCount(0);

        // 保存评论到Firestore
        db.collection("comments")
                .document(commentId)
                .set(comment)
                .addOnSuccessListener(aVoid -> {
                    // 添加评论到列表
                    commentList.add(comment);
                    commentAdapter.notifyItemInserted(commentList.size() - 1);
                    // 清空输入框
                    commentEditText.setText("");
                    // 滚动到底部
                    recyclerView.smoothScrollToPosition(commentList.size() - 1);
                    Toast.makeText(this, "The comment was successfully published", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "评论发表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onCommentLike(Comment comment) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "请先登录再点赞", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        boolean isLiked = comment.isLikedBy(userId);

        // 更新评论的点赞状态
        if (isLiked) {
            comment.removeLike(userId);
        } else {
            comment.addLike(userId);
        }

        // 更新Firestore中的评论
        db.collection("comments")
                .document(comment.getId())
                .update("likedBy", comment.getLikedBy(), "likesCount", comment.getLikesCount())
                .addOnSuccessListener(aVoid -> {
                    // 更新UI
                    int position = commentList.indexOf(comment);
                    if (position != -1) {
                        commentAdapter.notifyItemChanged(position);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onCommentLongClick(Comment comment) {
        // 只有评论作者可以删除评论
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(comment.getAuthorId())) {
            // 显示删除确认对话框
            new android.app.AlertDialog.Builder(this)
                    .setTitle("删除评论")
                    .setMessage("确定要删除这条评论吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        int position = commentList.indexOf(comment);
                        if (position != -1) {
                            deleteComment(comment, position);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    private void deleteComment(Comment comment, int position) {
        db.collection("comments")
                .document(comment.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    commentList.remove(position);
                    commentAdapter.notifyItemRemoved(position);
                    Toast.makeText(this, "评论已删除", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "删除评论失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onAuthorClick(String authorId, String authorName) {
        // 可以在这里添加查看作者信息的逻辑
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateExistingComments() {
        db.collection("comments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int updatedCount = 0;
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        boolean needsUpdate = false;

                        // 检查likesCount字段
                        if (!document.contains("likesCount")) {
                            needsUpdate = true;
                        }

                        if (needsUpdate) {
                            // 为缺失字段创建更新Map
                            java.util.Map<String, Object> updates = new java.util.HashMap<>();

                            if (!document.contains("likesCount")) {
                                updates.put("likesCount", 0);
                            }

                            // 更新文档
                            document.getReference().update(updates);
                            updatedCount++;
                        }
                    }

                    if (updatedCount > 0) {
                        System.out.println("已更新 " + updatedCount + " 条评论，添加了缺失的点赞字段");
                    }
                })
                .addOnFailureListener(e -> {
                    System.err.println("更新评论失败: " + e.getMessage());
                });
    }
}
