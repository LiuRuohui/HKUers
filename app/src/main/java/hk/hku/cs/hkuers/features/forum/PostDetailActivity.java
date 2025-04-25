package hk.hku.cs.hkuers.features.forum;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.adapters.CommentAdapter;
import hk.hku.cs.hkuers.features.forum.models.Comment;
import hk.hku.cs.hkuers.features.profile.UserProfileActivity;

public class PostDetailActivity extends AppCompatActivity implements CommentAdapter.OnCommentActionListener {
    private static final String TAG = "PostDetailActivity";

    private TextView titleTextView;
    private TextView contentTextView;
    private TextView authorTextView;
    private TextView timestampTextView;
    private TextView likesTextView;
    private TextView commentsTextView;
    private RecyclerView commentsRecyclerView;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;
    private FirebaseFirestore db;
    private String postId;
    private FirebaseUser currentUser;
    private Handler mainHandler;
    private ListenerRegistration commentListener;
    private boolean hasLiked = false;
    private ProgressBar progressBar;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // 初始化Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        mainHandler = new Handler(Looper.getMainLooper());
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Post Detail");

        // 初始化视图
        initViews();

        // 获取传递的帖子信息
        postId = getIntent().getStringExtra("post_id");
        if (postId == null) {
            showError("帖子未找到");
            finish();
            return;
        }

        // 设置帖子信息
        setupPostInfo();

        // 初始化评论列表
        setupComments();

        // 设置按钮点击事件
        setupClickListeners();

