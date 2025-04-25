package hk.hku.cs.hkuers.features.forum;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.auth.LoginActivity;
import hk.hku.cs.hkuers.features.forum.adapters.BannerAdapter;
import hk.hku.cs.hkuers.features.forum.adapters.PostAdapter;
import hk.hku.cs.hkuers.features.forum.models.BannerItem;
import hk.hku.cs.hkuers.features.forum.models.Post;

public class ForumActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener {
    private static final String TAG = "ForumActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ViewPager2 viewPager;
    private TabLayout indicatorDots;
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private Handler sliderHandler;
    private Runnable sliderRunnable;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);

        // 初始化Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 检查用户是否已登录
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 初始化帖子列表
        postList = new ArrayList<>();
        recyclerView = findViewById(R.id.rvPosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(this, postList, currentUser.getUid(), this);
        recyclerView.setAdapter(postAdapter);

        // 初始化轮播图
        setupBanner();

        // 设置按钮点击事件
        setupButtonClickListeners();

        // 加载热门帖子
        loadHotPosts();
    }

    private void setupBanner() {
        try {
            viewPager = findViewById(R.id.viewPager);
            indicatorDots = findViewById(R.id.indicatorDots);

            if (viewPager == null || indicatorDots == null) {
                Log.e(TAG, "ViewPager or TabLayout is null");
                return;
            }

            // 创建轮播图数据
            List<BannerItem> bannerItems = new ArrayList<>();
            bannerItems.add(new BannerItem("Campus Life", R.drawable.banner_campus_life));
            bannerItems.add(new BannerItem("Lost&Found", R.drawable.banner_lost_found));
            bannerItems.add(new BannerItem("Hot event", R.drawable.banner_events));

            // 设置轮播图适配器
            BannerAdapter bannerAdapter = new BannerAdapter(bannerItems);
            viewPager.setAdapter(bannerAdapter);

            // 设置指示器
            new TabLayoutMediator(indicatorDots, viewPager, (tab, position) -> {
                // 不需要设置任何内容
            }).attach();

            // 设置 TabLayout 的样式
            indicatorDots.setTabMode(TabLayout.MODE_FIXED);
            indicatorDots.setTabGravity(TabLayout.GRAVITY_CENTER);

            // 自动轮播
            setupAutoScroll(bannerItems);

        } catch (Exception e) {
            Log.e(TAG, "Error in setupBanner", e);
            Toast.makeText(this, "设置轮播图时出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupAutoScroll(List<BannerItem> bannerItems) {
        sliderHandler = new Handler();
        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentPage == bannerItems.size()) {
                    currentPage = 0;
                }
                viewPager.setCurrentItem(currentPage++, true);
                sliderHandler.postDelayed(this, 3000);
            }
        };

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
            }
        });
    }

    private void setupButtonClickListeners() {
        findViewById(R.id.btnCampusLife).setOnClickListener(v -> {
            Intent intent = new Intent(this, ForumBoardActivity.class);
            intent.putExtra("board_type", "campus_life");
            startActivity(intent);
        });

        findViewById(R.id.btnLostFound).setOnClickListener(v -> {
            Intent intent = new Intent(this, LostFoundActivity.class);
            startActivity(intent);
        });
    }

    private void loadHotPosts() {
        db.collection("forum_posts")
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setId(doc.getId());
                            postList.add(post);
                        }
                    }
                    postAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading hot posts", e);
                    Toast.makeText(this, "加载热门帖子失败", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sliderHandler != null && sliderRunnable != null) {
            sliderHandler.postDelayed(sliderRunnable, 3000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sliderHandler != null && sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }
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
}
