package hk.hku.cs.hkuers.features.forum;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.auth.LoginActivity;
import hk.hku.cs.hkuers.features.forum.adapters.PostAdapter;
import hk.hku.cs.hkuers.features.forum.models.Post;

public class ForumBoardActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener {

    private static final String TAG = "ForumBoardActivity";
    private static final String BOARD_CAMPUS_LIFE = "campus_life";
    private static final String BOARD_LOSTFOUND = "lostfound";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private Handler mainHandler;
    private String boardType;
    private FloatingActionButton fabNewPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum_board);

        // 获取版块类型
        boardType = getIntent().getStringExtra("board_type");
        if (boardType == null) {
            boardType = BOARD_CAMPUS_LIFE; // 默认为校园生活版块
        }

        // 初始化Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getBoardTitle());

        // 初始化控件
        initViews();

        // 设置点击事件
        setupClickListeners();

        // 加载帖子
        loadPosts();
    }

    private String getBoardTitle() {
        switch (boardType) {
            case BOARD_CAMPUS_LIFE:
                return "Campus Life";
            case BOARD_LOSTFOUND:
                return "Lost&Found";
            default:
                return "Forum";
        }
    }

    private void initViews() {
        postsRecyclerView = findViewById(R.id.rvPosts);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        fabNewPost = findViewById(R.id.fabNewPost);

        // 初始化帖子列表
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(this, postList, 
                mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null, this);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsRecyclerView.setAdapter(postAdapter);

        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::loadPosts);
    }

    private void setupClickListeners() {
        fabNewPost.setOnClickListener(v -> navigateToNewPost());
    }

    private void navigateToNewPost() {
        Intent intent = new Intent(this, NewForumBoardActivity.class);
        intent.putExtra("board_type", boardType);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserLoggedIn();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    private void checkUserLoggedIn() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void loadPosts() {
        showLoading(true);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showLoading(false);
            Log.e(TAG, "User not logged in");
            return;
        }

        Log.d(TAG, "Loading posts for board type: " + boardType);
        
        // 根据不同的版块类型查询不同的集合
        if (BOARD_LOSTFOUND.equals(boardType)) {
            // 失物招领页面查询 lostFoundItems 集合
            db.collection("lostFoundItems")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        showLoading(false);
                        Log.d(TAG, "Successfully loaded " + queryDocumentSnapshots.size() + " lost and found items");
                        updateLostFoundItems(queryDocumentSnapshots.getDocuments());
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Error loading lost and found items: " + e.getMessage(), e);
                        showError("加载失物招领信息失败: " + e.getMessage());
                    });
        } else {
            // 校园生活页面查询 forum_posts 集合
            db.collection("forum_posts")
                    .whereEqualTo("boardType", boardType)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        showLoading(false);
                        Log.d(TAG, "Successfully loaded " + queryDocumentSnapshots.size() + " forum posts");
                        updatePosts(queryDocumentSnapshots.getDocuments());
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Error loading forum posts: " + e.getMessage(), e);
                        showError("加载帖子失败: " + e.getMessage());
                    });
        }
    }

    private void updatePosts(List<DocumentSnapshot> documents) {
        postList.clear();
        for (DocumentSnapshot document : documents) {
            Post post = document.toObject(Post.class);
            if (post != null) {
                post.setId(document.getId());
                postList.add(post);
            }
        }
        
        // 在客户端按时间戳排序
        postList.sort((p1, p2) -> {
            // 假设时间戳是字符串格式，需要转换为可比较的格式
            try {
                long time1 = Long.parseLong(p1.getTimestamp());
                long time2 = Long.parseLong(p2.getTimestamp());
                return Long.compare(time2, time1); // 降序排列（最新的在前）
            } catch (NumberFormatException e) {
                // 如果时间戳不是数字格式，则按字符串比较
                return p2.getTimestamp().compareTo(p1.getTimestamp());
            }
        });
        
        postAdapter.notifyDataSetChanged();
    }

    // 更新失物招领列表
    private void updateLostFoundItems(List<DocumentSnapshot> documents) {
        postList.clear();
        for (DocumentSnapshot document : documents) {
            // 将失物招领项目转换为Post对象
            Post post = new Post();
            post.setId(document.getId());
            post.setTitle(document.getString("title") != null ? document.getString("title") : "无标题");
            post.setContent(document.getString("description") != null ? document.getString("description") : "无描述");
            post.setAuthor(document.getString("author") != null ? document.getString("author") : "匿名用户");
            post.setAuthorId(document.getString("authorId") != null ? document.getString("authorId") : "");
            post.setTimestamp(document.getString("timestamp") != null ? document.getString("timestamp") : "");
            post.setLikes("0");
            post.setComments("0");
            post.setBoardType(BOARD_LOSTFOUND);
            
            postList.add(post);
        }
        
        // 在客户端按时间戳排序
        postList.sort((p1, p2) -> {
            try {
                long time1 = Long.parseLong(p1.getTimestamp());
                long time2 = Long.parseLong(p2.getTimestamp());
                return Long.compare(time2, time1); // 降序排列（最新的在前）
            } catch (NumberFormatException e) {
                return p2.getTimestamp().compareTo(p1.getTimestamp());
            }
        });
        
        postAdapter.notifyDataSetChanged();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(show);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPostClick(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getId());
        intent.putExtra("post_title", post.getTitle());
        intent.putExtra("post_content", post.getContent());
        intent.putExtra("post_author", post.getAuthor());
        intent.putExtra("post_timestamp", post.getTimestamp());
        intent.putExtra("post_likes", post.getLikes());
        intent.putExtra("post_comments", post.getComments());
        startActivity(intent);
    }

    @Override
    public void onLikeClick(Post post) {
        // 点赞功能在 PostDetailActivity 中实现
    }

    @Override
    public void onCommentClick(Post post) {
        // 评论功能在 PostDetailActivity 中实现
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