        // 检查用户是否已点赞
        checkUserLikeStatus();
    }

    private void initViews() {
        titleTextView = findViewById(R.id.tvTitle);
        contentTextView = findViewById(R.id.tvContent);
        authorTextView = findViewById(R.id.tvAuthor);
        timestampTextView = findViewById(R.id.tvTimestamp);
        likesTextView = findViewById(R.id.tvLikes);
        commentsTextView = findViewById(R.id.tvComments);
        commentsRecyclerView = findViewById(R.id.rvComments);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupPostInfo() {
        String title = getIntent().getStringExtra("post_title");
        String content = getIntent().getStringExtra("post_content");
        String author = getIntent().getStringExtra("post_author");
        String timestamp = getIntent().getStringExtra("post_timestamp");
        String likes = getIntent().getStringExtra("post_likes");
        String comments = getIntent().getStringExtra("post_comments");

        titleTextView.setText(title);
        contentTextView.setText(content);
        authorTextView.setText(author);
        timestampTextView.setText(timestamp);
        likesTextView.setText(likes);
        commentsTextView.setText(comments);
    }

    private void setupComments() {
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentList, 
                currentUser != null ? currentUser.getUid() : null, this);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentAdapter);

        // 设置实时评论监听
        setupCommentListener();
    }

    private void setupCommentListener() {
        showLoading(true);
        commentListener = db.collection("forum_posts").document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    showLoading(false);
                    if (error != null) {
                        Log.e(TAG, "监听评论失败", error);
                        showError("加载评论失败");
                        return;
                    }
                    if (value != null) {
                        updateComments(value.getDocuments());
                    }
                });
    }

    private void updateComments(List<DocumentSnapshot> documents) {
        commentList.clear();
        for (DocumentSnapshot document : documents) {
            Comment comment = document.toObject(Comment.class);
            if (comment != null) {
                comment.setId(document.getId());
                commentList.add(comment);
            }
        }
        commentAdapter.notifyDataSetChanged();
        commentsTextView.setText(String.valueOf(commentList.size()));
    }

    private void setupClickListeners() {
        findViewById(R.id.btnLike).setOnClickListener(v -> handleLike());
        findViewById(R.id.btnComment).setOnClickListener(v -> showCommentDialog());
    }

    private void showCommentDialog() {
        if (currentUser == null) {
            showError("请先登录");
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Write your comment...");
        new AlertDialog.Builder(this)
                .setTitle("Add Comment")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String commentText = input.getText().toString().trim();
                    if (!commentText.isEmpty()) {
                        addComment(commentText);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void addComment(String commentText) {
        if (currentUser == null) {
            showError("请先登录");
            return;
        }

        Comment comment = new Comment(
                currentUser.getUid(),
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "匿名用户",
                commentText,
                dateFormat.format(new Date())
        );

        db.collection("forum_posts").document(postId)
                .collection("comments")
                .add(comment.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Comment added with ID: " + documentReference.getId());
                    // 更新评论计数
                    updateCommentCount(1);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding comment", e);
                    showError("添加评论失败: " + e.getMessage());
                });
    }

    private void updateCommentCount(int delta) {
        db.collection("forum_posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentCount = documentSnapshot.getString("comments");
                        int newCount = Integer.parseInt(currentCount != null ? currentCount : "0") + delta;
                        db.collection("forum_posts").document(postId)
                                .update("comments", String.valueOf(newCount))
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating comment count", e));
                    }
                });
    }

    private void handleLike() {
        if (currentUser == null) {
            showError("请先登录");
            return;
        }

        db.collection("forum_posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        boolean isLiked = documentSnapshot.getBoolean("hasLiked");
                        int delta = isLiked ? -1 : 1;
                        
                        // 更新点赞状态
                        db.collection("forum_posts").document(postId)
                                .update("hasLiked", !isLiked)
                                .addOnSuccessListener(aVoid -> {
                                    // 更新点赞数
                                    String currentLikes = documentSnapshot.getString("likes");
                                    int newLikes = Integer.parseInt(currentLikes != null ? currentLikes : "0") + delta;
                                    db.collection("forum_posts").document(postId)
                                            .update("likes", String.valueOf(newLikes))
                                            .addOnSuccessListener(aVoid2 -> {
                                                hasLiked = !isLiked;
                                                likesTextView.setText(String.valueOf(newLikes));
                                                // 更新UI
                                                findViewById(R.id.btnLike).setSelected(hasLiked);
                                            })
                                            .addOnFailureListener(e -> Log.e(TAG, "Error updating likes count", e));
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating like status", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting post document", e));
    }

    private void checkUserLikeStatus() {
        if (currentUser == null) {
            return;
        }

        db.collection("forum_posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        hasLiked = Boolean.TRUE.equals(documentSnapshot.getBoolean("hasLiked"));
                        findViewById(R.id.btnLike).setSelected(hasLiked);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking like status", e));
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentListener != null) {
            commentListener.remove();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onCommentLike(Comment comment) {
        if (currentUser == null) {
            showError("请先登录");
            return;
        }

        // 更新评论的点赞状态
        boolean isLiked = comment.isLikedBy(currentUser.getUid());
        if (isLiked) {
            comment.removeLike(currentUser.getUid());
        } else {
            comment.addLike(currentUser.getUid());
        }

        // 更新Firestore中的评论
        db.collection("forum_posts").document(postId)
                .collection("comments")
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
                    Log.e(TAG, "Error updating comment like", e);
                    showError("操作失败: " + e.getMessage());
                });
    }

    @Override
    public void onCommentLongClick(Comment comment) {
        // 只有评论作者可以删除评论
        if (currentUser != null && currentUser.getUid().equals(comment.getAuthorId())) {
            // 显示删除确认对话框
            new AlertDialog.Builder(this)
                    .setTitle("删除评论")
                    .setMessage("确定要删除这条评论吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        // 删除评论
                        db.collection("forum_posts").document(postId)
                                .collection("comments")
                                .document(comment.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    // 更新评论计数
                                    updateCommentCount(-1);
                                    // 从列表中移除评论
                                    int position = commentList.indexOf(comment);
                                    if (position != -1) {
                                        commentList.remove(position);
                                        commentAdapter.notifyItemRemoved(position);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting comment", e);
                                    showError("删除评论失败: " + e.getMessage());
                                });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    @Override
    public void onAuthorClick(String authorId, String authorName) {
        // 跳转到作者的个人资料页面
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("user_id", authorId);
        intent.putExtra("user_name", authorName);
        startActivity(intent);
    }
}
