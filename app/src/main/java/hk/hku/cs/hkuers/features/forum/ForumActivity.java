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

import java.util.ArrayList;
import java.util.List;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.adapters.BannerAdapter;
import hk.hku.cs.hkuers.features.forum.adapters.PostAdapter;
import hk.hku.cs.hkuers.features.forum.models.BannerItem;
import hk.hku.cs.hkuers.features.forum.models.Post;

public class ForumActivity extends AppCompatActivity {
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
        try {
            setContentView(R.layout.activity_forum);
            Log.d("ForumActivity", "onCreate: setContentView completed");

            // 初始化帖子列表
            postList = new ArrayList<>();

            // 初始化帖子RecyclerView
            recyclerView = findViewById(R.id.rvPosts);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            postAdapter = new PostAdapter(postList);
            recyclerView.setAdapter(postAdapter);

            // 添加示例数据
            addSamplePosts();

            // 初始化轮播图
            setupBanner();

            // 设置版块按钮点击事件
            setupButtonClickListeners();

        } catch (Exception e) {
            Log.e("ForumActivity", "Error in onCreate", e);
            Toast.makeText(this, "加载论坛页面时出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void addSamplePosts() {
        postList.add(new Post(
                "post1",
                "欢迎来到港大论坛",
                "这里是港大论坛的官方公告",
                "Admin",
                "2024-03-20",
                50,
                100,
                "announcement"
        ));

        postList.add(new Post(
                "post2",
                "校园活动通知",
                "本周五将举行校园开放日活动",
                "Student Union",
                "2024-03-19",
                30,
                80,
                "event"
        ));

        postList.add(new Post(
                "post3",
                "学术讲座预告",
                "计算机科学系将举办AI前沿讲座",
                "CS Department",
                "2024-03-18",
                20,
                60,
                "academic"
        ));

        postAdapter.notifyDataSetChanged();
    }

    private void setupBanner() {
        try {
            Log.d("ForumActivity", "setupBanner: start");
            viewPager = findViewById(R.id.viewPager);
            indicatorDots = findViewById(R.id.indicatorDots);

            if (viewPager == null || indicatorDots == null) {
                Log.e("ForumActivity", "ViewPager or TabLayout is null");
                return;
            }

            // 创建轮播图数据
            List<BannerItem> bannerItems = new ArrayList<>();
            bannerItems.add(new BannerItem("公告", R.drawable.ic_launcher_foreground));
            bannerItems.add(new BannerItem("热点", R.drawable.ic_launcher_foreground));
            bannerItems.add(new BannerItem("新闻", R.drawable.ic_launcher_foreground));

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
            Log.e("ForumActivity", "Error in setupBanner", e);
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
        findViewById(R.id.btnDiscussion).setOnClickListener(view -> {
            Intent intent = new Intent(this, ForumBoardActivity.class);
            intent.putExtra("board_type", "discussion");
            startActivity(intent);
        });

        findViewById(R.id.btnFun).setOnClickListener(view -> {
            Intent intent = new Intent(this, ForumBoardActivity.class);
            intent.putExtra("board_type", "fun");
            startActivity(intent);
        });

        findViewById(R.id.btnLostFound).setOnClickListener(view -> {
            Intent intent = new Intent(this, LostFoundActivity.class);
            startActivity(intent);
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
}
