package hk.hku.cs.hkuers.features.forum;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.adapters.PostAdapter;
import hk.hku.cs.hkuers.features.forum.models.Post;

public class ForumBoardActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private String boardType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum_board);

        // 获取传递的版块类型
        boardType = getIntent().getStringExtra("board_type");

        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 根据版块类型设置标题
        if ("discussion".equals(boardType)) {
            setTitle("学习讨论");
        } else if ("fun".equals(boardType)) {
            setTitle("校内趣闻");
        }

        // 初始化帖子列表
        postList = new ArrayList<>();
        recyclerView = findViewById(R.id.rvPosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // 设置发布新帖子按钮的点击事件
        findViewById(R.id.fabNewPost).setOnClickListener(v -> {
            Intent intent = new Intent(this, NewForumBoardActivity.class);
            intent.putExtra("board_type", boardType);
            startActivity(intent);
        });

        // 加载对应版块的帖子
        loadPosts();
    }

    private void loadPosts() {
        // 清空现有帖子
        postList.clear();

        // 根据版块类型加载不同的帖子
        if ("discussion".equals(boardType)) {
            // 添加学习讨论相关的示例帖子
            postList.add(new Post(
                    "discussion1", // id
                    "数据结构课程讨论", // title
                    "关于本周数据结构课程的内容讨论", // content
                    "CS Student", // author
                    "cs1", // authorId
                    "2024-03-20", // timestamp
                    "25", // likes
                    "50", // comments
                    "discussion" // boardType
            ));

            postList.add(new Post(
                    "discussion2", // id
                    "算法作业求助", // title
                    "关于动态规划算法的实现问题", // content
                    "Math Student", // author
                    "math1", // authorId
                    "2024-03-19", // timestamp
                    "15", // likes
                    "30", // comments
                    "discussion" // boardType
            ));
        } else if ("fun".equals(boardType)) {
            // 添加校内趣闻相关的示例帖子
            postList.add(new Post(
                    "fun1", // id
                    "校园趣事分享", // title
                    "今天在图书馆遇到的有趣事情", // content
                    "Funny Student", // author
                    "fun1", // authorId
                    "2024-03-20", // timestamp
                    "40", // likes
                    "80", // comments
                    "fun" // boardType
            ));
        }

        postAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
